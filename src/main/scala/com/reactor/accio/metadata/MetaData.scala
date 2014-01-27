package com.reactor.accio.metadata

import com.reactor.accio.metadata.connections.ConnectionMatrix
import java.util.ArrayList
import com.reactor.accio.metadata.confluence.Confluence
import scala.collection.JavaConversions._

class MetaData {
	var free_text:String = null
	var keywords:ArrayList[Keyword] = new ArrayList[Keyword]
	var connection_matrix:ConnectionMatrix = null
	var confluence:Confluence = null
	
	//================================================================================
	// Constructors
	//================================================================================
	def this(fullText:String) {
		this()
		free_text = fullText;
		keywords = new ArrayList[Keyword];
		connection_matrix = new ConnectionMatrix();
		confluence = new Confluence();
	}

	//================================================================================
	// Add Keyword
	//================================================================================
	def addKeyword(keyword:Keyword) {
		if (!keywords.contains(keyword)) {
			keywords.add(keyword);
		}
	}

	//================================================================================
	// Init Matrix
	//================================================================================
	def initConnectionMatrix() {

		// Init matrix
		connection_matrix.init(keywords);
	}

	//================================================================================
	// Prune MetaData
	//================================================================================
	def prune() {
		val connectedIds = connection_matrix.findConnectedIds();

		// Remove Candidates that have been proven wrong
		keywords.toList map { keyword =>
		  keyword.removeOthers(connectedIds);
		}

		// If we have any connections, remove all non-connected ones
		if (connectionsExist()) {
			val newKeywords = new ArrayList[Keyword];

			keywords.toList map { keyword =>
				if (keyword.connection_found) {
					newKeywords.add(keyword);
				}
			}

			// Set new keywords
			keywords = newKeywords;
		}

		// Remove all keywords that were added additionally
		else {
			val newKeywords = new ArrayList[Keyword];
			
			keywords.toList map { keyword =>
				if (!keyword.additionalKeyword) {
					newKeywords.add(keyword);
				}
				
				else if (keyword.additionalKeyword && keyword.connection_found) {
					newKeywords.add(keyword);
				}
			}
		}
	}

	//================================================================================
	// Prune for top candidates
	//================================================================================
	def pruneForTopCandidates() {
		keywords.toList map { keyword =>
			keyword.pruneForTop();
		}
	}
	
	//================================================================================
	// Shallow process prune. Remove candidates that are not mentioned in freetext
	//================================================================================
	def removeInvalidCandidates() {
		val newKeywords = new ArrayList[Keyword];
		
		keywords.toList map { keyword =>
			if (keyword.topCandidateValid(free_text)) {
				newKeywords.add(keyword);
			}
		}
		
		if (newKeywords.size() > 0) {
			keywords = newKeywords;
		}
	}
	
	//================================================================================
	// Remove duplicates
	//================================================================================
	def removeDuplicates() {
		val usedIds = new ArrayList[String];
		val newKeywords = new ArrayList[Keyword];
		
		keywords.toList map { keyword =>
			keyword.removeDuplicates(usedIds);
			
			if (keyword.candidateCount() > 0) {
				newKeywords.add(keyword);
			}			
		}
		
		keywords = newKeywords;
	}
	
	//================================================================================
	// Determine if > 0 connections have been found
	//================================================================================
	def connectionsExist(): Boolean = {
	  
	  keywords.toList map { keyword =>
	  	if (keyword.connection_found) {
			return true;
		} 
	  }

		false;
	}
	
	//================================================================================
	// Get list of topic ids
	//================================================================================
	def topicIDList(): ArrayList[String] = {
		var topicIds = new ArrayList[String];
		
		keywords.toList map { keyword =>
		  keyword.candidates.toList map { candidate =>
				if (!topicIds.contains(candidate.grabMid())) {
					topicIds.add(candidate.grabMid());
				}
			}
		}
		
		return topicIds;
	}
	
	//================================================================================
	// Get related ids
	//================================================================================
	def relatedIDList(): ArrayList[String] = {
		return connection_matrix.findRelatedIds();
	}
}