package com.reactor.accio.pipeline.gather

import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.base.patterns.pull.FlowControlArgs
import akka.actor.ActorRef
import com.reactor.base.utilities.Tools
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._
import com.fasterxml.jackson.databind.JsonNode
import com.reactor.accio.transport.ConfluenceNodeList

class ItunesGatherer(args: FlowControlArgs) extends FlowControlActor(args) {
  val baseUrl = "https://itunes.apple.com/search?"
    
  ready()
  
  def receive ={
    case query:String =>
      processQuery(query, sender)
      complete()
    case a:Any => println(a)
  }
  
  def processQuery(query:String, origin:ActorRef) {
    Tools.fetchURL(baseUrl
    		+ "term=" + query
    		+ "&limit=" + 5) match{
      case Some(response) =>
        if(response.has("results")){
          val data = extractData(response.get("results"))
          reply(origin, ConfluenceNodeList(data))
          return
        }
      case None =>
        println("No response from itunes api")
    }
    reply(origin, ConfluenceNodeList(null))
  }
  
  def extractData(nodeList:JsonNode):ArrayBuffer[Any] = {
    if(nodeList == null)
      null
      
    val data = ArrayBuffer[Any]()
    for(node <- nodeList)
      data add node
    data
  }
}