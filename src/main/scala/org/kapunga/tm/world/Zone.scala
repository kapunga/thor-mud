package org.kapunga.tm.world

/**
 * A container for rooms.  This groups related rooms allowing them to be loaded
 * and unloaded in a batch and to allow areas to be easily built by creators.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
class Zone {
  private var rooms: Set[Room] = Set()

  def registerRoom(room: Room) = rooms = rooms + room
}

/**
 * A bottom level zone to be used instead of null for zones.
 * The only room that should be in here is TheVoid
 */
object TheNether extends Zone
