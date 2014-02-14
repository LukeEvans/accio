package com.reactor.accio.service

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import akka.actor._
import akka.actor.ActorRef
import akka.actor.Props
import akka.util.Timeout
import play.api.libs.json._
import spray.http._
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.unmarshalling._
import spray.json.DefaultJsonProtocol._
import spray.routing._
import spray.util.LoggingContext
import com.fasterxml.jackson.core.JsonParseException
import scala.compat.Platform
import akka.pattern.AskTimeoutException
import akka.util.Timeout.durationToTimeout
import com.reactor.base.patterns.throttle.Dispatcher
import com.reactor.base.patterns.throttle.Throttler.Rate
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import spray.routing.directives.FieldDefMagnet.apply
import com.reactor.base.patterns.throttle.TimerBasedThrottler
import com.reactor.base.patterns.throttle.Throttler.SetTarget
import com.reactor.base.patterns.throttle.Throttler.Queue
import com.reactor.accio.transport.AccioRequest
import com.reactor.base.patterns.pull.FlowControlConfig
import com.reactor.base.patterns.pull.FlowControlFactory
import com.reactor.base.patterns.pull.FlowControlConfig
import com.reactor.base.patterns.pull.FlowControlArgs
import com.reactor.base.transport._
import com.reactor.base.patterns.pull.FlowControlConfig
import com.reactor.accio.pipeline.AccioArgs

class ApiActor() extends Actor with ApiService {
  def actorRefFactory = context
   	
  println("Starting API Service actor...")
  
  // Pipeline
  val accioPipeline = FlowControlFactory.flowControlledActorForContext(context, FlowControlConfig(name="accioPipeline", actorType="com.reactor.accio.pipeline.AccioPipeline"))
  
  // Set up throttler
  val dispatcher = actorRefFactory.actorOf(Props(classOf[Dispatcher], accioPipeline), "dispatcher")
  val throttler = actorRefFactory.actorOf(Props(new TimerBasedThrottler(new Rate(40, 1 seconds))))
  
  // Set the target
  throttler ! SetTarget(Some(dispatcher))
  
implicit def AccioExceptionHandler(implicit log: LoggingContext) =
  ExceptionHandler {
    case e: NoSuchElementException => ctx =>
      println("no element")
      val err = "\n--No Such Element Exception--"
      log.error("{}\n encountered while handling request:\n {}\n\n{}", err, ctx.request,e)
      ctx.complete(BadRequest, "Ensure all required fields are present.")
    
    case e: JsonParseException => ctx =>
      println("json parse")
      val err = "\n--Exception parsing input--"
      log.error("{}\nencountered while handling request:\n {}\n\n{}", err, ctx.request,e)
      ctx.complete(InternalServerError, "Ensure all required fields are present with all Illegal characters properly escaped")
      
    case e: AskTimeoutException => ctx =>
      println("Ask Timeout")
      val err = "\n--Timeout Exception--"
      log.error("{}\nencountered while handling request:\n {}\n\n{}", err, ctx.request,e)
      ctx.complete(RequestTimeout, "Server Timeout")
    
    case e: NullPointerException => ctx => 
      println("Null Pointer")
      val err = "\n--Exception parsing input--"
      log.error("{}\nencountered while handling request:\n {}\n\n{}", err, ctx.request,e)
      ctx.complete(InternalServerError, "Ensure all required fields are present with all Illegal characters properly escaped")
    
    case e: Exception => ctx => 
      e.printStackTrace()
      println("Unknown")
      val err = "\n--Unknon Exception--"
      log.error("{}\nencountered while handling request:\n {}\n\n{}", err, ctx.request,e)
      ctx.complete(InternalServerError, "Internal Server Error")
  }
    
  // Route requests to our HttpService
  def receive = runRoute(apiRoute)
  
}

trait ApiService extends HttpService {

  private implicit val timeout = Timeout(5 seconds);
  implicit val accioPipeline:ActorRef
  implicit val throttler:ActorRef

  
    // Mapper        
  val mapper = new ObjectMapper() with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
      
  val apiRoute =
        path(""){
          get{
        	  getFromResource("web/index.html")
          }
        }~        
        path("metadata"){
                get{
                  respondWithMediaType(MediaTypes.`application/json`){
                          entity(as[HttpRequest]){ obj => ctx =>
                          	val request = new AccioRequest(obj)
                            initiateRequest(request, ctx)
                          }
                  }        
                }~     
                post{              
                  respondWithMediaType(MediaTypes.`application/json`){
                          entity(as[String]){ obj => ctx =>
                          	val request = new AccioRequest(obj)
                            initiateRequest(request, ctx)
                          }
                  }        
                }
        }~
        path("health"){
                get{
                        complete{"OK."}
                }
                post{
                        complete{"OK."}
                }
        }~
        path(RestPath) { path =>
          val resourcePath = "/usr/local/accio-dist" + "/config/loader/" + path
          getFromFile(resourcePath)
        }
        
        def initiateRequest(request:AccioRequest, ctx: RequestContext) {
            val dispatchReq = DispatchRequest(RequestContainer(request), ctx, mapper)
        	throttler.tell(Queue(dispatchReq), Actor.noSender)
        }
        
}

