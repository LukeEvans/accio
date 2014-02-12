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
class Confluence(args:FlowControlArgs) extends FlowControlActor(args) {
  
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
	  
	}
}