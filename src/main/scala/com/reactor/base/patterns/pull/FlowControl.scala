package com.reactor.base.patterns.pull

import akka.actor.Props
import akka.actor.ActorRef
import akka.cluster.routing.ClusterRouterConfig
import akka.routing.RoundRobinRouter
import akka.cluster.routing.ClusterRouterSettings
import akka.actor.Actor
import akka.actor.ActorLogging
import com.reactor.base.patterns.pull.MasterWorkerProtocol._
import akka.actor.ActorContext
import akka.actor.ActorSystem
import com.reactor.accio.transport.TransportMessage

class FlowControlArgs() extends TransportMessage {
  var master:ActorRef = null
  var manager:ActorRef = null
  
  var flowConfig:FlowControlConfig = null
  
  def addManager(ref:ActorRef) { 
    manager = ref
  }
  
  def addMaster(ref:ActorRef) {
    master = ref
  }
  
  def addFlowConfig(conf:FlowControlConfig) {
    flowConfig = conf
  }

  def workerArgs(): FlowControlArgs = {
  	val newArgs = new FlowControlArgs()
  	newArgs.addMaster(master)
  	return newArgs
  }  
}

case class FlowControlConfig(name:String, actorType:String, parallel:Int=1, role:String="frontend") extends TransportMessage

object FlowControlFactory extends {
  def flowControlledActorForContext(context:ActorContext, flowConfig:FlowControlConfig, args:FlowControlArgs = new FlowControlArgs): ActorRef = {
	  context.actorOf(Props(classOf[FlowControlMaster], flowConfig, args))
  }
  
  def flowControlledActorForSystem(system:ActorSystem, flowConfig:FlowControlConfig, args:FlowControlArgs = new FlowControlArgs): ActorRef = {
	  system.actorOf(Props(classOf[FlowControlMaster], flowConfig, args))
  }
}

class FlowControlMaster(config:FlowControlConfig, args:FlowControlArgs) extends Master(config.name) {
   log.info("{} master starting...", config.name)
  
  args.addMaster(self)
  
  // Router to manager workers
  val splitRouter = context.actorOf(Props(classOf[FlowControlWorker], config, args).withRouter(ClusterRouterConfig(RoundRobinRouter(), 
      ClusterRouterSettings(
	  totalInstances = 1000, maxInstancesPerNode = config.parallel,
	  allowLocalRoutees = true, useRole = Some(config.role)))))
}

class FlowControlWorker(config:FlowControlConfig, masterArgs:FlowControlArgs) extends Worker(masterArgs.master) {
  implicit val ec = context.dispatcher
 
  log.info("{} Worker staring", config.name)
  
  // Get the worker version of the args
  val args = masterArgs.workerArgs()
  
  args.addManager(self)
  args.addFlowConfig(config)
  
  // Start actor that will handle the actual work
  val actor = context.actorOf(Props(Class.forName(config.actorType).asInstanceOf[Class[FlowControlActor]], args))
  
  // Handle work
  def doWork(workSender: ActorRef, msg: Any): Unit = {
      actor.tell(msg, workSender)
  }  
} 

abstract class FlowControlActor(args:FlowControlArgs) extends Actor with ActorLogging {
	
	
	// Tell manager that we're ready for work
    def ready() {
	  args.manager ! ReadyForWork
	}
	
    // Complete work
	def complete() {
	  args.manager ! WorkComplete()
	}
	
	// Return to sender from proper actor
	def reply(origin:ActorRef, msg:Any) {
	  origin.tell(msg, origin)
	}
}