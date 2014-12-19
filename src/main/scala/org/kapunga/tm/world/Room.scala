package org.kapunga.tm.world

import org.kapunga.tm.soul.Agent

/**
 * The basic unit of space for all of ThorMUD.  Every entity in the game must be in a room
 * with the exception of items that are contained by another entity, such as an weapon on a
 * player.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 *
 * @constructor Creates the room and adds it to a zone.
 *
 * @param title The title of this room.
 * @param description The description of this room.
 * @param zone The zone to which this room is a member.
 */
class Room(title: String, description: String, zone: Zone) {
  var agents: Set[Agent] = Set()

  zone.registerRoom(this)

  /**
   * Place a given agent into this room.
   *
   * @param agent The agent entering the room.
   */
  def enter(agent: Agent) = {
    agents foreach (a => {
      a tell s"${agent.name} has arrived."
      a.prompt()
    })

    agents = agents + agent
    agent.setLocation(this)
    look(agent)
  }

  /**
   * Remove a given agent from this room.
   *
   * @param agent The agent leaving.
   */
  def vacate(agent: Agent) = {
    if (agents.contains(agent)) {
      agents = agents - agent
      agents foreach (a => {
        a tell s"${agent.name} has left."
        a.prompt()
      })
    }
  }

  /**
   * Tells a given agent the result of looking around the room.
   *
   * @param agent The agent to tell.
   */
  def look(agent: Agent) = {
    agent tell s"\n$title\n\n$description\n"
    whoIsHere(agent)
    agent.prompt()
  }

  /**
   * Tells a given player who is in the room.
   *
   * @param agent The agent to tell who is in the room.
   */
  def whoIsHere(agent: Agent) = agents.filter(a => a != agent) foreach (a => agent tell s"${a.name} is here.")
}

/**
 * This is a bottom level singleton room.  It should be used sort of an a null for rooms.
 */
// TODO Give the void a database handle.
object TheVoid extends Room("The Void", "You are surrounded by an inky black nothingness.", TheNether)


