package com.reactor.accio.pipeline

import akka.actor.ActorRef
import com.reactor.accio.transport.MetadataContainer
import com.reactor.accio.metadata.MetaData
import com.reactor.base.utilities.Tools
import com.fasterxml.jackson.databind.node.ArrayNode
import com.reactor.accio.metadata.Keyword
import scala.collection.mutable.ArrayBuffer
import java.util.regex.Pattern
import scala.collection.JavaConversions._
import com.reactor.accio.transport.MetadataContainer
import com.reactor.accio.storage.GraphDB
import com.reactor.accio.metadata.Candidate
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Failure
import scala.collection.JavaConversions._
import com.fasterxml.jackson.databind.JsonNode
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import com.reactor.base.patterns.pull._
import scala.concurrent.Await
import com.reactor.accio.transport.StringList
import com.reactor.accio.transport.CandidateContainer
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import com.reactor.accio.transport.CandidateContainer
import com.reactor.accio.transport.CandidateContainer

// Describer args
case class DescriberArgs(flickrFetcher:ActorRef, wikiImageFetcher:ActorRef) extends FlowControlArgs 

// Image intermediate
case class ImageIntermediate(primary:Option[StringList], secondary:Option[StringList])

// Describer actor
class Describer(args:DescriberArgs) extends FlowControlActor(args) {
	
	// Graph database
	val graphdb = new GraphDB()

	val fetcherArgs = DescriberArgs(args.flickrFetcher, args.wikiImageFetcher)
	val fetcher = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="describerFetcher", actorType="com.reactor.accio.pipeline.DescriberFetcher"), fetcherArgs)
	
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
		implicit val timeout = Timeout(3 seconds)
		
		val futures = new ArrayBuffer[Future[Option[Candidate]]]()
		
		metaData.keywords.toList map { keyword =>
			keyword.candidates.toList map { candidate =>
				futures += (fetcher ? CandidateContainer(candidate)).mapTo[Option[Candidate]]
			}
		}	  
		
		// Sequence list
		Future.sequence(futures) onComplete {
	  		case Success(completed) => 
	  			val connectedMetaData = metaData
	  			reply(origin, MetadataContainer(connectedMetaData))
	  		case Failure(e) => 
	  			log.error("An error has occurred: " + e.getMessage())
		}			
	}
	
}

class DescriberFetcher(args:DescriberArgs) extends FlowControlActor(args) {
	
	// Graph database
	val graphdb = new GraphDB()
	
	// Save fetchers
	val wikiImageFetcher = args.wikiImageFetcher
	val flickrFetcher = args.flickrFetcher
	
	// Ready
	ready()
	
	def receive = {
	  case CandidateContainer(candidate) =>
	    val origin = sender
	    process(candidate, origin)
	    complete()
	}
	
	// Process
	def process(candidate:Candidate, origin:ActorRef) {
	  // Timeout
	  implicit val timeout = Timeout(3 seconds)
	  
	  // Fetch images
	  val primaryFuture =  (wikiImageFetcher ? CandidateContainer(candidate)).mapTo[Option[StringList]] 
	  val secondaryFuture = (flickrFetcher ? CandidateContainer(candidate)).mapTo[Option[StringList]] 
	  
	  val results: Option[JsonNode] = graphdb.findVertexDetails(candidate.mid)
	  
	  results match {
	    case Some(details) => 
	      candidate.grabVertexMetaData(details)
	      
	      // Collect Images
	      val fetched = for {
	    	  primary <- primaryFuture
	    	  secondary <- secondaryFuture
	      } yield ImageIntermediate(primary, secondary)
	  
	      fetched onComplete {
	      	case Success(completed) =>
	      		completed.primary match {
	      			case Some(urls) => candidate.addPrimaryImages(urls.strings)
	      			case None =>
	      		}
	      		
	      		completed.secondary match {
	      			case Some(urls) => candidate.addSecondaryImages(urls.strings)
	      			case None =>
	      		}
	      		
	      		reply(origin, candidate)
	      		
	  		case Failure(e) => 
	  			log.error("A fetching error has occurred: " + e.getMessage())
	  			
	  			reply(origin, None)
	      }
	      
	    case None =>
	    	reply(origin, None)
	  }		
		
	}
}