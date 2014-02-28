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

// Extractor actor
class Describer(args:FlowControlArgs) extends FlowControlActor(args) {
	
	// Graph database
	val graphdb = new GraphDB()
	val flickerFetcher = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="flickerFetcher", actorType="com.reactor.accio.pipeline.gather.FlickrGatherer"))
	
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
		
		val futures = new ArrayBuffer[Future[Option[Candidate]]]()
		
		metaData.keywords.toList map { keyword =>
			keyword.candidates.toList map { candidate =>
				futures += Future { fetch(candidate) }
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
	
	// Fetch
	def fetch(candidate:Candidate): Option[Candidate] = {
	  implicit val timeout = Timeout(10 seconds)
	  
	  val imageOptionFuture = (flickerFetcher ? candidate.name).mapTo[Option[String]]
	  val imageOption = Await.result(imageOptionFuture, timeout.duration)
	  
	  val results: Option[JsonNode] = graphdb.findVertexDetails(candidate.mid)
	  
	  results match {
	    case Some(details) => 
	      candidate.grabVertexMetaData(details)
	  		imageOption match {
	  			case Some(url) => 
	  				candidate.images.add(url)
	  				return Some ( candidate )
	  			case None => 
	  				println("no image for: " + candidate.name)
	  				
	  			return Some ( candidate )
	  		}	      
	      
	    case None =>
	    	println("None")
	    	
	    	return Some ( candidate )
	  }
	  
	}
	
}