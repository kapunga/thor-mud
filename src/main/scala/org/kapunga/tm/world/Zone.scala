package org.kapunga.tm.world

import org.kapunga.tm.soul.Agent
import org.slf4j.LoggerFactory

/**
 * A container for rooms.  This groups related rooms allowing them to be loaded
 * and unloaded in a batch and to allow areas to be easily built by creators.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
class Zone(tmId: Int, name: String) extends Persistent {
  private val log = LoggerFactory.getLogger(s"Zone-${name.replaceAll(" ", "-")}")
  private var rooms: Map[Int, Room] = Map()
  private var loaded = false
  protected var spawnPoint: Option[SpawnPoint] = None

  log.info(s"$name initialized.")

  def id = tmId

  def getName = name

  def registerRoom(room: Room) = {
    log.info(s"Loading room ${room.id}:'${room.getTitle}'")
    rooms = rooms + (room.id -> room)
  }

  def load() = {
    if (!loaded) {
      WorldDAO.getRoomsForZone(this)
      WorldDAO.getRoomLinksForZone(this).foreach(link => {
        if (rooms.contains(link.orig) && rooms.contains(link.dest)) {
          log.info(s"Linking room id:${link.orig} to id:${link.dest} direction: '${link.direction}'")
          rooms(link.orig).setDirection(link.direction, rooms(link.dest))
        }
      })

      if (rooms.size > 0) {
        spawnPoint = Some(new SpawnPoint(rooms(rooms.keys.toList.sorted.head)))
        log.info(s"Setting spawn point for '$name' to '${spawnPoint.get.room.getTitle}'")
      }

      loaded = true
    }
  }

  def getRooms = rooms

  def warp(agent: Agent, oldRoom: Room) = {
    spawnPoint match {
      case Some(room) =>
        oldRoom.vacate(agent, "disappears in a puff of smoke.")
        agent.tell("You feel your body get tugged through the fabric of space.")
        room.room.enter(agent, "appears in a puff of smoke.")
      case None =>
        agent.tell("You cannot warp to that zone.")
        agent.prompt()
    }
  }
}

/**
 * A bottom level zone to be used instead of null for zones.
 * The only room that should be in here is TheVoid
 */
object TheNether extends Zone(-1, "The Nether") {
  this.spawnPoint = Some(new SpawnPoint(TheVoid))

  override def load(): Unit = {}
}

object NullZone extends Zone(-2, "NULL")

object NoSuchZone extends Zone(-3, "NULL")
