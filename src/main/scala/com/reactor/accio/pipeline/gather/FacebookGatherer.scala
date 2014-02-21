package com.reactor.accio.pipeline.gather

import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.base.patterns.pull.FlowControlArgs
import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._
import com.reactor.base.utilities.Tools
import com.fasterxml.jackson.databind.JsonNode
import com.reactor.accio.transport.KeywordsContainer
import com.reactor.accio.transport.AccioRequest
import java.util.ArrayList
import com.reactor.accio.metadata.Keyword
import com.reactor.accio.transport.ConfluenceNodeList
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

class FacebookGatherer(args:FlowControlArgs) extends FlowControlActor(args) {
  // Initiate
  ready()
  
  def receive = {
    case KeywordsContainer(keywords, request) => process(keywords, request, sender)
    case a:Any => println("FacebookGatherer: Unkown Message received - " + a)
  }
  
  def process(keywords:ArrayList[Keyword], request:AccioRequest, origin:ActorRef){
    val confluenceNodes = new ArrayBuffer[Any]   
    val queryStrings = new ListBuffer[String]
    val token = request.facebook_token
    
    keywords map{
      keyword =>
        queryStrings.add(keyword.original_text)
        if(!keyword.candidates.isEmpty)
          queryStrings.add(keyword.candidates.get(0).name)
    }
    
    val futures = new ArrayBuffer[Future[ArrayBuffer[Any]]]
    
    queryStrings map {
      query => futures += Future{ FacebookAPI.searchFriendsPosts(query, token)}
    }
    
    Future.sequence(futures) onComplete{
      case Success(completed) =>
        completed map{
          data => confluenceNodes += data         
        }
        reply(origin, ConfluenceNodeList(confluenceNodes))
      case Failure(e) =>
        e.printStackTrace()     
    }
  }
}

private object FacebookAPI{
  val baseUrl = "https://graph.facebook.com/"
  
  def searchFriendsPosts(topic:String, token:String):ArrayBuffer[Any] ={
    println("Searching facebook for - " + topic)
    Tools.fetchURL(baseUrl +
    		      	"me/home?q=" + topic +
    		      	"&access_token=" + token) match{
      case None => null
      case Some(response) => extractData(response.get("data"))
    }
  }
  
  def extractData(dataNode:JsonNode):ArrayBuffer[Any] = {
    if(dataNode == null)
      null
    
    val data = new ArrayBuffer[Any]  
    for(node <- dataNode)
      data.add(node)

    data
  }
} 