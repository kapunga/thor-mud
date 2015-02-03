package org.kapunga.tm.soul

import akka.actor.ActorRef
import org.kapunga.tm.{Quit, Prompt, Notify}
import org.kapunga.tm.command.CommandContext
import org.kapunga.tm.world.Room

/**
 * An agent represents an in game handler for a player.  It has
 * contains references to both the Soul of the character and the
 * actor that is handling input and output from that player.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
// TODO Properly implement this class, it's a bit of a hack at the moment.
// TODO Make tells and prompt atomic.
abstract class Agent(soul: Soul, agentActor: ActorRef) {
  var location: Room = null

  /**
   * @return The Soul's name for this agent.
   */
  def name = soul.name

  /**
   * Send a notify message.  This will be passed on to the agent's actor and
   * intern the player playing the agents connection.
   *
   * @param sentence The item we want to tell the agent.
   */
  def tell(sentence: String) = agentActor ! Notify("\n" + sentence)

  /**
   * Notify the agent we would like them to send a prompt.  This should be done after
   * sending a batch of tells.
   */
  def prompt() = agentActor ! Prompt

  /**
   * Force the agent to quit.  Can be as a result of agent input, or as a result
   * of a command.
   */
  def quit() = agentActor ! Quit

  /**
   * Set's an agent's location.
   * @param loc The room where the agent is currently located.
   */
  def setLocation(loc: Room) = location = loc

  /**
   * Spawn an agent to it's spawn point.  This only executes if the agent is
   * not already present in a room.
   */
  def spawn() = {
    if (location == null) {
      AgentManager.add(this)
      soul.spawnPoint.spawn(this)
    }
  }

  /**
   * Remove the agent from the game world, generally on a logout.
   */
  def deSpawn() = {
    location.vacate(this)
    location = null
    AgentManager.remove(this)
  }

  /**
   * Get a command context for this agent.
   *
   * @return A command context for the agent and it's location.
   */
  def context = new CommandContext(this, location)
}

/**
 * An agent for a player who is currently playing as a spirit.
 *
 * @param soul The soul of the player this agent
 * @param agentActor The actor handling IO to the player
 */
case class SpiritAgent(soul: Soul, agentActor: ActorRef) extends Agent(soul, agentActor)
