package com.reactor.accio.service

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import com.reactor.nlp.utilities.IPTools
import com.typesafe.config.ConfigFactory
import akka.cluster.Cluster
import akka.kernel.Bootable
import akka.cluster.ClusterEvent.ClusterDomainEvent
import com.reactor.base.patterns.listeners.Listener
import akka.actor.actorRef2Scala
import akka.cluster.ClusterEvent.ClusterDomainEvent
import com.reactor.base.patterns.listeners.Listener
import akka.cluster.ClusterEvent.ClusterDomainEvent
import com.reactor.base.patterns.listeners.Listener
import akka.actor.ActorRef
import com.reactor.base.patterns.pull.FlowControlFactory
import akka.cluster.routing.ClusterRouterConfig
import akka.cluster.routing.AdaptiveLoadBalancingRouter
import akka.cluster.routing.ClusterRouterSettings
import com.reactor.base.patterns.pull.FlowControlConfig


class ApiBoot extends Bootable {

	val ip = IPTools.getPrivateIp();
      
	println("IP: " + ip)
	
	val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=2551") 
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [accio-frontend]\nakka.remote.netty.tcp.hostname=\""+ip+"\"")).withFallback(ConfigFactory.load("accio"))
      
    implicit val system = ActorSystem("Accio-0-1", config)
        
    //#registerOnUp
    Cluster(system) registerOnMemberUp {
	  
	      // Easy role change for debugging
          val worker_role = "accio-frontend"
          val supervisor_role = "accio-frontend"
          val default_parallelization = 1
		    
		// Actor actually handling the requests
   		val service = system.actorOf(Props(classOf[ApiActor]).withRouter(	
    	  ClusterRouterConfig(AdaptiveLoadBalancingRouter(akka.cluster.routing.MixMetricsSelector), 
    	  ClusterRouterSettings(
    	  totalInstances = 100, maxInstancesPerNode = 1,
    	  allowLocalRoutees = true, useRole = Some("accio-frontend")))),
    	  name = "serviceRouter")
    		 
       IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = 8080)
    }
  

     def startup(){
         val clusterListener = system.actorOf(Props(classOf[Listener], system), name = "clusterListener")
         Cluster(system).subscribe(clusterListener, classOf[ClusterDomainEvent])
    }

	def shutdown(){
		system.shutdown()
	}
}

object ApiApp {
   def main(args: Array[String]) = {
     val api = new ApiBoot
     api.startup
   }
}
