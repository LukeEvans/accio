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

// Extractor actor
class Extractor(args:FlowControlArgs) extends FlowControlActor(args) {
  
	val nlpURL:String = "http://access.alchemyapi.com/calls/text/TextGetRankedKeywords?apikey=fb5e30fc9358653bee86ffd698ed024aae33325c&outputMode=json&text=";
	
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
	  
	  // Get Alchemy results
	  val results = Tools.fetchURL(nlpURL + metaData.free_text)
	  
	  results match {
	    case Some(results) =>
		  
	      // Extract keywords from Alchemy
		  for (r <- results.path("keywords")) {
		    metaData.addKeyword(new Keyword(r));
		  }	     

		  // Additional keywords based on hints
		  additionalKeywords(metaData.free_text) map { k => 
		    metaData.addKeyword(new Keyword(k)) 
		  }
		  
		  // Reply
		  reply(origin, MetadataContainer(metaData.copy()))
	  
	    case None =>
	  }
	  
	}
	
	// Additional Keywords
	def additionalKeywords(text:String): ArrayBuffer[String] = {
	  val hints = new ArrayBuffer[String]
	  
	  // Capitals
	  val capsPattern = Pattern.compile("""[A-Z]\w+\s+[A-Z]\w+""");
	  val m1 = capsPattern.matcher(text);
	  while (m1.find()) {
		hints += m1.group(0)
	  }

	  // Determinates
	  val determinatePattern = Pattern.compile("""(?:the|an|a|that|this)\s+([A-Z]\w+)(?:[\s\W]($|[^A-Z]))""");
	  val m2 = determinatePattern.matcher(text);
	  while (m2.find()) {
		hints += m2.group(0)
	  }	  
	  
	  return hints
	} 
}