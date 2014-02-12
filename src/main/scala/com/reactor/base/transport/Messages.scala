package com.reactor.base.transport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import org.elasticsearch.rest.RestResponse
import spray.routing.RequestContext

abstract class RESTRequest(data:Any) extends Serializable
abstract class RESTResponse(data:Any) extends Serializable {
    def finish(startTime:Long, mapper:ObjectMapper): String = {
    	return mapper.writeValueAsString(data)
  }
}

// Error
case class Error(status: String)

// Accio request and responses
case class RequestContainer(req:RESTRequest)
case class ResponseContainer(resp:RestResponse)

// Dispatch messages
case class DispatchRequest(request:RequestContainer, ctx:RequestContext, mapper:ObjectMapper)
case class OverloadedDispatchRequest(message:Any)
  
// HTTP Request
case class HttpObject(uri: String, obj: JsonNode = null, response: JsonNode = null, method: String = "GET") 
case class JsonResponse(node: JsonNode)