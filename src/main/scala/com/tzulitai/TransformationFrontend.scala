package com.tzulitai

import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import akka.pattern.ask
import play.api.libs.json

import scala.util.parsing.json.JSON

/**
 * Created by tzulitai on 4/17/15.
 */

class TransformationFrontend extends Actor {

  var backends = IndexedSeq.empty[ActorRef]
  var jobCounter = 0

  def receive = {
    case job: TransformationJob if backends.isEmpty =>
      sender() ! JobFailed("Service unavailable, try again later", job)

    case job: TransformationJob =>
      jobCounter += 1
      backends(jobCounter%backends.size) forward job

    // sender() is the ActorRef to the caller of the current message
    case BackendRegistration if !backends.contains(sender()) =>
      context watch sender // the frontend now monitors the backend (the sender of BackendRegistration)
      backends = backends :+ sender()

    case Terminated(a) => // backends will send this to frontends when they die because they are monitored
      backends.filterNot(_ == a)
  }
}

object TransformationFrontend {
  def main (args: Array[String]): Unit = {
    val port = if (args.length==0) "0" else args(0)

    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
      .withFallback(ConfigFactory.parseString("akka.cluster.roles=[frontend]"))
      .withFallback(ConfigFactory.parseString(ApplicationConf.getStr))
      .withFallback(ConfigFactory.empty())

    val system = ActorSystem("ClusterSystem", config)
    val frontend = system.actorOf(Props[TransformationFrontend], name = "frontend")

    val counter = new AtomicInteger
    import system.dispatcher
    system.scheduler.schedule(2.seconds,2.seconds) {
      implicit val timeout = Timeout(5.seconds)
      (frontend ? TransformationJob("hello-" + counter.incrementAndGet())) onSuccess {
        case result => println(result)
      }
    }
  }
}
