package com.reactor.accio.metadata

import java.util.ArrayList
import com.fasterxml.jackson.databind.JsonNode
import scala.collection.JavaConversions._
import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.lang.builder.HashCodeBuilder
import com.reactor.accio.transport.TransportMessage

class Candidate extends TransportMessage {

	var name:String = null
	var id:String = null
	var notable_for:String = null
	var notable_type:String = null
	var types:ArrayList[String] = new ArrayList[String]
	var wikipedia_title:String = null
	var wikipedia_description:String = null
	var mid:String = null
	var candidate_type:String = null
	var images:ArrayList[String] = new ArrayList[String]
	var icon:String = null

	//================================================================================
	// Constructors
	//================================================================================
	def this(node:JsonNode) {
		this()
		
		try {
			name = node.path("name").asText();
			id = node.path("mid").asText();

			types = new ArrayList[String]

			// Translate id to the graph version of the id
			translateMid();
			
			// Set static fields
			candidate_type = "Wikipedia";
			icon = "https://s3.amazonaws.com/Channel_Icons/Wikipedia-logo-v2.png";
			images = new ArrayList[String]

		} catch {
		  	case e:Exception => e.printStackTrace();
		}
	}

	//================================================================================
	// Translate mid to Titan mid
	//================================================================================
	def translateMid() {
		try {

			if (id.startsWith("/m/")) {
				mid = id.replaceAll("/m/", "ns:m.");
			}

			else if (id.startsWith("/g/")) {
				mid = id.replaceAll("/g/", "ns:g.");
			}

		} catch {
			case e:Exception => e.printStackTrace();
		}
	}

	//================================================================================
	// Get translated id
	//================================================================================
	def grabMid(): String = {
		return mid;
	}

	//================================================================================
	// Grab metadata from Vertex
	//================================================================================
	def grabVertexMetaData(details:JsonNode) {

		try {
			wikipedia_description = details.path("wikipedia_description").asText();
			wikipedia_title = details.path("wikipedia_title").asText();
			
			try {
				notable_for = details.path("notable_for").asText().replaceAll("@en", "");
				notable_type = details.path("notable_type").asText().replaceAll("@en", "");
			} catch {
				case e:Exception => e.printStackTrace();
			}
			
			if (types == null) {
				types = new ArrayList[String]
			}
			
			details.path("types").toList map { n => 
				types.add(n.asText());
			}
			
		} catch {
		  	case e:Exception => e.printStackTrace();
			e.printStackTrace();
			// Ignore
		}
	}

	//================================================================================
	// House Keeping
	//================================================================================
	override def toString(): String = {
		return name + " -- " + mid + "\n";
	}

	def equals(obj:Candidate): Boolean = {
		try { 
			val other = obj.asInstanceOf[Candidate]
			if (mid.equalsIgnoreCase(other.mid) && (name.equalsIgnoreCase(other.name))) {
				return true;
			}
			return false;

		} catch {
		  	case e:Exception => return false;
		}
	}

	override def hashCode(): Int = {
		return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
				append(mid).
				append(name).
				toHashCode();
	}  
}