package org.kapunga.tm

import akka.actor.{Props, ActorSystem}
import akka.routing.{RoundRobinPool, DefaultResizer}
import org.kapunga.tm.command.{CommandExecutorService, CommandExecutor}
import org.slf4j.LoggerFactory

/**
 * The top level component to the ThorMUD system.  Is responsible for starting and stopping
 * the actor system and starting up core actors.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
class ThorMud {
  implicit val system = ActorSystem(ThorMud.ACTOR_SYSTEM_NAME)
  val log = LoggerFactory.getLogger("ThorMUD-root")

  def start() = {
    // Initialize the CommandExecutorPool
    log.info("Starting up CommandExecutorService...")
    val poolResizer = Some(DefaultResizer(lowerBound = 2, upperBound = 15))
    val pool = system.actorOf(RoundRobinPool(5, poolResizer).props(Props[CommandExecutor]), "commandRouter")
    if (CommandExecutorService.initExecutorPool(pool)) {
      log.info("CommandExecutorService started.")
    } else {
      log.error("Unable to start CommandExecutorService, shutting down.")
      system.shutdown()
    }

    // Start up the TCP server
    if (!system.isTerminated) system.actorOf(TcpServer.serverProps(12345), ThorMud.TCP_ACTOR_NAME)
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
