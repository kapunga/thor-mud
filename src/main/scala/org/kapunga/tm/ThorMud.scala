package org.kapunga.tm

import akka.actor.ActorSystem

/**
 * The top level component to the ThorMUD system.  Is responsible for starting and stopping
 * the actor system and starting up core actors.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
class ThorMud {
  implicit val system = ActorSystem(ThorMud.ACTOR_SYSTEM_NAME)

  def start() = {
    // Start up the TCP server
    system.actorOf(TcpServer.serverProps(12345), ThorMud.TCP_ACTOR_NAME)
  }

  def stop() = {
    system.shutdown()
  }
}

/**
 * The companion Object to the ThorMud class.  Primarily contains global constants
 * to things like the actor system name and top level actor names.
 * @author Paul J Thordarson kapunga@gmail.com
 */
object ThorMud {
  val ACTOR_SYSTEM_NAME = "thorMudSystem"
  val TCP_ACTOR_NAME = "tcpServer"
}
