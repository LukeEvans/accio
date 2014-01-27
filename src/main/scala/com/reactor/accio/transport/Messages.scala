package com.reactor.accio.transport

import spray.routing.RequestContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode

object Messages {

// Accio request and responses
case class RequestContainer(req:AccioRequest)
case class ResponseContainer(resp:AccioResponse)

// Error
case class Error(status: String)

// Dispatch messages
case class DispatchRequest(request:RequestContainer, ctx:RequestContext, mapper:ObjectMapper)
case class OverloadedDispatchRequest(message:Any)
  
// HTTP Request
case class HttpObject(uri: String, obj: JsonNode = null, response: JsonNode = null, method: String = "GET") 
case class JsonResponse(node: JsonNode)

}