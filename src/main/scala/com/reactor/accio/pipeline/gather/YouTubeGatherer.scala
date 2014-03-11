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

class YouTubeGatherer(args: FlowControlArgs) extends FlowControlActor(args) {

	val baseDataUrl = "https://www.googleapis.com/youtube/v3/"
			val apiKey = "AIzaSyDEv9_zQNRGpw789wTE5NbnUn4IywHUR5U"	
	
	val maxVideos = 3

	// Ready
	ready()

	def receive = {
		case query:String =>
			val origin = sender
			processQuery(origin, query)
	}	

	// Process
	def processQuery(origin:ActorRef, query:String) {
		val confluenceNodes = ArrayBuffer[Any]()

		val response = Tools.fetchURL(baseDataUrl
				+ "search?part=snippet&type=video&maxResults=10&regionCode=us&key="
				+ apiKey 
				+ "&q="
				+ query)	

		response match {
			case Some ( responseNode ) =>
			
				responseNode.path("items").toList.take(maxVideos) map { videoNode =>
					val youtubeVideo = new YoutubeVideo(videoNode)
					confluenceNodes += youtubeVideo
				}
			
			case None =>
		}
		
		// Reply
		reply(origin, ConfluenceNodeList(confluenceNodes.take(maxVideos)))
	}

}


// Youtube video class
class YoutubeVideo(videoNode:JsonNode) extends TransportMessage {

	var id:String = null
	var title:String = null
	var channel:String = null
	var embedded_html:String = null
	var description:String = null
	var thumbnail:String = null
	var date:Date = null
	var image_url = "https://s3.amazonaws.com/Channel_Icons/youtube_icon.png";
	var story_type = "youtube"
	var view_count = 0
	var likes = 0
	var dislikes = 0	

	if(videoNode.has("id") && videoNode.path("id").has("videoId"))
		id = videoNode.path("id").path("videoId").asText()
	if(id != null){
		embedded_html = "<iframe width=\"560\" height=\"315\" " + "src=\"//www.youtube.com/embed/" + id + " frameborder=\"0\" allowfullscreen></iframe>"
	}
	if(videoNode.has("snippet")){
		val snipNode = videoNode.get("snippet")
		if(snipNode.has("title"))
			title = snipNode.get("title").asText()
		if(snipNode.has("channelTitle"))
			channel = snipNode.get("channelTitle").asText()
		if(snipNode.has("description"))
			description = snipNode.get("description").asText()
		if(snipNode.has("thumbnails"))
			thumbnail = snipNode.get("thumbnails").get("high").get("url").asText()
		if(snipNode.has("publishedAt")){
			try{
				val dateString = snipNode.get("publishedAt").asText()
				date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(dateString)

			} catch {
				case e:Exception => {
					e.printStackTrace()
				}
			}
		}
	}
	
	if(videoNode.has("statistics")){
		val statNode = videoNode.get("statistics")
		if(statNode.has("viewCount"))
			view_count = statNode.get("viewCount").asInt()
		if(statNode.has("likeCount"))
			likes = statNode.get("likeCount").asInt()
		if(statNode.has("dislikeCount"))
			dislikes = statNode.get("dislikeCount").asInt()               
	}        
}