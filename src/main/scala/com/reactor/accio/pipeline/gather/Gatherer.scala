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
import com.reactor.accio.metadata.confluence.ConfluenceNode
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
	val twitterGatherer:ActorRef = null
	val youtubeGatherer:ActorRef = null
	val stocksGatherer:ActorRef = null
	
	val news_list = ArrayBuffer[String]()
	val twitter_list = ArrayBuffer[String]()
	val stocks_list = ArrayBuffer[Candidate]()
	
	// Ready
	ready()
	
	def receive = {
	  case MetadataContainer(metaData) =>
	    val origin = sender
	    process(metaData.copy, origin)
	    complete()
	}
	
	// Process
	def process(metaData:MetaData, origin:ActorRef) {
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
	  	futures += (newsGatherer ? IdList(news_list)).mapTo[ConfluenceNodeList]
	  	futures += (twitterGatherer ? IdList(twitter_list)).mapTo[ConfluenceNodeList] 
//	  	futures += (youtubeGatherer ? metaData.free_text).mapTo[ConfluenceNodeList]
//	  	futures += (stocksGatherer ? CandidateList(stocks_list)).mapTo[ConfluenceNodeList]
	  			
		Future.sequence(futures) onComplete {
	  		case Success(completed) => 
	  			completed map { list =>
	  				metaData.confluence.addConfluenceNodes(list.confluenceNodes)
	  			}
	  		case Failure(e) => 
	  			log.error("An error has occurred: " + e.getMessage())
		}	  	
	  	
	  	reply(origin, MetadataContainer(metaData))
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
						stocks_list += candiate
					case _ => 
			}
				
			case None => 					
		}
	}
}