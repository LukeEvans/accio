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

// GraphDB Master
class DisambiguateMaster(parallel:Int, role:String) extends Master("accio-graphdb-master") {
	log.info("GraphDB master starting...")
	
	// graphdb router
	val graphDBRouter = context.actorOf(Props(classOf[GraphDBWorker], self).withRouter(ClusterRouterConfig(RoundRobinRouter(), 
      ClusterRouterSettings(
	  totalInstances = 1000, maxInstancesPerNode = parallel,
	  allowLocalRoutees = true, useRole = Some(role)))),
	  name = "graphDBRouter")
}

// GraphDB Worker
class GraphDBWorker(master: ActorRef) extends Worker(master) {
 
  log.info("GraphDB Worker staring")
  
  implicit val ec = context.dispatcher
  val graphdb = context.actorOf(Props(classOf[GraphDBActor], self))
  
  // Handle work
  def doWork(workSender: ActorRef, msg: Any): Unit = {
      graphdb.tell(msg, workSender)
  } 
}

// GraphDB actor
class GraphDBActor extends Actor with ActorLogging {
	def receive = {
	  case _ =>
	}
}