package com.reactor.accio.pipeline

import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.base.patterns.pull.FlowControlArgs
import com.reactor.accio.transport.MetadataContainer
import com.reactor.base.utilities.Tools
import com.fasterxml.jackson.databind.node.ArrayNode
import com.reactor.accio.metadata.MetaData
import akka.actor.ActorRef
import com.reactor.accio.metadata.Keyword
import scala.collection.JavaConversions._
import com.reactor.accio.metadata.Candidate
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Failure
import com.reactor.accio.transport.MetadataContainer

// Disambiguate Actor
class Disambiguator(args:FlowControlArgs) extends FlowControlActor(args) {
  
	val disambigURL: String = "https://www.googleapis.com/freebase/v1/search?key=AIzaSyDgEm2hDVbVeruKXokiMyrShPZaVL4VICU&query=";
  
	// Ready
	ready()
	
	def receive = {
	  case MetadataContainer(metaData) =>
	    println("\nDisambig Starting!!\n")
	    val origin = sender
	    process(metaData, origin)
	    complete()
	}
	
	// Process
	def process(metaData:MetaData, origin:ActorRef) {
	  
	  // Get ids
	  val futures: List[Future[Option[Keyword]]] = metaData.keywords.toList map { keyword =>
	  	Future { fetch(disambigURL + keyword.original_text, keyword)   }
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
	def fetch(url:String, keyword:Keyword): Option[Keyword] = {
	  val results = Tools.fetchURL(url)
	 
	  results match {
	    case Some(results) =>
			  
		  // Add candidate
		  results.path("result").toList map { node =>
		    keyword.addCandidate(new Candidate(node))
		    
		    if (keyword.candidateCount == Keyword.MAX_CANDIDATES) {
		      return Some (keyword)
		    }
		  }
	  
	    case None => return None
	  }
	  
	  Some (keyword)
	}
}