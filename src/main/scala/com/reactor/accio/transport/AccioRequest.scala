package com.reactor.accio.transport

import com.fasterxml.jackson.databind.ObjectMapper
import spray.http.HttpRequest
import com.reactor.base.transport.RESTRequest

class AccioRequest extends RESTRequest {

	@transient
	val mapper = new ObjectMapper()
	var text:String = null
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(request:String) {
	  this()
	  
	  var cleanRequest = request.replaceAll("\\r", " ").replaceAll("\\n", " ").trim();
	  val reqJson = mapper.readTree(cleanRequest);

	  text = if (reqJson.has("text")) reqJson.path("text").asText() else null
	}
	
	def this(request:HttpRequest) {
	  this()
	  
	  text = if (request.uri.query.get("text") != None) request.uri.query.get("text").get else null
	  
	}
}