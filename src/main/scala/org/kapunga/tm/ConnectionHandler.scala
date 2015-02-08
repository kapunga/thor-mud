package org.kapunga.tm

import akka.actor._
import akka.event.Logging
import akka.io.Tcp._
import akka.util.ByteString

import org.kapunga.tm.soul.Soul

/**
 * This actor handles input and output from the players socket.  It has a delegate actor that it passes off strings to
 * once they have been received.  This delegate is initially a LoginHandler, but once a player either creates a new
 * character or logs in with an existing one, the delegate is changed to a SoulHandler that deals with a player's
 * interaction with the game world.  This actor also has a reference to a monitor actor that sets up any needed telnet
 * commands and then monitors the connection keeping it alive.  It also handles any control characters received from
 * an end user's client.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 *
 * @constructor There is only one constructor, and it takes a TCP connection.
 *
 * @param connection An ActorRef to the Akka IO actor that handles the physical connection.
 */
// TODO Make LoggedIn only receivable once.
// TODO Make sure LoggedIn can only be received from a child.
// TODO Intercept up and down arrows and implement command history.
// TODO Intercept left and right arrays and implement insertion
class ConnectionHandler(connection: ActorRef) extends Actor {
  import context._
  import ConnectionHandler._

  val log = Logging(context.system, this)

  connection ! Register(self)

  watch(connection)

  var monitor = actorOf(Props[ConnectionMonitor], "monitor")
  var delegate = actorOf(Props[LoginHandler], "login")

  watch(delegate)

  var passwordMode = false
  var inputBuffer = ""

  def receive = {
    /*
     * Process a received message from the Akka IO actor this ConnectionHandler is
     * registered with.
     */
    case Received(data) =>
      // Ping the monitor, this restarts the monitors communication timeout counter.
      monitor ! Ping

      if (isIAC(data)) {
        // If we receive a command, pass it on to the monitor.
        monitor ! CommandSequence(data)
      } else if (isNewline(data)) {
        // Echo a newline, flush the input buffer to the delegate, and turn off password mode.
        connection ! Write(ByteString("\r\n"))
        delegate ! Input(inputBuffer.trim)
        inputBuffer = ""
        passwordMode = false
      } else if (isTab(data)) {
        // Request tab completion from the delegate.
        delegate ! TabComplete(inputBuffer)
      } else if (isDelete(data)) {
        // Remove a character from the input buffer and echoes deleting it.
        if (inputBuffer.length > 0) {
          inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1)
          connection ! Write(wipeChar)
        }
      } else if (isEscape(data)) {
        // Things like up and down arrows come with escape sequences.
        log.info(s"Got escape sequence: ${dataString(data)}")
      } else {
        // If we haven't processed it yet, append it to the input buffer and echo it,
        // optionally echoing a '*' instead if we are in password mode.
        inputBuffer = inputBuffer + data.utf8String
        if (passwordMode) {
          connection ! Write(ByteString('*'))
        } else {
          connection ! Write(data)
        }
      }

    /*
     * Send output to the player.
     */
    case Output(content) =>
      connection ! Write(ByteString(content.replaceAll("\n","\r\n")))

    /*
     * Sends a command to the player's client
     */
    case SendCommand(command) =>
      connection ! Write(command)

    case TabCompleteResult(output, options) =>
      inputBuffer = inputBuffer + output

      if (options.size > 0) {
        val podSize = findPod(options.fold("")((a, b) => if (a.length > b.length) a else b).size + 1, 80)
        var count = 0

        for (command: String <- options.sorted) {
          if (count == 0) self ! Output("\n")

          self ! Output(command.padTo(podSize, ' '))
          count = count + 1

          if (count == 80 / podSize) count = 0
        }

        delegate ! Prompt
      } else {
        self ! Output(output)
      }

    /*
     * Sends the player some output as a prompt, so it is not terminated by a newline.  The output
     * content is appended with the current input buffer.  This is helpful because as a player is
     * typing it is possible that output is sent and the player losses what they were typing.
     */
    case ShowPrompt(content) =>
      self ! Output(content)
      self ! Output(inputBuffer)

    /*
     * Puts the ConnectionHandler in password mode where a '*' is echoed for each input character.
     * Password mode automatically clears on the next newline from the client.
     */
    case PasswordMode =>
      passwordMode = true

    /*
     * When a LoginHandler delegate confirms a login, this message is received and the
     * ConnectionHandler switches it's delegate to a SoulHandler to handle player in-game
     * interaction.
     */
    case LoggedIn(soul) =>
      unwatch(delegate)
      delegate ! PoisonPill

      delegate = actorOf(SoulHandler.connectionProps(soul), soul.name.toLowerCase)
      watch(delegate)

    /*
     * When the delegate unexpectedly dies, we get this message.  We send a ConfirmedClose
     * message to the Akka IO handler and it will respond with a ConfirmedClose message when
     * it has done that.
     */
    case Terminated =>
      log.warning("User delegate died unexpectedly.")
      connection ! ConfirmedClose

