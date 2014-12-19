package org.kapunga.tm

import akka.actor.{Cancellable, Actor}
import akka.event.Logging
import akka.io.Tcp.PeerClosed
import akka.util.ByteString

import scala.concurrent.duration._

/**
 * This actor is responsible for monitoring the status of it's parent.  It handles all telnet
 * control sequences and is responsible for ensuring that the player's remote client is in the
 * correct state.  Additionally if the player's client goes a prescribed length of time without
 * sending any input, the monitor will send a ping.  If no response is received within the timeout
 * period, the ConnectionMonitor informs it's parent it needs to close and cleanup.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
// TODO Create expected responses to commands to confirm that the remote client is in a correct state.
class ConnectionMonitor extends Actor {
  import ConnectionMonitor._
  import context._

  val log = Logging(context.system, this)

  private var commandTimer: Cancellable = null
  private var responseTimer: Cancellable = null

  sendCommand(setupCommand)
  
  def receive = {
    /*
     * This message is sent by the parent every time it receives some input from the players client.
     * On receipt, the connection monitor cancels all timers and restarts the command timer.
     */
    case Ping =>
      if (commandTimer != null) commandTimer.cancel()
      if (responseTimer != null) responseTimer.cancel()
      
      startCommandTimer()

    /*
     * This message is sent by a timer started by the ConnectionMonitor itself.  Upon receipt it
     * has it's parent send an "Are you there?" message to the player's remote client.  It also starts
     * the response timer and
     */
    case Check =>
      sendCommand(areYouThere)
      startResponseTimer()

    /*
     * This message is sent by the parent when a command sequence is received by a player's telnet client.
     * Currently the only thing done upon receipt is to log the command.
     */
    case Command(command) =>
      log.info(s"Received control sequence: ${stringRepr(command)}")
  }

  def preStop() = {
    commandTimer.cancel()
    responseTimer.cancel()
  }
  
  private def startCommandTimer() = commandTimer = system.scheduler.scheduleOnce(COMMAND_TIMEOUT, self, Check)
  
  private def startResponseTimer() = responseTimer = system.scheduler.scheduleOnce(RESPONSE_TIMEOUT, self, PeerClosed)

  private def sendCommand(command: ByteString) = {
    log.info(s"Sending control sequence: ${stringRepr(command)}")
    parent ! SendCommand(command)
  }
}

object ConnectionMonitor {
  val COMMAND_TIMEOUT = 45.seconds
  val RESPONSE_TIMEOUT = 5.seconds

  // Telnet command characters.  For more information:
  // http://www.cs.cf.ac.uk/Dave/Internet/node141.html
  val echo = '\u0001'.toByte
  val supGoAhead = '\u0003'.toByte
  val status = '\u0005'.toByte
  val timingMark = '\u0006'.toByte
  val termType = '\u0018'.toByte
  val windowSize = '\u001f'.toByte
  val termSpeed = '\u0020'.toByte
  val remFlowCon = '\u0021'.toByte
  val lineMode = '\u0022'.toByte
  val envVars = '\u0024'.toByte

  val SE = '\u00f0'.toByte
  val NOP = '\u00f1'.toByte
  val DM = '\u00f2'.toByte
  val BRK = '\u00f3'.toByte
  val IP = '\u00f4'.toByte
  val AO = '\u00f5'.toByte
  val AYT = '\u00f6'.toByte
  val EC = '\u00f7'.toByte
  val EL = '\u00f8'.toByte
  val GA = '\u00f9'.toByte
  val SB = '\u00fa'.toByte
  val WILL = '\u00fb'.toByte
  val WONT = '\u00fc'.toByte
  val DO = '\u00fd'.toByte
  val DONT = '\u00fe'.toByte
  val IAC = '\u00ff'.toByte

  val setupCommand = ByteString(IAC,WILL,echo,IAC,WILL,supGoAhead,IAC,WONT,lineMode)
  val areYouThere = ByteString(IAC, DO, AYT)

  /**
   * Gives a string representation of a byte as specified in the telnet specification.
   * Most results are non-printable ascii characters sent as commands to control telnet
   * communication characteristics.
   *
   * @param byte The byte we want a representation for.
   * @return The String representation of the specified byte.
   */
  def stringRepr(byte: Byte): String = {
    byte match {
      case `echo` => "ECHO"
      case `supGoAhead` => "SUPPRESS_GO_AHEAD"
      case `status` => "STATUS"
      case `timingMark` => "TIMING_MARK"
      case `termType` => "TERMINAL_TYPE"
      case `windowSize` => "WINDOW_SIZE"
      case `termSpeed` => "TERMINAL_SPEED"
      case `remFlowCon` => "REMOTE_FLOW_CONTROL"
      case `lineMode` => "LINE_MODE"
      case `envVars` => "ENVIRONMENT_VARIABLES"

      case SE => "SE"
      case NOP => "NOP"
      case DM => "DM"
      case BRK => "BRK"
      case IP => "IP"
      case AO => "AO"
      case AYT => "AYT"
      case EC => "EC"
      case EL => "EL"
      case GA => "GA"
      case SB => "SB"
      case WILL => "WILL"
      case WONT => "WONT"
      case DO => "DO"
      case DONT => "DONT"
      case IAC => "IAC"
      case _ => byte.toString
    }
  }

  /**
   * Gives the string representation of a byte string as specified in the telnet specification.
   *
   * @param byteString The ByteString to convert.
   * @return A string representation of the specified ByteString.
   */
  def stringRepr(byteString: ByteString): String = byteString.toList.map(stringRepr).fold("")((a, b) => a + " " + b)
}

/**
 * Sent by a ConnectionHandler whenever any input it received.
 */
case object Ping

/**
 * Sent by a timer to signal the ConnectionMonitor to send an
 * "Are you there" message to a player's client.
 */
case object Check

/**
 * Sent by a ConnectionHandler when a command sequence is received.
 * @param command The command sequence received.
 */
case class Command(command: ByteString)
