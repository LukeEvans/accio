package com.reactor.accio.pipeline

import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.base.patterns.pull.FlowControlArgs
import com.reactor.accio.transport.AccioRequest
import akka.actor.ActorRef
import scala.concurrent.duration._
import com.reactor.base.transport.RequestContainer
import com.reactor.base.patterns.pull.FlowControlFactory
import com.reactor.base.patterns.pull.FlowControlConfig
import akka.actor.Props
import akka.util.Timeout
import akka.pattern.ask
import com.reactor.accio.transport.MetadataContainer
import com.reactor.accio.metadata.MetaData
import com.reactor.accio.transport.MetadataContainer
import scala.concurrent.ExecutionContext.Implicits.global
import com.reactor.accio.transport.MetadataContainer
import scala.util.Success
import scala.util.Failure
import com.reactor.base.transport._

case class AccioArgs(extractor:ActorRef) extends FlowControlArgs

class AccioPipeline(args:FlowControlArgs) extends FlowControlActor(args) {

  // Stages
  val extractor = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="extractor", actorType="com.reactor.accio.pipeline.Extractor"))
  val disambig = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="disambig", actorType="com.reactor.accio.pipeline.Disambiguator"))
  val connector = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="connector", actorType="com.reactor.accio.pipeline.Connector"))
  val describer = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="describer", actorType="com.reactor.accio.pipeline.Describer"))
  val confluence = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="confluence", actorType="com.reactor.accio.pipeline.gather.Gatherer"))
  
  // Ready
  ready()
  
  def receive = {
    case RequestContainer(request:AccioRequest) =>
      val origin = sender
      process(request, origin)
      complete()
      
    case s:String => 
      println("Got: " + s)
      complete()
  }
  
  // Process
  def process(req:AccioRequest, origin:ActorRef) {
	  implicit val timeout = Timeout(10 seconds)
	  
	  // Initial metadata
	  val initial = MetadataContainer(new MetaData(req.text))
	  
	  val completed = for {
	    extracted <- (extractor ? initial).mapTo[MetadataContainer]
	    disambiguated <- (disambig ? extracted).mapTo[MetadataContainer]
	    connected <- (connector ? disambiguated).mapTo[MetadataContainer]
	    described <- (describer ? connected).mapTo[MetadataContainer]
	    confluenced <- (confluence ? described).mapTo[MetadataContainer]
	  } yield confluenced
	  
	  completed onComplete {
	  	case Success(metadataResponse) =>
	  		origin ! ResponseContainer(new RESTResponse(metadataResponse.metadata))
	  	case Failure(e) => 
	  	  log.error("An error has occurred: " + e.getMessage())
	  }
  }
  
  
}