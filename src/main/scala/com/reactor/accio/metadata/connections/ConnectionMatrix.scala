package com.reactor.accio.metadata.connections

import java.util.ArrayList
import java.util.ArrayList
import com.reactor.accio.metadata.Keyword
import java.util.ArrayList
import scala.collection.JavaConversions._

class ConnectionMatrix {

	var connection_sets:ArrayList[ConnectionSet] = new ArrayList[ConnectionSet];
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(keywords:ArrayList[Keyword]) {
	    this()
		connection_sets = new ArrayList[ConnectionSet]
		init(keywords);
	}
	
	//================================================================================
	// Init
	//================================================================================
	def init(keywords:ArrayList[Keyword]) {

		for (i <- 0 until keywords.size() -1 ) {
			val k1 = keywords.get(i);

			for (j <- i+1 until keywords.size()) {
				val k2 = keywords.get(j);
				
				k1.candidates.toList map { c1 => 
					
				  	k2.candidates.toList map { c2 => 
						val set = new ConnectionSet(c1.grabMid(), c2.grabMid());
						connection_sets.add(set);
					}
				}
			}
			
		}
	}
	
	//================================================================================
	// Get all sets
	//================================================================================
	def grabSets(): ArrayList[ConnectionSet] = {
		return connection_sets;
	}
	
	//================================================================================
	// Prune all non-connected things
	//================================================================================
	def prune() {
		
		val newSet = new ArrayList[ConnectionSet]
		
		connection_sets.toList map { set => 
			if (set.connected()) {
				newSet.add(set);
			}
		}
		
		connection_sets = newSet;
	}
	
	//================================================================================
	// Get list of candidate ids that are allowed to stay
	//================================================================================
	def findConnectedIds(): ArrayList[String]  = {
		
		// First, prune the list
		prune();
		
		var ids = new ArrayList[String]
		
		connection_sets.toList map { set =>
			val s = set.source_id;
			val t = set.target_id;
			
			if (!ids.contains(s)) {
				ids.add(s);
			}
			
			if (!ids.contains(t)) {
				ids.add(t);
			}
		}
		
		return ids;
	}
	
	//================================================================================
	// Get list of ids that are common and therefore possibly related
	//================================================================================
	def findRelatedIds(): ArrayList[String] = {
		val relatedIds = new ArrayList[String]
		
		connection_sets.toList map { set =>
		  	set.connections.toList map { connection => 
				if (connection.isInstanceOf[CommonRelationship]) {
					val relationship = connection.asInstanceOf[CommonRelationship];
					
					val id = relationship.common_vertex.id;
					
					if (!relatedIds.contains(id)) {
						relatedIds.add(id);
					}
				}
			}
		}
		
		return relatedIds;
	}  
	
}