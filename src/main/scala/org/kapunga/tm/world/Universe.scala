package org.kapunga.tm.world

import org.slf4j.LoggerFactory

/**
 * @author Paul J Thordarson kapunga@gmail.com
 */
// TODO: Make zone loading smarter.
// TODO: Make indexes so we don't have to iterate.
object Universe {
  private val log = LoggerFactory.getLogger("Universe")
  private var pantheons: Map[Int, Pantheon] = Map()
  private var zones: Map[Int, Zone] = Map(TheNether.id -> TheNether)
  
  def init() = {
    log.info("Loading pantheons.")
    WorldDAO.findAllPantheons().foreach(p => {
      pantheons = pantheons + (p.id -> p)
    })

    log.info("Loading zones.")
    WorldDAO.findAllZones().filter(z => z.id != -1).foreach(z => zones = zones + (z.id -> z))

    log.info("Linking pantheons.")
    WorldDAO.findAllPantheonMembers().foreach(combo => {
      if (pantheons.contains(combo._1) && zones.contains(combo._2)) pantheons(combo._1).setZone(zones(combo._2))
    })

    log.info("Loading extra zones.")
    zones.values.foreach(z => if (WorldDAO.hasRooms(z)) z.load())

    log.info("Setting up spawn points.")
    if (pantheons.contains(0) && pantheons(0).zone.getRooms.contains(0)) {
      SpawnPoint.globalSpawn = new SpawnPoint(pantheons(0).zone.getRooms(0))
    }
  }

  def allPantheons: Iterable[Pantheon] = pantheons.values

  def allZones: Iterable[Zone] = zones.values

  def getZone(id: Int): Zone = if (zones.contains(id)) zones(id) else TheNether
}
