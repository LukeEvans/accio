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

// Elasticsearch Master
class ElasticsearchMaster(parallel:Int, role:String) extends Master("accio-elasticsearh-master") {
	log.info("Elasticsearch master starting...")
	
	// elasticsearch router
	val elasticsearchRouter = context.actorOf(Props(classOf[ElasticsearchWorker], self).withRouter(ClusterRouterConfig(RoundRobinRouter(), 
      ClusterRouterSettings(
	  totalInstances = 1000, maxInstancesPerNode = parallel,
	  allowLocalRoutees = true, useRole = Some(role)))),
	  name = "elasticsearchRouter")
}

// Elasticsearch Worker
class ElasticsearchWorker(master: ActorRef) extends Worker(master) {
 
  log.info("Elasticsearch Worker staring")
  
  implicit val ec = context.dispatcher
  val elasticsearch = context.actorOf(Props(classOf[ElasticsearchActor], self))
  
  // Handle work
  def doWork(workSender: ActorRef, msg: Any): Unit = {
      elasticsearch.tell(msg, workSender)
  } 
}

// Elasticsearchor actor
class ElasticsearchActor extends Actor with ActorLogging {
	def receive = {
	  case _ =>
	}
}