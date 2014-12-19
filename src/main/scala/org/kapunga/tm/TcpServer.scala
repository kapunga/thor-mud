package org.kapunga.tm

import akka.actor.{Actor, Props}
import akka.event.Logging
import akka.io.{ IO, Tcp }
import akka.io.Tcp.{Connected, CommandFailed, Bind, Bound}
import java.net.InetSocketAddress

/**
 * A simple TCP handler to listen on a port and create incoming connections.  It binds to a
 * particular port with the Akka IO actor and creates ConnectionHandler actors.  It keeps a
 * running tally of the connections it has handled and each ConnectionHandler is named with
 * the sequence number of the connection since the last startup.
 *
 * @param port The port we want to bind the MUD to.
 *
 * @author Paul J Thordarson
 */
// TODO Figure out how to do secure connections over telnet.
// TODO Add limitations to the number of simultaneous connections.
// TODO Add a whitelist or blacklist to external ips.
// TODO Move log messages to a message bundle or something.
// TODO If this receives a CommandFailed message, we need to shut down the whole system.
class TcpServer(port: Int) extends Actor {
  import TcpServer._
  import context.system

  val log = Logging(system, this)
  var connectionNum: Int = 1

  log.info(STARTING_SERVER)

  IO(Tcp) ! Bind(self, new InetSocketAddress("0.0.0.0", port))
             
  def receive = {
    /*
     * This message is received when the Akka IO actor successfully binds to a port.  No action
     * needs to be taken, it is merely logged.
     */
    case b @ Bound(localAddress) =>
      log.info(PORT_BOUND, localAddress)

    /*
     * This message is received when the Akka IO actor receives an new connection.  A new
     * ConnectionHandler is created that binds registers itself to perform IO to this connection.
     */
    case c @ Connected(remote, local) =>
      log.info(CONNECTION_ACCEPTED)
      context.actorOf(ConnectionHandler.connectionProps(sender()), s"conn$connectionNum")
      connectionNum += 1

    /*
     * This message is received when the Akka IO actor fails to bind to the correct port.
     * The TcpServer stops itself.
     */
    case CommandFailed(_: Bind) =>
      log.error(BINDING_FAILED)
      context stop self
  }
}

/**
 * A companion Object to TcpServer.  It has a method for obtaining props for a new TcpServer
 * and a number of static Strings for logging.
 */
object TcpServer {
  val STARTING_SERVER = "Starting up TCP Server..."
  val PORT_BOUND = "TCP Server bound {}"
  val BINDING_FAILED = "TCP Server port binding failed, stopping server."
  val CONNECTION_ACCEPTED = "Incoming connection accepted, starting up Connection Handler..."

  def serverProps(port: Int): Props = Props(new TcpServer(port))
}
