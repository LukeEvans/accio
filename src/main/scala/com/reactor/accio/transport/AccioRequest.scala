package com.reactor.accio.transport

import com.fasterxml.jackson.databind.ObjectMapper
import spray.http.HttpRequest

class AccioRequest extends TransportMessage {

	@transient
	val mapper = new ObjectMapper()
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(request:String, rt:String) {
	  this()
	  
	  var cleanRequest = request.replaceAll("\\r", " ").replaceAll("\\n", " ").trim();
	  val reqJson = mapper.readTree(cleanRequest);
	}
	
	def this(request:HttpRequest, rt:String) {
	  this()
	}
}