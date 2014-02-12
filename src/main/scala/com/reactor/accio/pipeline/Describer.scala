package com.reactor.accio.pipeline

import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.base.patterns.pull.FlowControlArgs
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
import com.reactor.accio.graphdb.GraphDB
import com.reactor.accio.metadata.Candidate
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Failure
import scala.collection.JavaConversions._
import com.fasterxml.jackson.databind.JsonNode

// Extractor actor
class Describer(args:FlowControlArgs) extends FlowControlActor(args) {
	
	// Graph database
	val graphdb = new GraphDB()
	
	// Ready
	ready()
	
	def receive = {
	  case MetadataContainer(metaData) =>
	    val origin = sender
	    process(metaData, origin)
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
	  			val connectedMetaData = metaData.copy
	  			reply(origin, MetadataContainer(connectedMetaData))
	  		case Failure(e) => 
	  			log.error("An error has occurred: " + e.getMessage())
	  }		
	}
	
	// Fetch
	def fetch(candidate:Candidate): Option[Candidate] = {
	  val results: Option[JsonNode] = graphdb.findVertexDetails(candidate.mid)
	 
	  results match {
	    case Some(details) => 
	      candidate.grabVertexMetaData(details);
	      return Some( candidate )
	      
	    case None =>
	    	println("None")
	    	return None
	  }
	}
}