    /*
     * This message is sent by the delegate on a player initiated quit.  We send the player
     * a goodbye message, then fire a ConfirmedClose message off to the Akka IO handler which
     * sends a ConfirmedClose message back when it's done.
     */
    case CloseConnection(message) =>
      connection ! Write(ByteString(message + "\r\n"))
      connection ! ConfirmedClose
      log.info("User quit.")

    /*
     * This message is received from the  Akka IO handler when the ConnectionHandler sends the
     * same message to it, once we have decided to close the connection.  When this message is
     * received, the ConnectionHandler stops itself.
     */
    case ConfirmedClosed =>
      log.info("Connection closed.")
      stop(self)

    /*
     * This message is received from the Akka IO Handler when the connection is unexpectedly
     * closed by the remote client.  The ConnectionHandler logs this and stops itself.
     */
    case PeerClosed =>
      log.info("Connection closed by client.")
      stop(self)
  }

  def findPod(start: Int, max: Int): Int = {
    if (max <= start) {
      start
    } else if (max % start == 0) {
      start
    } else {
      findPod(start + 1, max)
    }
  }

  // Returns true if a ByteString starts with an IAC character.
  private def isIAC(data: ByteString): Boolean = data(0) == ConnectionMonitor.IAC

  // Returns true if a ByteString starts with a tab.
  private def isTab(data: ByteString): Boolean = data(0) == '\t'.toByte

  // Returns true if a ByteString is a newline.
  private def isNewline(data: ByteString): Boolean = data == newline

  // Returns true if a ByteString starts with an escape.  Often indicates an arrow key.
  private def isEscape(data: ByteString): Boolean = data(0) == '\u001b'.toByte

  // Returns true if a ByteString starts with a delete.
  private def isDelete(data: ByteString): Boolean = data(0) == '\u007f'.toByte
}

object ConnectionHandler {
  // The character sequence sent to echo a delete and wipe the previous character.
  // Basically go back, print a space, and go back again.
  val wipeChar = ByteString('\u0008', ' ', '\u0008')

  // The ByteString both received and echoed when the remote client sends us a newline.
  val newline = ByteString('\u000d','\u0000')

  /**
   * Get props to create a new connection class.
   *
   * @param connection A reference to the Akka IO handler that is doing the raw
   *                   input and output
   * @return Props for creating a new ConnectionHandler.
   */
  def connectionProps(connection: ActorRef): Props = Props(new ConnectionHandler(connection))

  /**
   * A convenience method that renders a string of bytes to their integer strings
   * for debugging purposes.
   *
   * @param data The input ByteString
   * @return The rendered string
   */
  def dataString(data: ByteString): String = data.map(b => b.asInstanceOf[Int].toString).fold("")((a, b) => a + " " + b)
}

/**
 * This message is sent to the delegate once it finishes receiving well formed input.
 * This does not necessarily mean the user has sent a valid command, just that a string
 * of characters terminated by a newline has been sent, as we operate in character at a time mode.
 *
 * @param input The input string from the player.
 */
case class Input(input: String)

/**
 * This message is sent to the ConnectionHandler when a player needs to see some output.  The
 * string is terminated with a line break.
 *
 * @param output Output intended for the player.
 */
case class Output(output: String)

/**
 * This message is sent to the delegate if the player has entered a tab.  It requests that the delegate
 * send back a TabCompleteResult message.
 *
 * @param input Input so far that we are looking to complete.
 */
case class TabComplete(input: String)

/**
 * This message is sent in response to a TabComplete message.
 *
 * @param output The output as far as we could tab complete.
 * @param options A list of potential matches for whatever has been typed by the player thus far.
 */
case class TabCompleteResult(output: String, options: List[String])

/**
 * An empty TabCompleteResult
 */
object EmptyTabComplete extends TabCompleteResult("", Nil)

/**
 * This message is sent when some part of the system, most likely a ConnectionMonitor needs to send
 * a control sequence to the player's client.  For telnet it starts with 'IAC' ('\u00ff')
 *
 * @param command The command to send.
 */
case class SendCommand(command: ByteString)

/**
 * This message indicates a prompt should be sent.  It is almost the same as output, but there is no
 * linebreak.  Callers of this message should terminate with a space if needed.  Any part of the input
 * buffer is also appended so the player does not lose what they have been typing.
 *
 * @param prompt The prompt to send.
 */
case class ShowPrompt(prompt: String)

/**
 * This message puts the ConnectionHandler into password mode until the next linebreak is received.
 * In password mode, all input characters will be echoed with a '*'.
 */
case object PasswordMode

/**
 * This message is sent by the LoginHandler once the player has logged in.  It causes the
 * ConnectionHandler to replace it's delegate with a SoulHandler to deal with in-game interaction
 * with the player.
 *
 * @param soul The logged in soul.
 */
case class LoggedIn(soul: Soul)

/**
 * This message is sent on a player initiated quit or exit command.  It causes the ConnectionHandler to
 * close it's connection and terminate it's children.
 *
 * @param message A message to send to the user before disconnecting.
 */
case class CloseConnection(message: String)
