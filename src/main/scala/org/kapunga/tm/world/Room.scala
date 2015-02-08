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
class Room(tmId: Int, title: String, description: String, zone: Zone) extends Persistent {
  var directions: Map[String, Room] = Map()
  var agents: Set[Agent] = Set()

  zone.registerRoom(this)

  def id = tmId

  def getTitle: String = title

  def setDirection(dir: String, room: Room) = directions = directions + (dir -> room)

  /**
   * Place a given agent into this room.
   *
   * @param agent The agent entering the room.
   */
  def enter(agent: Agent, message: String = "has arrived.") = {
    agents foreach (a => {
      a tell s"${agent.name} $message"
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
  def vacate(agent: Agent, message: String = "has left.") = {
    if (agents.contains(agent)) {
      agents = agents - agent
      agents foreach (a => {
        a tell s"${agent.name} $message"
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
    agent tell s"\n$getTitle\n\n$description\n\n$getExitsString\n"
    whoIsHere(agent)
    agent.prompt()
  }

  def move(agent: Agent, direction: String) = {
    if (directions.contains(direction) && agents.contains(agent)) {
      agents = agents - agent
      agents foreach (a => {
        a tell s"${agent.name} leaves $direction."
        a.prompt()
      })
      agent.tell(s"You leave $direction.")
      directions(direction).enter(agent)
    } else {
      agent.tell("You can't leave in that direction.")
      agent.prompt()
    }
  }

  /**
   * Tells a given player who is in the room.
   *
   * @param agent The agent to tell who is in the room.
   */
  def whoIsHere(agent: Agent) = agents.filter(a => a != agent) foreach (a => agent tell s"${a.name} is here.")

  def getExitsString: String = {
    val exits = {
      if (directions.keySet.size == 0) {
        "none"
      } else {
        directions.keySet.mkString(" ")
      }
    }

    s"Exits: [ $exits ]"
  }
}

/**
 * This is a bottom level singleton room.  It should be used sort of an a null for rooms.
 */
// TODO Give the void a database handle.
object TheVoid extends Room(-1, "The Void", "You are surrounded by an inky black nothingness.", TheNether)

case class Link(orig: Int, dest: Int, direction: String)


