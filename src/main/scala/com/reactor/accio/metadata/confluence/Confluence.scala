 package com.reactor.accio.metadata.confluence
 
import java.util.ArrayList
 import scala.util.Random
 
class Confluence() {

	 val confluence_matrix = new ArrayList[ArrayList[Any]]()
		 
	 // Add a list of confluence nodes
	 def addConfluenceNodes(confluenceNodes:ArrayList[Any]) {
		 confluence_matrix.add(confluenceNodes)
	 }
	 
}