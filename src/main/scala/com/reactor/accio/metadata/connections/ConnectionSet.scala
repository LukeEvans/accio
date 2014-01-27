package com.reactor.accio.metadata.connections

import java.util.ArrayList
import com.fasterxml.jackson.databind.JsonNode
import scala.collection.JavaConversions._

class ConnectionSet {

	var source_id:String = null;
	var target_id:String = null;
	var connections:ArrayList[Connection] = new ArrayList[Connection];
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(s:String, t:String) {
		this()
		source_id = s;
		target_id = t;
		connections = new ArrayList[Connection]
	}
	
	def this(node:JsonNode) {
		this()
		source_id = node.path("source_id").asText();
		target_id = node.path("target_id").asText();
		connections = new ArrayList[Connection]
		
		// Build the connections one by one
		node.path("connections").toList map { n =>
			if (n.path("connection_type").asText().equals("mediator_relationship")) {
				connections.add(new MediatorRelationship(n));
			}
			
			if (n.path("connection_type").asText().equals("direct_relationship")) {
				connections.add(new Relationship(n));
			}
			
			if (n.path("connection_type").asText().equals("similarly_notable")) {
				connections.add(new Similarity(n));
			}
			
			if (n.path("connection_type").asText().equals("common_relationship")) {
				connections.add(new CommonRelationship(n));
			}
			
		}
	}
	//================================================================================
	// Add Connection
	//================================================================================
	def addConnection(c:Connection) {
		connections.add(c);
	}
	
	//================================================================================
	// Determine if we have any connections
	//================================================================================
	def connected(): Boolean = {
		return connections.size() != 0;
	}

	//================================================================================
	// Determine if we have a common type
	//================================================================================
	def commonTypeExists(): Boolean = {
		connections.toList map { c => 
			if (c.isInstanceOf[Similarity]) {
				return true;
			}
		}
		
		return false;
	}  
}