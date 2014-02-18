package com.reactor.accio.metadata.connections

import com.fasterxml.jackson.databind.JsonNode
import com.reactor.accio.transport.TransportMessage

// Connection
class Connection() extends TransportMessage {

	var connection_type:String = "connection"
	
	//================================================================================
	// Define connection type
	//================================================================================
	def markConnectionType(node:JsonNode) {
		connection_type = node.path("connection_type").asText()
	}
}

// Common Relationship
class CommonRelationship extends Connection {
	connection_type = "common_relationship"
	var common_edge_label:String = null
	var common_vertex:DisplayVertex = null
	
	def this(v:DisplayVertex) {
		this()
		common_vertex = v
	}
	
	def this(v:DisplayVertex, e:String) {
		this()
		common_vertex = v
		common_edge_label = e
	}
	
	def this(node:JsonNode) {
		this()
		markConnectionType(node)
		
		common_edge_label = node.path("common_edge_label").asText()
		common_vertex = new DisplayVertex(node.path("common_vertex"))
	}	
}

// Relationship
class Relationship extends Connection {

	connection_type = "direct_relationship"
	var edge_label:String = null
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(e:String) {
		this()
		edge_label = e
	}
	
	def this(node:JsonNode) {
		this()
		markConnectionType(node)
		
		edge_label = node.path("edge_label").asText()
	}
}

// Mediator Relationship
class MediatorRelationship extends Relationship {
	
	connection_type = "mediator_relationship"
	var mediator:Mediator = null
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(e:String) {
		this()
		edge_label = e;
	}
	
	def this(e:String, m:Mediator) {
		this()
		edge_label = e;
		mediator = m;
	}
	
	def this(node:JsonNode) {
		this()
		markConnectionType(node)
		
		mediator = new Mediator(node.path("mediator"));
	}
}

// Similarity 
class Similarity extends Connection {

	connection_type = "similarly_notable"
	var common_type:String = null
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(t:String) {
		this()
		common_type = t
	}
	
	def this(node:JsonNode) {
		this()
		markConnectionType(node)
		
		common_type = node.path("common_type").asText();
	}
}

