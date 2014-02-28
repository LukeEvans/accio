 package com.reactor.accio.metadata.confluence
 
 import java.util.ArrayList
 import scala.collection.JavaConversions.asScalaBuffer
 import scala.collection.mutable.ArrayBuffer
 import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
 import com.fasterxml.jackson.module.scala.DefaultScalaModule
 import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
 import com.reactor.accio.transport.TransportMessage
 import java.util.HashMap
 
class Confluence() extends TransportMessage {

	 val confluence_matrix = ArrayBuffer[ArrayBuffer[Any]]()

     // Mapper    
	 @transient
     var mapper = new ObjectMapper() with ScalaObjectMapper
      	mapper.registerModule(DefaultScalaModule)
      	mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
      
	 // Add a list of confluence nodes
	 def addConfluenceNodes(confluenceNodes:ArrayList[Any]) {
		 val confluenceNodeBuffer = ArrayBuffer[Any]()
		 
		 confluenceNodes map { n => confluenceNodeBuffer += translateConfluenceNode(n) }
		 
		 confluence_matrix += confluenceNodeBuffer
	 }
	 
	 // Translate Confluence Node
	 def translateConfluenceNode(node:Any): HashMap[String, Any] = {
		 
		 // Re init mapper if it's null
		 if (mapper == null) {
			      mapper = new ObjectMapper() with ScalaObjectMapper
			        mapper.registerModule(DefaultScalaModule)
			        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
		 }
		 
		 val json = mapper.writeValueAsString(node)
		 val mapObj = mapper.readValue(json, classOf[HashMap[String, Any]])
		 
		 // If we're dealing with updated news
		 if (mapObj.containsKey("story_type")) {
			 return mapObj
		 }

		 // Change "type" fields to "story_type"
		 if (mapObj.containsKey("type")) {
			 val t = mapObj.get("type") 
			 
			 t match {
				 case "News" => mapObj.put("story_type", "news")
				 case string:String => 
					 mapObj.put("story_type", string.toLowerCase())
					 mapObj.remove("type")
			 }
		 }
		 
		 return mapObj
	 }
}