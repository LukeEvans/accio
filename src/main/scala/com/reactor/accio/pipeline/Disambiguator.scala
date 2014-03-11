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
import com.reactor.accio.pipeline.gather.YahooFinance

// Disambiguate Actor
class Disambiguator(args:FlowControlArgs) extends FlowControlActor(args) {
  
	val disambigURL: String = "https://www.googleapis.com/freebase/v1/search?key=AIzaSyDgEm2hDVbVeruKXokiMyrShPZaVL4VICU&query=";
  
	// Ready
	ready()
	
	def receive = {
	  case MetadataContainer(metaData) =>
	    val origin = sender
	    process(metaData.copy, origin)
	}
	
	// Process
	def process(metaData:MetaData, origin:ActorRef) {
	  
	  // Get ids
	  val futures: List[Future[Option[Keyword]]] = metaData.keywords.toList map { keyword =>
	  	val url = defineURL(keyword)
	  	Future { fetch(url, keyword)   }
      }
	  
	  // Sequence list
	  Future.sequence(futures) onComplete {
	  	case Success(completed) => 
	  	  reply(origin, MetadataContainer(metaData))
	  	case Failure(e) => 
	  	  log.error("An error has occurred: " + e.getMessage())
	  	  reply(origin, None)
	  }
	  
	}
	
	// Determine best URL for keyword
	def defineURL(keyword:Keyword): String = {
		
		// Disambig term
		var disambigTerm = keyword.original_text
		
		// If it looks like we have a stock ticker, try the company name instead
		if (YahooFinance.possibleTicker(disambigTerm)) {
			YahooFinance.validTickerSymbol(disambigTerm) match {
				case Some(company_name) => disambigTerm = company_name
				case None =>
			}
		}
		
		// Buld url
		val url = disambigURL + disambigTerm
		return url
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