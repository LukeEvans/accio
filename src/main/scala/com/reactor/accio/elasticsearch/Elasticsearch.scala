package com.reactor.accio.elasticsearch

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

// Elasticsearch Actor
class Elasticsearch(args:FlowControlArgs) extends FlowControlActor(args) {
  
	// Ready
	ready()
	
	def receive = {
	  case _ =>
	    complete()
	}
}