package com.reactor.accio.pipeline.images

import com.reactor.base.patterns.pull.FlowControlArgs
import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.accio.transport.IdList
import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer
import com.reactor.accio.storage.Mongo
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Failure
import java.util.HashMap
import com.reactor.accio.transport.ConfluenceNodeList
import com.reactor.accio.storage.Elasticsearch
import com.reactor.base.utilities.Tools
import com.fasterxml.jackson.databind.JsonNode
import java.util.Date
import scala.collection.JavaConversions._
import java.text.SimpleDateFormat
import com.reactor.accio.transport.ConfluenceNodeList
import com.reactor.accio.transport.TransportMessage
import com.reactor.accio.transport.KeywordContainer
import com.reactor.accio.metadata.Keyword
import com.reactor.accio.transport.CandidateContainer
import com.reactor.accio.pipeline.gather.FlickrPhoto
import com.reactor.accio.metadata.Candidate
import com.reactor.accio.transport.StringList
import com.reactor.accio.transport.StringList

class WikiImageFetcher(args: FlowControlArgs) extends FlowControlActor(args) {

	// Ready
	ready()

	def receive = {
		case CandidateContainer(candidate) =>
			val origin = sender
			processQuery(origin, candidate)
			complete()
	}	

	// Process
	def processQuery(origin:ActorRef, candidate:Candidate) {

		val photoStrings = ArrayBuffer[String]()
		
		photoStrings += "YOYOYO_Wiki.jpg"
		
		// Reply
		reply(origin, Some( StringList(photoStrings)))
	}
}
