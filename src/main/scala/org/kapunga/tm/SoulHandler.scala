package org.kapunga.tm

import akka.actor.{PoisonPill, Props, Actor}
import akka.event.Logging
import org.kapunga.tm.command.CommandExecutorService
import org.kapunga.tm.soul.{Spirit, SpiritAgent, Agent, Soul}

/**
 * This actor is responsible for handling sanitized input from a player once that player
 * has been logged in, and giving in game notification.
 *
 * @param soul The soul of the logged in player.
 */
// TODO Support dynamic prompts definable by the player.
// TODO Support quit messages so a user can be notified of why they may have been forced to quit.
class SoulHandler(soul: Soul) extends Actor {
  import context.parent

  val log = Logging(context.system, this)
  val agent: Agent = new SpiritAgent(soul, Spirit.getSpirit(soul), self)
  agent.spawn()

  def receive = {
    /*
     * This message is received once the ConnectionHandler has cleaned up some input.
     * It is logged and then passed off to the CommandExecutorService.
     */
    case Input(content) =>
      log.info(s"Received input: '$content'")
      CommandExecutorService.command(content, agent.context)
    /*
     * This message is received when a quit action is received either as a result of the
     * player's input or something like a reboot or a Kick
     */
    case Quit =>
      parent ! CloseConnection("Bye Bye!\n")

    /*
     * This message is received when a player inputs a tab.  It should dispatch the tab to
     * the CommandExecuterService for completion.  Currently unimplemented.
     */
    case TabComplete(partialCommand) =>
      CommandExecutorService.tabComplete(partialCommand, agent.context) match {
        case EmptyTabComplete =>
          parent ! Output("\n")
          prompt()
        case TabCompleteResult(output, options) =>
          parent ! TabCompleteResult(output, options)
      }

    /*
     * This message is received when an in-game event happens and the player needs to be notified.
     * This can generally come from just about anywhere.
     */
    case Notify(item) =>
      parent ! Output(item)
    /*
     * This message is received when a player needs to receive a prompt.  It should generally be
     * sent after a single or related bunch of Notify messages has been sent.
     */
    case Prompt =>
      prompt()
  }

  override def postStop(): Unit = {
    agent.deSpawn()
    super.postStop()
  }

  /**
   * Issues a prompt to the player with a default of the soul name.  The ConnectionHandler
   * will append the current command buffer to the output.
   *
   * @param content What we wish the prompt to be.  Defaults to the soul name.
   */
  def prompt(content: String = soul.name + " > ") = parent ! ShowPrompt("\n" + content)
}

/**
 * A companion Object to the soul handler.  Provides Props for constructing a SoulHandler actor.
 */
object SoulHandler {
  def connectionProps(soul: Soul): Props = Props(new SoulHandler(soul))
}

/**
 * Sent by in game processes when a line of text needs to be sent to the player.
 *
 * @param item The string to be displayed to the player.
 */
case class Notify(item: String)

/**
 * This message is sent by in-game processes when a chunk of output has finished being sent and the player
 * needs to be given a prompt.
 */
case object Prompt

/**
 * This message is sent by the CommandExecutorService when the player has entered a command to quit the game.
 */
case object Quit
