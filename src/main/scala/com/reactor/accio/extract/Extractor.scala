package com.reactor.accio.extract

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

// Extract Master
class ExtractMaster(parallel:Int, role:String) extends Master("accio-extractor-master") {
	log.info("Extract master starting...")
	
	// extractor router
	val extractorRouter = context.actorOf(Props(classOf[ExtractWorker], self).withRouter(ClusterRouterConfig(RoundRobinRouter(), 
      ClusterRouterSettings(
	  totalInstances = 1000, maxInstancesPerNode = parallel,
	  allowLocalRoutees = true, useRole = Some(role)))),
	  name = "extractorRouter")
}

// Extract Worker
class ExtractWorker(master: ActorRef) extends Worker(master) {
 
  log.info("Extract Worker staring")
  
  implicit val ec = context.dispatcher
  val extractor = context.actorOf(Props(classOf[ExtractActor], self))
  
  // Handle work
  def doWork(workSender: ActorRef, msg: Any): Unit = {
      extractor.tell(msg, workSender)
  } 
}

// Extractor actor
class ExtractActor extends Actor with ActorLogging {
	def receive = {
	  case _ =>
	}
}