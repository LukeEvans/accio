package com.reactor.accio.pipeline.gather

import com.reactor.base.patterns.pull.FlowControlArgs
import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.accio.transport.IdList
import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer
import com.reactor.accio.storage.Mongo
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Failure
import java.util.HashMap
import com.reactor.accio.transport.ConfluenceNodeList
import com.reactor.accio.storage.Elasticsearch
import com.reactor.base.utilities.Tools
import com.fasterxml.jackson.databind.JsonNode
import java.util.Date
import scala.collection.JavaConversions._
import java.text.SimpleDateFormat
import com.reactor.accio.transport.ConfluenceNodeList
import com.reactor.accio.transport.StringList
import java.math.BigDecimal
import com.reactor.accio.transport.TransportMessage
import com.reactor.accio.transport.ConfluenceNodeList

object YahooFinance {
	val symbolFetchBaseURL = "http://d.yimg.com/autoc.finance.yahoo.com/autoc?callback=YAHOO.Finance.SymbolSuggest.ssCallback&query="
	val stockFetchBaseURL = """http://query.yahooapis.com/v1/public/yql?format=json&env=store://datatables.org/alltableswithkeys&q=select * from yahoo.finance.quote where symbol in """
		
	// Get list of symbols
	def getSymbols(companies:ArrayBuffer[String]): ArrayBuffer[String] = {
		val symbols = ArrayBuffer[String]()
		
		companies map { c =>
			Tools.fetchYahooURL(symbolFetchBaseURL + c) match {
				case Some ( symbolNode ) =>
					symbols += symbolNode.get("ResultSet").get("Result").get(0).get("symbol").asText()
				case None => 
			}
		}
		
		return symbols
	}
	
	// Build YQL query
	def buildURL(symbols:ArrayBuffer[String]): String = {
		var url = new String(stockFetchBaseURL + " (")
		
		symbols map { s =>
			url += "\"" + s + "\","
		}
		
		return url.substring(0, url.length() - 1) + ")"
	}		
	
	// Fetch stock objects
	def fetchStockFromURL(url:String): ArrayBuffer[Stock] = {
		val stockList = ArrayBuffer[Stock]()
		
		try {
			
			Tools.fetchURL(url) match {
				case Some ( response ) =>
					
					val quoteNode = response.get("query").get("results").get("quote")
					if (quoteNode.isArray()) {
						quoteNode.toList map { quoteNode =>
							stockList += new Stock(quoteNode)
						}
					}
					
					else {
						stockList += new Stock(quoteNode) 
					}
					
				case None =>
			}
		} 	
		
		return stockList
	}
	
	// Determine if symbol is valid
	def validTickerSymbol(symbol:String): Option[String] = {
		
		if (symbol == null || symbol.length() <= 1) return None
		
		try {
			val buff = ArrayBuffer[String]()
			buff += symbol
			
			val url = buildURL(buff)
			val stock = fetchStockFromURL(url).get(0)
			
			if (stock.stock_exchange != null) {
				return Some (stock.company)
			}
			
			return None
			
		} catch {
			case e:Exception => return None
		}
		
	}
	
	// Determine if string could be a ticker
	def possibleTicker(ticker:String): Boolean = {
		ticker matches "[A-Z]{3,5}"
	}
}

class FinanceGatherer(args: FlowControlArgs) extends FlowControlActor(args) {

	// Ready
	ready()

	def receive = {
		case StringList(companies) =>
			val origin = sender
			process(origin, companies)
	}	

	// Process
	def process(origin:ActorRef, companies:ArrayBuffer[String]) {
		val confluenceNodes = ArrayBuffer[Any]()
		
		// Get url
		val url = YahooFinance.buildURL(YahooFinance.getSymbols(companies))
		YahooFinance.fetchStockFromURL(url) map { stock =>
			confluenceNodes += stock
		}
		
		// Reply
		reply(origin, ConfluenceNodeList(confluenceNodes) )
	}
}


// Stock
class Stock(stockNode:JsonNode) extends TransportMessage {
	
	var id:String = null
	var story_type = "stocks"
	var icon = "https://s3.amazonaws.com/Twitter_Source_Images/Winston-Twitter-Images/Stocks_icon.png"
	var source = "Yahoo Finance"
	var company:String = null
	var symbol:String = null
	var value:Double = 0
	var change:Double = 0
	var percent_change:String = null
	var stock_exchange:String = null
	
	if (stockNode != null) {
		
		company = stockNode.get("Name").asText()
		id = Tools.generateHash(company)
		symbol = stockNode.path("symbol").asText()
		stock_exchange = stockNode.path("StockExchange").asText()
	
		if (stockNode.has("LastTradePriceOnly") && stockNode.has("Change")) {
			updateValues(stockNode.path("LastTradePriceOnly").asDouble(), stockNode.path("Change").asDouble())
		}
		
		// Update values
		def updateValues(v:Double, c:Double) {
			value = v;
			change = c;
			percent_change = calculatePercent(v, c);
		}	
		
		// Calculate Percentage
		def calculatePercent(v:Double, c:Double): String = {
			if(v == 0)
				return "0.0%"
	
			val changePercent:Double = (c/v) * 100
			var dec = new BigDecimal(changePercent);
			dec = dec.setScale(2, BigDecimal.ROUND_HALF_UP);
	
			return dec.toString() + "%";
		}	
	
	}
}