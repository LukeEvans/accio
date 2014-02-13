 package com.reactor.accio.metadata.confluence
 
 import scala.collection.mutable.ArrayBuffer

class ConfluenceNode()

class Confluence {

	 val confluence_matrix = ArrayBuffer[ArrayBuffer[Any]]()
	 
	 // Add a list of confluence nodes
	 def addConfluenceNodes(confluenceNodes:ArrayBuffer[Any]) {
		 confluence_matrix += confluenceNodes
	 }
}