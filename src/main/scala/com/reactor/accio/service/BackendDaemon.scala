package com.reactor.accio.service

import akka.kernel.Bootable
import akka.cluster.Cluster
import com.reactor.nlp.utilities.IPTools
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.actor.Props
import com.reactor.base.patterns.listeners.Listener

class BackendDaemon extends Bootable {

	val ip = IPTools.getPrivateIp();
      
	println("IP: " + ip)
	
	val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=2552") 
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]\nakka.remote.netty.tcp.hostname=\""+ip+"\"")).withFallback(ConfigFactory.load("reactor"))
      
    implicit val system = ActorSystem("Accio-0-1", config)
    
	def startup(){
         val clusterListener = system.actorOf(Props(classOf[Listener], system))
         Cluster(system).subscribe(clusterListener, classOf[ClusterDomainEvent])
    }

	def shutdown(){
		system.shutdown()
	}
	
}

object BackendDaemon {
   def main(args: Array[String]) = {
     val backend = new BackendDaemon
     backend.startup
   }
}