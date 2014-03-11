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

	// Base url
	val baseURL = """http://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=1000&titles="""
		
	// Ready
	ready()

	def receive = {
		case CandidateContainer(candidate) =>
			val origin = sender
			processQuery(origin, candidate)
	}	

	// Process
	def processQuery(origin:ActorRef, candidate:Candidate) {

		val photoStrings = ArrayBuffer[String]()
		
		// If we don't have any idea where to check wikipedia. Just return 
		if (candidate == null || candidate.wikipedia_title == null || candidate.wikipedia_title.size <= 1) {
			reply(origin, Some (photoStrings))
		}
		
		Tools.fetchURL(baseURL + candidate.wikipedia_title) match {
			case Some (fetched) =>
				try {
					val thumbnailNode = parseThumbnail(fetched)
					val photo = new WikiPhoto(thumbnailNode)
					
					if (photo.photoValid) {
						photoStrings += photo.source
					}
					
					reply(origin, Some (StringList(photoStrings)) )
				}
				
				catch {
					case e:Exception =>
						reply(origin, None)
				}
				
			case None => reply(origin, None)
		}
		
	}
	
	// Parse thumbnail out of response
	def parseThumbnail(response:JsonNode): JsonNode = {
		val thumbnail = response.get("query").get("pages").findValue("thumbnail")
		return thumbnail
	}
}

// Wikipedia Photo
class WikiPhoto(photoNode:JsonNode) extends TransportMessage {
	
	val source = if (photoNode.has("source")) photoNode.get("source").asText() else null
	val height = if (photoNode.has("height")) photoNode.get("height").asInt() else 0
	val width = if (photoNode.has("width")) photoNode.get("width").asInt() else 0

	def photoValid: Boolean = {
		if (height >= 460 && width >= 640) {
			return true
		}
		
		else {
			return false
		}
	}
}
