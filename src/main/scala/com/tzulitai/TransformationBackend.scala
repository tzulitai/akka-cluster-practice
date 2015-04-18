package com.tzulitai

import akka.actor.{Props, ActorSystem, RootActorPath, Actor}
import akka.cluster.{Member, MemberStatus, Cluster}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json

/**
 * Created by tzulitai on 4/16/15.
 */

class TransformationBackend extends Actor {

  // Backend workers will be aware of the cluster that it belongs to
  val cluster = Cluster(context.system)

  // (1) cluster.subscribe(self) lets the backend actor subscribe itself to cluster events.
  //     For example, the event akka.cluster.event.MemberUp is invoked when a new node adds to the cluster.
  //     or, on a timed interval, messages of type akka.cluster.event.CurrentClusterState are sent to backends
  //     to update the current cluster state.
  // (2) cluster.subscribe(self, classOf[MemberUp]): subscribes self to cluster events, and also invoke MemberUp
  //     upon joining.
  override def preStart() = cluster.subscribe(self, classOf[MemberUp])
  override def postStop() = cluster.unsubscribe(self)

  def receive = {
    // The forwarded job from frontends.
    case TransformationJob(text: String) => sender() ! TransformationResult(text.toUpperCase)

    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) => register(m)
  }

  def register(member: Member) =
    if (member.hasRole("frontend"))
      // look up the frontend actor on remote node
      context.actorSelection(RootActorPath(member.address) / "user" / "frontend") !
        BackendRegistration
}


object TransformationBackend {
  def main(args: Array[String]): Unit =  {
    val port = if (args.isEmpty) "0" else args(0)

    val appConfStr =
      """
        | akka {
        |   actor {
        |     provider = "akka.cluster.ClusterActorRefProvider"
        |   }
        |   remote {
        |     log-remote-lifecycle-events = off
        |     netty.tcp {
        |       hostname = "127.0.0.1"
        |       port = 0
        |     }
        |   }
        |
        |   cluster {
        |     seed-nodes = [
        |       "akka.tcp://ClusterSystem@127.0.0.1:2551",
        |     ]
        |
        |     auto-down-unreachable-after = 10s
        |   }
        | }
      """.stripMargin

    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
      .withFallback(ConfigFactory.parseString("akka.cluster.roles=[backend]"))
      .withFallback(ConfigFactory.parseString(ApplicationConf.getStr))
      .withFallback(ConfigFactory.empty())

    val system = ActorSystem("ClusterSystem", config)
    system.actorOf(Props[TransformationBackend], name="backend")
  }
}