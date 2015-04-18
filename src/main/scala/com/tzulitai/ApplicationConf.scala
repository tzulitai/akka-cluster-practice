package com.tzulitai

/**
 * Created by tzulitai on 4/17/15.
 */
object ApplicationConf {
  def getStr =
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
}
