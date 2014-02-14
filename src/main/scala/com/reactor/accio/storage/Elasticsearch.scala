package com.reactor.accio.storage

import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.base.patterns.pull.FlowControlArgs
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.client.transport.TransportClient
import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import java.util.HashMap
import org.elasticsearch.action.search.SearchResponse
import scala.collection.JavaConversions._

// Elasticsearch Actor
class Elasticsearch() {
	
  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", "es_sg").build();
  val client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress("ec2-54-211-99-5.compute-1.amazonaws.com", 9300));
  
  val mapper = new ObjectMapper()
  
  def findTwitterByID(id:String): Option[HashMap[String, Any]] = {
  	val result = client.prepareSearch("twitter").setQuery(QueryBuilders.queryString(id).defaultField("id")).execute().actionGet()
  	
  	getTopJsonFromSearch(result) match {
  		case Some( top ) =>
  			return Some ( top )
  		case None =>
  	}
  	
  	None
  }
  
  	
  def getTopJsonFromSearch(response:SearchResponse): Option[HashMap[String, Any]] = {
		try {
			var top:HashMap[String, Any] = null
			for(hit <- response.getHits().toList){
				val smap = hit.getSource().asInstanceOf[HashMap[String, Any]]
				val db = smap.get("db").toString();
				if(smap.get("valid") != null && (smap.get("valid").asInstanceOf[Boolean] || db.equalsIgnoreCase("Twitter"))){
					top = smap
					return Some ( top )
				}
			}

		} catch {
			case e:Exception => {
				return None
			}
		}
		
		None
	}  
}