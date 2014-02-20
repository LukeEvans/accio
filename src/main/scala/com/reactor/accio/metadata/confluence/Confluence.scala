 package com.reactor.accio.metadata.confluence
 
import java.util.ArrayList
 import scala.util.Random
 import com.reactor.accio.transport.TransportMessage
 import scala.collection.mutable.ArrayBuffer
 import scala.collection.JavaConversions._
 
class Confluence() extends TransportMessage {

	 val confluence_matrix = ArrayBuffer[ArrayBuffer[Any]]()
		 
	 // Add a list of confluence nodes
	 def addConfluenceNodes(confluenceNodes:ArrayList[Any]) {
		 val confluenceNodeBuffer = ArrayBuffer[Any]()
		 
		 confluenceNodes map { n => confluenceNodeBuffer += n }
		 
		 confluence_matrix += confluenceNodeBuffer
	 }
	 
}