package com.reactor.base.transport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import spray.routing.RequestContext

abstract class RESTRequest(data:Any) extends Serializable
class RESTResponse(data:Any) extends Serializable {
	
	var finalData = data
	
    def finish(startTime:Long, mapper:ObjectMapper): String = {
    	return mapper.writeValueAsString(finalData)
  }
}

// Error
case class Error(status: String)

// Accio request and responses
case class RequestContainer(req:RESTRequest)
case class ResponseContainer(resp:RESTResponse)

// Dispatch messages
case class DispatchRequest(request:RequestContainer, ctx:RequestContext, mapper:ObjectMapper)
case class OverloadedDispatchRequest(message:Any)
  
// HTTP Request
case class HttpObject(uri: String, obj: JsonNode = null, response: JsonNode = null, method: String = "GET") 
case class JsonResponse(node: JsonNode)