package com.reactor.accio.pipeline.gather

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

class FlickrGatherer(args: FlowControlArgs) extends FlowControlActor(args) {

	val baseDataUrl = "http://api.flickr.com/services/rest/?&method=flickr.photos.search&format=json&sort=relevance"
	val apiKey = "ee914fcffa514b5081adc20bef2f6186"	
	val maxPhotos = 5

	// Ready
	ready()

	def receive = {
		case query:String =>
			val origin = sender
			processQuery(origin, query)
			complete()
		case KeywordContainer(keyword) =>
		  processKeyword(keyword, sender)
		  complete()
	}	

	// Process
	def processQuery(origin:ActorRef, query:String) {

		val response = Tools.fetchFlickrURL(baseDataUrl
				+ "&api_key=" + apiKey 
				+ "&text=" + query)	

		response match {
			case Some ( responseNode ) =>
			
				try {
					val photoNode = responseNode.get("photos").get("photo").get(0)
					val photo = new FlickrPhoto(photoNode)
					
					reply(origin, Some (photo.url) )
					return
				}
				
			case None =>
		}
		
		// Reply
		reply(origin, None)
	}
	
	def processKeyword(keyword:Keyword, origin:ActorRef){
		val response = Tools.fetchFlickrURL(baseDataUrl
				+ "&api_key=" + apiKey 
				+ "&text=" + keyword.candidates.get(0).name
				+ "&per_page=" + 5)	

		response match {
			case Some ( responseNode ) =>
			
				try {
					val data = extractData(responseNode.get("photos").get("photo"))
					
					reply(origin, ConfluenceNodeList(data.take(maxPhotos)))
					return
				}
				
			case None =>
		}
		
		// Reply
		reply(origin, ConfluenceNodeList(ArrayBuffer[Any]()))
	}
	
  def extractData(dataNode:JsonNode):ArrayBuffer[Any] = {
    if(dataNode == null)
      null
      
    val data = new ArrayBuffer[Any]
    for(node <- dataNode)
      data.add(new FlickrPhoto(node))
      
    data
  }
}



// Youtube video class
class FlickrPhoto(photoNode:JsonNode) extends TransportMessage {
		
	val id = if (photoNode.has("id")) photoNode.get("id").asText() else null
	val owner = if (photoNode.has("owner")) photoNode.get("owner").asText() else null
	val secret = if (photoNode.has("secret")) photoNode.get("secret").asText() else null
	val server = if (photoNode.has("server")) photoNode.get("server").asText() else null
	val farm = if (photoNode.has("farm")) photoNode.get("farm").asText() else null
	val story_type = "flickr"
	val url = "http://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + ".jpg"
}
