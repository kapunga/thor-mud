package org.kapunga.tm.world

import org.anormcypher.CypherParser._
import org.anormcypher._


/**
 * @author Paul J Thordarson kapunga@gmail.com
 */
object WorldDAO {
  implicit val connection = Neo4jREST()

  val pantheonParser: CypherRowParser[Pantheon] = {
    int("tmId") ~ str("name") ~ str("desc") map {
      case id ~ name ~ desc => new Pantheon(id, name, desc)
    }
  }

  val zoneParser: CypherRowParser[Zone] = {
    int("tmId") ~ str("name") map {
      case id ~ name => new Zone(id, name)
    }
  }

  val roomParser: CypherRowParser[Room] = {
    int("tmId") ~ str("title") ~ str("desc") ~ int("zoneId") map {
      case id ~ title ~ desc ~ zone => new Room(id, title, desc, Universe.getZone(zone))
    }
  }

  val pantheonMemberParser: CypherRowParser[(Int, Int)] = {
    int("pId") ~ int("zId") map {
      case pId ~ zId => (pId, zId)
    }
  }

  val linkParser: CypherRowParser[Link] = {
    int("oId") ~ int("dId") ~ str("dir") map {
      case o ~ d ~ dir => Link(o, d, dir)
    }
  }

  val baseFilter: (Persistent => Boolean) = p => p.id != -1

  def findAllPantheons(): Seq[Pantheon] = {
    val query = Cypher("MATCH (pan :Pantheon) RETURN pan.tmId AS tmId, pan.name AS name, pan.desc AS desc")
    query.list(pantheonParser).filter(baseFilter)
  }

  def findAllZones(): Seq[Zone] = {
    val query = Cypher("MATCH (zone :Zone) RETURN zone.tmId AS tmId, zone.name AS name")
    query.list(zoneParser).filter(baseFilter)
  }

  def findAllPantheonMembers(): Seq[(Int, Int)] = {
    val query = Cypher("MATCH (z :Zone)-[:Member]->(p :Pantheon) RETURN z.tmId AS zId, p.tmId AS pId")
    query.list(pantheonMemberParser)
  }

  def hasRooms(zone: Zone): Boolean = {
    val query = Cypher(s"OPTIONAL MATCH (z:Zone)--(r:Room) WHERE z.tmId = ${zone.id} RETURN COUNT(r) > 0 AS hasRooms;")

    query.single(bool("hasRooms"))
  }

  def getRoomsForZone(zone: Zone): Seq[Room] = {
    val query = Cypher(s"MATCH (room :Room)-[:Member]->(z :Zone) WHERE z.tmId = ${zone.id} " +
                       "RETURN room.tmId AS tmId, room.title AS title, room.desc AS desc, z.tmId AS zoneId")
    query.list(roomParser)
  }

  def getRoomLinksForZone(zone: Zone): Seq[Link] = {
    val query = Cypher(s"MATCH (z: Zone)--(o :Room)-[l :Link]->(d :Room)--(z: Zone) WHERE z.tmId = ${zone.id} " +
                       "RETURN o.tmId AS oId, d.tmId AS dId, l.dir AS dir")
    query.list(linkParser)
  }

  def getAllDirections: Seq[String] = Cypher("MATCH ()-[l :Link]-() RETURN DISTINCT l.dir AS dir").list(str("dir"))
}

trait Persistent {
  def id: Int
}
