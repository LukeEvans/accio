package com.reactor.accio.storage
import com.reactor.accio.metadata.connections.ConnectionSet
import com.tinkerpop.rexster.client.RexsterClient
import com.tinkerpop.rexster.client.RexsterClientFactory
import java.util.List
import java.util.HashMap
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.ArrayList

// GraphDB Master
class GraphDB {
  
	val rexster = RexsterClientFactory.open("ec2-54-211-25-154.compute-1.amazonaws.com");
//	val rexster = RexsterClientFactory.open("10.170.43.190");
			
//	val rexster:RexsterClient = RexsterClientFactory.open("ec2-54-221-10-7.compute-1.amazonaws.com");
  	val mapper = new ObjectMapper()
  	
	// Find Connections
	def findConnections(set:ConnectionSet): Option[ConnectionSet] = {
		
  		try {
			val mid1 = set.source_id
			val mid2 = set.target_id
			
			val results:List[String] = rexster.execute("g = rexster.getGraph('reactorgraph'); r = new Reactor(g); r.defineConnections(mid1,mid2)", 
					    new HashMap[String, Object](){{
					        put("mid1", mid1);
					        put("mid2", mid2);
					    }});
	
				val jsonString = results.get(0); 
				val setNode = mapper.readTree(jsonString);
				
				val newSet = new ConnectionSet(setNode);
				Some (newSet)
  		} catch {
  			case e:Exception => {
  				return None
  			}
  		}
	}
  	
  	// Fine details
  	def findVertexDetails(mid:String): Option[JsonNode] = {
		try {
			val results:List[String] = rexster.execute("g = rexster.getGraph('reactorgraph'); r = new Reactor(g); r.findDetailsForMid(mid)", 
					new HashMap[String, Object](){{
						put("mid", mid);
					}})
			
			
			val jsonString = results.get(0)
			val detailNode = mapper.readTree(jsonString)
			return Some (detailNode)

		} catch {
			case e:Exception => {
				return None
			}
		}  		
  	}
  	
  	// Find confluence node
	def findConfluenceIDs(topic:ArrayList[String], related:ArrayList[String] = new ArrayList[String]()): Option[JsonNode] = {
		try {
			val results:List[String] = rexster.execute("g = rexster.getGraph('reactorgraph'); r = new Reactor(g); r.findConfluence(a,b)", 
				    new HashMap[String, Object](){{
				        put("a", topic);
				        put("b", related);
				    }});
			
			val jsonString = results.get(0);
			val confluenceNode = mapper.readTree(jsonString);
			
			return Some ( confluenceNode )
			
		} catch {
			case e:Exception => {
				None
			}
		}
	}  	
}