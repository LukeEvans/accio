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

class FlickrFetcher(args: FlowControlArgs) extends FlowControlActor(args) {

	val baseDataUrl = "http://api.flickr.com/services/rest/?&method=flickr.photos.search&format=json&sort=relevance"
	val apiKey = "ee914fcffa514b5081adc20bef2f6186"	
	val maxPhotos = 5

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
		
		val response = Tools.fetchFlickrURL(baseDataUrl
				+ "&api_key=" + apiKey 
				+ "&text=" + candidate.name)	

		response match {
			case Some ( responseNode ) =>
			
				try {
					for (photoNode <- responseNode.get("photos").get("photo")) {
						val photo = new FlickrPhoto(photoNode)
						photoStrings += photo.url
					}
					
					reply(origin, Some (StringList(photoStrings)) )
					return
				}
				
			case None =>
		}
		
		// Reply
		reply(origin, None)
	}
}
