package com.reactor.accio.pipeline.gather

import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.base.patterns.pull.FlowControlArgs
import akka.actor.ActorRef
import com.reactor.accio.transport.MetadataContainer
import com.reactor.accio.metadata.MetaData
import scala.collection.JavaConversions._
import com.reactor.accio.transport.MetadataContainer
import com.reactor.accio.metadata.Candidate
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import com.reactor.accio.transport.ConfluenceNodeList
import com.reactor.accio.transport.CandidateList
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import com.reactor.accio.transport.CandidateList
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global
import com.reactor.accio.storage.GraphDB
import com.reactor.accio.transport.IdList
import com.reactor.base.patterns.pull.FlowControlFactory
import com.reactor.base.patterns.pull.FlowControlConfig
import com.reactor.accio.transport.MetadataContainer
import com.reactor.accio.transport.StringList
import java.util.ArrayList
import com.reactor.accio.transport.ConfluenceContainer
import com.reactor.accio.transport.AccioRequest
import com.reactor.accio.transport.KeywordsContainer
import com.reactor.accio.transport.KeywordContainer

// Gather Types
trait GatherType
case class General() extends GatherType
case class Business() extends GatherType
case class Film() extends GatherType
case class Music() extends GatherType
case class ActorOrDirector() extends GatherType
case class TV() extends GatherType

// Extractor actor
class Gatherer(args:FlowControlArgs) extends FlowControlActor(args) {

	// Graph database
	val graphdb = new GraphDB()
	
	// Start gatherers
	val newsGatherer:ActorRef = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="newGatherer", actorType="com.reactor.accio.pipeline.gather.NewsGatherer"))
	val twitterGatherer:ActorRef = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="twitterGatherer", actorType="com.reactor.accio.pipeline.gather.TwitterGatherer"))
	val youtubeGatherer:ActorRef = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="youtubeGatherer", actorType="com.reactor.accio.pipeline.gather.YouTubeGatherer"))
	val stocksGatherer:ActorRef = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="stockGatherer", actorType="com.reactor.accio.pipeline.gather.FinanceGatherer"))
	val facebookGatherer:ActorRef = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="facebookGatherer", actorType="com.reactor.accio.pipeline.gather.FacebookGatherer"))
	val flickrGatherer:ActorRef = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="flickrGatherer", actorType="com.reactor.accio.pipeline.gather.FlickrGatherer"))
	val itunesGatherer:ActorRef = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="itunesGatherer", actorType="com.reactor.accio.pipeline.gather.ItunesGatherer"))
	
	val news_list = ArrayBuffer[String]()
	val twitter_list = ArrayBuffer[String]()
	val stocks_list = ArrayBuffer[String]()
	val itunes_list = ArrayBuffer[String]()
	
	// Ready
	ready()
	
	def receive = {
	  case MetadataContainer(metaData) =>
	    val origin = sender
	    process(metaData.copy, null, origin)
	  case ConfluenceContainer(metaData, request) =>
	    val origin = sender
	    process(metaData.copy, request, origin)
	}
	
	// Process
	def process(metaData:MetaData, request:AccioRequest, origin:ActorRef) {
	  implicit val timeout = Timeout(10 seconds)
	  
		// Sort candidates by the types of things that need fetched
		metaData.keywords.toList map { keyword =>
	  		keyword.candidates.toList map { candiate =>
	  			sortCandidate(candiate)
	  		}
		}
		
	  	// Get news and twitter ids
	  	val graphConfluence = graphdb.findConfluenceIDs(metaData.topicIDList)
	  	graphConfluence match {
	  		case Some(graphNode) =>
	  			
	  			if (graphNode.has("topicNewsIds")) {
	  				graphNode.path("topicNewsIds").toList map { id => news_list += id.asText() }
	  			}
	  			
	  			if (graphNode.has("topicTwitterIds")) {
	  				graphNode.path("topicTwitterIds").toList map { id => twitter_list += id.asText() }
	  			}
	  			
	  		case None =>
	  	}
	  	// List of pending confluence node lists
	  	val futures = ArrayBuffer[Future[ConfluenceNodeList]]()
	  	
	  	// Start fetching everything
	  	if (!stocks_list.isEmpty) futures += (stocksGatherer ? StringList(stocks_list.clone)).mapTo[ConfluenceNodeList]
	  	if (!news_list.isEmpty) futures += (newsGatherer ? IdList(news_list.clone)).mapTo[ConfluenceNodeList]
	  	if (!twitter_list.isEmpty) futures += (twitterGatherer ? IdList(twitter_list.clone)).mapTo[ConfluenceNodeList] 
	  	if (metaData.free_text != null) futures += (youtubeGatherer ? metaData.free_text).mapTo[ConfluenceNodeList]
	  	if (request.facebook_token != null) futures += (facebookGatherer ? KeywordsContainer(metaData.keywords, request)).mapTo[ConfluenceNodeList]
	  	if (!metaData.keywords.isEmpty()) futures += (flickrGatherer ? KeywordContainer(metaData.keywords.get(0))).mapTo[ConfluenceNodeList]
	  	if (!itunes_list.isEmpty()) futures += (itunesGatherer ? itunes_list.get(0)).mapTo[ConfluenceNodeList]
	  			
	  	// Clear lists to start fresh on next call
	  	news_list.clear
	  	twitter_list.clear
	  	stocks_list.clear
	  	itunes_list.clear
	  	
		Future.sequence(futures) onComplete {
	  		case Success(completed) => 
	  			completed map { list =>
	  				metaData.confluence.addConfluenceNodes(new ArrayList(list.confluenceNodes))
	  			}
	  			
	  			reply(origin, MetadataContainer(metaData))
	  			
	  		case Failure(e) => 
	  			log.error("An error has occurred: " + e.getMessage())
	  			reply(origin, None)
		}	  	
	}
	
	// Define the gather type of a candidate
	def defineGatherType(candidate:Candidate): Option[GatherType] = {
		val types = candidate.types
	
		if (types.contains("ns:business.issuer")) {
			return Some (Business())
		}
		
		if (types.contains("ns:film.film")) {
			return Some (Film())
		}
		
		if(types.contains("ns:music.artist")
		    || types.contains("ns:music.composition")){
		  return Some(Music())
		}
		
		if(types.contains("ns:film.director") 
		    || types.contains("ns:film.actor")){
		  return Some(ActorOrDirector())
		}
		
		if(types.contains("ns:tv.tv_program")
		    || types.contains("ns:tv.tv_director")){  
		  return Some(TV())
		}
		
		else {
			return Some (General())
		}
		
		None
	}
	
	// Separate candidates bases on their gather type
	def sortCandidate(candidate:Candidate) {
		defineGatherType(candidate) match {
			case Some(gatherType) => 
				gatherType match {
					case _:Business => 
					  stocks_list += candidate.name
					case _:Film => 
					  itunes_list += candidate.name
					case _:TV =>
					  itunes_list += candidate.name
					case _:Music =>
					  itunes_list += candidate.name
					case _:ActorOrDirector =>
					  itunes_list += candidate.name
					case _ => 
			}
				
			case None => 					
		}
	}
}