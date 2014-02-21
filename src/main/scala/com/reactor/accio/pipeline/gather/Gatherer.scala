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

// Gather Types
trait GatherType
case class General() extends GatherType
case class Business() extends GatherType
case class Film() extends GatherType

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
	
	val news_list = ArrayBuffer[String]()
	val twitter_list = ArrayBuffer[String]()
	val stocks_list = ArrayBuffer[String]()
	
	// Ready
	ready()
	
	def receive = {
	  case MetadataContainer(metaData) =>
	    val origin = sender
	    process(metaData.copy, null, origin)
	    complete()
	  case ConfluenceContainer(metaData, request) =>
	    val origin = sender
	    process(metaData.copy, request, origin)
	    complete()
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
	  	if (!news_list.isEmpty) futures += (newsGatherer ? IdList(news_list.clone)).mapTo[ConfluenceNodeList]
	  	if (!twitter_list.isEmpty) futures += (twitterGatherer ? IdList(twitter_list.clone)).mapTo[ConfluenceNodeList] 
	  	if (metaData.free_text != null) futures += (youtubeGatherer ? metaData.free_text).mapTo[ConfluenceNodeList]
	  	if (!stocks_list.isEmpty) futures += (stocksGatherer ? StringList(stocks_list.clone)).mapTo[ConfluenceNodeList]
	  	if(request.facebook_token != null) futures += (facebookGatherer ? KeywordsContainer(metaData.keywords, request)).mapTo[ConfluenceNodeList]
	  			
	  	// Clear lists to start fresh on next call
	  	news_list.clear
	  	twitter_list.clear
	  	stocks_list.clear
	  	
		Future.sequence(futures) onComplete {
	  		case Success(completed) => 
	  			completed map { list =>
	  				metaData.confluence.addConfluenceNodes(new ArrayList(list.confluenceNodes))
	  			}
	  			
	  			reply(origin, MetadataContainer(metaData))
	  			
	  		case Failure(e) => 
	  			log.error("An error has occurred: " + e.getMessage())
		}	  	
	}
	
	// Define the gather type of a candidate
	def defineGatherType(candiate:Candidate): Option[GatherType] = {
	
		if (candiate.types.contains("ns:business.issuer")) {
			return Some (Business())
		}
		
		if (candiate.types.contains("ns:film.film")) {
			return Some (Film())
		}
		
		else {
			return Some (General())
		}
		
		None
	}
	
	// Separate candidates bases on their gather type
	def sortCandidate(candiate:Candidate) {
		defineGatherType(candiate) match {
			case Some(gatherType) => 
				gatherType match {
					case t:Business =>
						stocks_list += candiate.name
					case _ => 
			}
				
			case None => 					
		}
	}
}