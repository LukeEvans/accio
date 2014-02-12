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
import scala.concurrent.Future
import com.reactor.accio.metadata.connections.ConnectionSet
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Failure

// Extractor actor
class Connector(args:FlowControlArgs) extends FlowControlActor(args) {
  
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
	  
		// Get the matrix
		metaData.initConnectionMatrix();

		
		val futures: List[Future[Option[ConnectionSet]]] = metaData.connection_matrix.grabSets.toList map { set =>
	  		Future { fetch(set)  }
		}
	  
		// Sequence list
		Future.sequence(futures) onComplete {
	  		case Success(completed) => 
	  			reply(origin, MetadataContainer(metaData.copy()))
	  		case Failure(e) => 
	  			log.error("An error has occurred: " + e.getMessage())
	  }
	  
	}
	

	// Fetching
	def fetch(set:ConnectionSet): Option[ConnectionSet] = {
	  val results: Option[String] = Some("yest")
	 
	  results match {
	    case Some(results) =>
	  
	    case None => return None
	  }
	  
	  Some (set)
	}
}