package com.reactor.accio.storage

import com.mongodb.casbah.MongoURI
import java.util.HashMap
import com.mongodb.BasicDBObject
import com.mongodb.casbah.commons.MongoDBObject
import scala.collection.JavaConversions._

class Mongo {
  val uri = MongoURI("mongodb://levans002:dakota1@ds031887.mongolab.com:31887/winston-db")
  val db = uri.connectDB
  val collection = "winston-news"
  val coll = db.right.get.getCollection(collection)
  
  // Fetch news
  def fetchNewsByID(id:String): Option[HashMap[String, Any]] = {
  	val query = MongoDBObject("_id" -> id)
  	val obj = coll.findOne( query )
			
	val value = obj.toMap().asInstanceOf[HashMap[String, Any]]
			
	value.remove("_id");
	value.remove("_class");
	value.put("id", id);
	
	Some ( value )
  }
}