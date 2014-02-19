package com.reactor.accio.metadata.connections

import com.fasterxml.jackson.databind.JsonNode
import com.reactor.accio.transport.TransportMessage

class DisplayEdge extends TransportMessage {

	var edge:String = null
	var vertex_id:String = null
	var vertex_name:String = null
	
	//================================================================================
	// Constructor
	//================================================================================
	def this(e:String, i:String, n:String) {
		this()
		edge = e;
		vertex_id = i;
		vertex_name = n;
	}
	
	def this(node:JsonNode) {
		this()
		edge = node.path("edge").asText();
		vertex_id = node.path("vertex_id").asText();
		vertex_name = node.path("vertex_name").asText();
	}
}
