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
import com.reactor.accio.storage.GraphDB

// Extractor actor
class Connector(args:FlowControlArgs) extends FlowControlActor(args) {
  
	// Graph database
	val graphdb = new GraphDB()
	
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
	  
		// Get the matrix
		metaData.initConnectionMatrix();

		// Fetch each set
		val sets = metaData.connection_matrix.grabSets.toList
		val futures: List[Future[Option[ConnectionSet]]] = sets map { set =>
	  		Future { fetch(set)  }
		}
	  
		// Sequence list
		Future.sequence(futures) onComplete {
	  		case Success(completed) => 
	  			val connectedMetaData = metaData
	  			connectedMetaData.prune
	  			reply(origin, MetadataContainer(connectedMetaData))
	  		case Failure(e) => 
	  			log.error("An error has occurred: " + e.getMessage())
	  }
	  
	}
	

	// Fetching
	def fetch(set:ConnectionSet): Option[ConnectionSet] = {
	  val results: Option[ConnectionSet] = graphdb.findConnections(set)
	 
	  results match {
	    case Some(results) => 
	      set.connections = results.connections
	      return Some( set )
	    case None =>
	    	println("None")
	    	return None
	  }
	}
}