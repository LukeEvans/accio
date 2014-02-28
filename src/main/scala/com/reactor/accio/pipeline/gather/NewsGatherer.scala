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

class NewsGatherer(args: FlowControlArgs) extends FlowControlActor(args) {
	
	// Mongo
	val mongo = new Mongo()
	val maxNews = 8
	
	// Ready
	ready()
	
	def receive = {
		case IdList(list) =>
			val origin = sender
			process(origin, list)
			complete()
	}	
	
	// Process
	def process(origin:ActorRef, idList:ArrayBuffer[String]) {
		val confluenceNodes = ArrayBuffer[Any]()
		
		val futures = new ArrayBuffer[Future[Option[Any]]]()
		
		// Send off all news
		idList map { id => futures += Future { fetch(id)} }
		
		// Sequence list
		Future.sequence(futures) onComplete {
	  		case Success(completed) =>
	  			
	  			completed map { newsOption => 
	  				newsOption match {
	  					case Some( news ) =>
	  						confluenceNodes += news
	  					case None => 
	  				}
	  			}
	  			
	  			reply(origin, ConfluenceNodeList(confluenceNodes.take(maxNews)))
	  			
	  		case Failure(e) => 
	  			log.error("An error has occurred: " + e.getMessage())
	  			reply(origin, ConfluenceNodeList(new ArrayBuffer[Any]()))
		}			
	}
	
	// Fetch 
	def fetch(id:String): Option[Any] = {
		val result = mongo.fetchNewsByID(id)
		return result
	}
}