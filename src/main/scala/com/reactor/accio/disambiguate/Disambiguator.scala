package com.reactor.accio.disambiguate

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

// Disambiguate Master
class DisambiguateMaster(parallel:Int, role:String) extends Master("accio-disambiguateor-master") {
	log.info("Disambiguate master starting...")
	
	// disambiguator router
	val disambiguateorRouter = context.actorOf(Props(classOf[DisambiguateWorker], self).withRouter(ClusterRouterConfig(RoundRobinRouter(), 
      ClusterRouterSettings(
	  totalInstances = 1000, maxInstancesPerNode = parallel,
	  allowLocalRoutees = true, useRole = Some(role)))),
	  name = "disambiguateorRouter")
}

// Disambiguate Worker
class DisambiguateWorker(master: ActorRef) extends Worker(master) {
 
  log.info("Disambiguate Worker staring")
  
  implicit val ec = context.dispatcher
  val disambiguateor = context.actorOf(Props(classOf[DisambiguateActor], self))
  
  // Handle work
  def doWork(workSender: ActorRef, msg: Any): Unit = {
      disambiguateor.tell(msg, workSender)
  } 
}

// Disambiguateor actor
class DisambiguateActor extends Actor with ActorLogging {
	def receive = {
	  case _ =>
	}
}