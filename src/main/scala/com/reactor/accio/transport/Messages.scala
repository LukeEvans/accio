package com.reactor.accio.transport

import scala.collection.mutable.ArrayBuffer
import spray.http.HttpRequest
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._
import com.reactor.base.transport.RESTRequest
import com.reactor.base.transport.RESTResponse
import com.reactor.accio.metadata.MetaData
import com.fasterxml.jackson.databind.ObjectMapper
import com.reactor.accio.metadata.Candidate


case class MetadataContainer(metadata:MetaData)
case class CandidateList(candidates:ArrayBuffer[Candidate]) 
case class ConfluenceNodeList(confluenceNodes:ArrayBuffer[Any]) 
case class IdList(ids:ArrayBuffer[String])  
case class StringList(strings:ArrayBuffer[String])

// Accio Request
class AccioRequest extends RESTRequest {

	@transient
	val mapper = new ObjectMapper()
	var text:String = null
	var alt:Boolean = false                                                                                                                                                        
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(request:String) {
	  this()
	  
	  var cleanRequest = request.replaceAll("\\r", " ").replaceAll("\\n", " ").trim();
	  val reqJson = mapper.readTree(cleanRequest);

	  text = if (reqJson.has("text")) reqJson.path("text").asText() else null
	  alt = if (reqJson.has("alt")) reqJson.path("alt").asBoolean() else false
	}
	
	def this(request:HttpRequest) {
	  this()
	  
	  text = if (request.uri.query.get("text") != None) request.uri.query.get("text").get else null
	  alt = if (request.uri.query.get("alt") != None) request.uri.query.get("alt").get.asInstanceOf[Boolean] else false
	  
	}
}

// Accio Response
class AccioResponse(accioRequest:AccioRequest, metadata:MetaData) extends RESTResponse(metadata) {

	val newData = ArrayBuffer[ArrayBuffer[Any]]()
	
	if (accioRequest.alt) {
		wiki(3)
		confluence(3)
		finalData = newData
		
	}
	
	// Wiki
	def wiki(max:Int) {
		
		val wiki = ArrayBuffer[Any]()
		
		metadata.keywords.toList map { keyword =>
			if (keyword.candidates != null && !keyword.candidates.isEmpty()) {
				keyword.candidates.take(max).toList map { candidate =>
					wiki += candidate
				}
			}
		}	
		
		newData += wiki
	}
	
	// Confluence
	def confluence(max:Int) {
		metadata.confluence.confluence_matrix map { list =>
			newData += list.take(max)
		}
	}
}