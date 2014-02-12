package com.reactor.accio.graphdb

import com.reactor.base.patterns.pull.Master
import com.reactor.base.patterns.pull.Worker
import akka.actor.ActorRef
import com.reactor.base.patterns.monitoring.MonitoredActor
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.cluster.routing.ClusterRouterConfig
import akka.routing.RoundRobinRouter
import akka.cluster.routing.ClusterRouterSettings
import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.base.patterns.pull.FlowControlArgs
import com.reactor.accio.metadata.connections.ConnectionSet
import com.tinkerpop.rexster.client.RexsterClient
import com.tinkerpop.rexster.client.RexsterClientFactory
import java.util.List
import java.util.HashMap
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

// GraphDB Master
class GraphDB {
  
	val rexster:RexsterClient = RexsterClientFactory.open("ec2-54-221-10-7.compute-1.amazonaws.com");
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
				println(jsonString)
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
}