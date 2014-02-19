package com.reactor.accio.metadata.connections

import java.util.ArrayList
import com.fasterxml.jackson.databind.JsonNode
import scala.collection.JavaConversions._
import java.util.ArrayList
import com.reactor.accio.transport.TransportMessage

// Display Vertex
class DisplayVertex extends TransportMessage {
  
	var name:String = null
	var id:String = null
	var types:ArrayList[String] = new ArrayList[String]
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(mid:String) {
		this()
		id = mid;
	}
	
	def this(mid:String, n:String) {
		this()
		
		init(mid, n, types)
	}
	
	def this(mid:String, n:String, t:ArrayList[String]) {
		this()
		
		init(mid, n, t)
	}
	
	def this(node:JsonNode) {
		this()
		
		init(node)
	}
	
	def init(mid:String, n:String, t:ArrayList[String]) {
		name = n;
		id = mid;
		types = t;
		
		if (name == null) {
			name = "";
		}	  
	}
	
	def init(node:JsonNode) {
		name = node.path("name").asText();
		id = node.path("id").asText();
		types = new ArrayList[String]

		node.path("types").toList map { n =>
		  types.add(n.asText())
		}	  
	}
}


class Mediator extends DisplayVertex {

	var edges:ArrayList[DisplayEdge] = new ArrayList[DisplayEdge];
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(mid:String, n:String, t:ArrayList[String], e:ArrayList[DisplayEdge]) {
		this()
		
		init(mid, n, t)
		edges = e;
		buildName();
	}
	
	def this(node:JsonNode) {
		this()
		
		init(node)
		node.path("edges").toList map { n => 
			edges.add(new DisplayEdge(n));
		}
	}
	
	//================================================================================
	// Build Name
	//================================================================================
	def buildName() {
		if (edges == null) {
			return;
		}
		
		edges.toList map { e => 
			if (e != null && e.vertex_name != null) {
				name += e.vertex_name + " -- ";
			}
		}
		
		name = name.substring(0, name.length()-4);
	}
}