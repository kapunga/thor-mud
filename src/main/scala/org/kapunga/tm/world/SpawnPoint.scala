package org.kapunga.tm.world

import org.kapunga.tm.soul.Agent

/**
 * This class represents a room where an agent can spawn.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 *
 * @param spawnPoint The room where we are spawning.
 */
class SpawnPoint(spawnPoint: Room) {
  def spawn(spirit: Agent) = spawnPoint.enter(spirit)
}

/**
 * A companion object to SpawnPoints.  Right now just
 * contains a global spawn, this object will probably
 * change more to a DAO as spawn points get more dynamic.
 */
object SpawnPoint {
  def globalSpawn = new SpawnPoint(TheVoid)
}
