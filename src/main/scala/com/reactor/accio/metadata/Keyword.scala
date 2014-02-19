package com.reactor.accio.metadata

import java.util.ArrayList
import com.fasterxml.jackson.databind.JsonNode
import scala.collection.JavaConversions._
import java.util.ArrayList
import org.apache.commons.lang.builder.HashCodeBuilder
import com.reactor.accio.transport.TransportMessage

object Keyword {
  val MAX_CANDIDATES = 3
}

class Keyword extends TransportMessage {

	var original_text:String = null
	var connection_found:Boolean = false
	var candidates:ArrayList[Candidate] = null
	var additionalKeyword:Boolean = false
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(s:String) {
	     this()
		original_text = s;
		candidates = new ArrayList[Candidate];
	}
	
	def this (s:String, additional:Boolean) {
	    this()
		original_text = s;
		additionalKeyword = additional;
		candidates = new ArrayList[Candidate]
	}
	
	def this(node:JsonNode) {
		this()
		try {
			original_text = node.path("text").asText();
			candidates = new ArrayList[Candidate]
		} catch {
		  	case e:Exception => e.printStackTrace();
		}
	}
	
	//================================================================================
	// Add Candidate
	//================================================================================
	def addCandidate(candidate:Candidate) {
		if (!candidates.contains(candidate)) {
			candidates.add(candidate);
		}
	}
	
	//================================================================================
	// Candidate size
	//================================================================================
	def candidateCount(): Int = {
		return candidates.size();
	}
	
	//================================================================================
	// Remove Others
	//================================================================================
	def removeOthers(good:ArrayList[String]) {
		val newCandidates = new ArrayList[Candidate]
		
		candidates.toList map { c =>
			if (good.contains(c.grabMid())) {
				newCandidates.add(c);
				connection_found = true;
			}
		}
		
		if (newCandidates.size() > 0) {
			candidates = newCandidates;
		}
	}
	
	//================================================================================
	// Remove all but the top candidate
	//================================================================================
	def pruneForTop() {
		val newCandidates = new ArrayList[Candidate]
		
		if (candidates != null && candidates.size() > 0) {
			newCandidates.add(candidates.get(0));
			candidates = newCandidates;
		}
	}
	
	//================================================================================
	// Remove duplicates
	//================================================================================
	def removeDuplicates(usedIds:ArrayList[String]) {
		val newCandidates = new ArrayList[Candidate]
		
		candidates.toList map { candidate =>
			if (!usedIds.contains(candidate.id)) {
				newCandidates.add(candidate);
				usedIds.add(candidate.id);
			}
		}
		
		candidates = newCandidates;
	}
	
	//================================================================================
	// Determine if top candidate was mentioned in freetext
	//================================================================================
	def topCandidateValid(originalText:String): Boolean = {
		if (originalText == null) {
			return false;
		}
		
		if (candidates == null || candidates.size() == 0) {
			return false;
		}
		
		val lowerText = originalText.toLowerCase();
		val topCandidateName = candidates.get(0).name.toLowerCase();
		
		if (topCandidateName == null || topCandidateName.equalsIgnoreCase("")) {
			return false;
		}
		
		
		// Also, don't add it if there is no wiki_description
		if (candidates.get(0).wikipedia_description == null || candidates.get(0).wikipedia_description.length() == 0) {
			return false;
		}
		
		return lowerText.contains(topCandidateName);
	}
	
	//================================================================================
	// House Keeping
	//================================================================================
	override def toString(): String = {
		var s = "";

		s = original_text + "\n";
		
		candidates.toList map { c =>
			s += c.toString();
		}
		
		return s;
	}
	
  override def equals(obj:Any) = {
  obj match {
    case o: Keyword => 
    	if (o.original_text.equalsIgnoreCase(this.original_text)) true else false
    case _ => false
  }}

  override def hashCode(): Int = {
    super.hashCode()
  }
}
