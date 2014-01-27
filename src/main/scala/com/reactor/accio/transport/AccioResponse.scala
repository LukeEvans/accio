package com.reactor.accio.transport

import com.fasterxml.jackson.databind.ObjectMapper
import scala.compat.Platform

class AccioResponse extends TransportMessage {
	
	var status = "OK"
	var time:String = null
	
    //================================================================================
	// Finish 
	//================================================================================
	def finishResponse(start:Long, mapper:ObjectMapper): String = {
	  val stop = Platform.currentTime
	  val duration = stop - start
	  time = duration + " ms"
	  val jsonString = mapper.writeValueAsString(this)
	  jsonString;
	}
}