package org.kapunga.tm.soul

import org.anormcypher._
import org.anormcypher.CypherParser._
import org.kapunga.tm.world.{Universe, Universal, Pantheon}

class Spirit(soul: Soul, defaultLevel: Spirit.Value = Spirit.Whisper) {
  private var spiritLevels = Map(Universal.id -> defaultLevel).withDefault((id) => defaultSpiritLevel)

  private def defaultSpiritLevel: Spirit.Value = spiritLevels(Universal.id)

  def promote(pantheon: Pantheon = Universal): Boolean = {
    spiritLevels(pantheon.id) match {
      case Spirit.Almighty =>
        false
      case spirit: Spirit.Value =>
        spiritLevels = spiritLevels + (pantheon.id -> Spirit(spirit.id + 1))
        true
    }
  }

  def demote(pantheon: Pantheon = Universal): Boolean = {
    spiritLevels(pantheon.id) match {
      case Spirit.Whisper =>
        false
      case spirit: Spirit.Value =>
        if (pantheon != Universal && defaultSpiritLevel >= spiritLevel(pantheon)) {
          false
        } else {
          spiritLevels = spiritLevels + (pantheon.id -> Spirit(spirit.id - 1))
          true
        }
    }
  }

  def setLevel(level: Spirit.Value, pantheon: Pantheon = Universal): Boolean = {
    if (pantheon != Universal && defaultSpiritLevel > level) {
      false
    } else {
      spiritLevels = spiritLevels + (pantheon.id -> level)
      true
    }
  }

  def spiritLevel(pantheon: Pantheon = Universal) = spiritLevels(pantheon.id)
}

object Spirit extends Enumeration {
  val Whisper, Ghost, Angel, DemiGod, God, Almighty, Creator = Value

  implicit val connection = Neo4jREST()

  val spiritLevelParser: CypherRowParser[(Int, Int)] = {
    int("panthId") ~ int("level") map {
      case a ~ b => (a, b)
    }
  }

  def getSpirit(soul: Soul): Spirit = {
    val query = Cypher(s"MATCH (s :Soul)-[l :Spirit]->(p :Pantheon) WHERE s.name = '${soul.name}'"
                       + " RETURN p.tmId AS panthId, l.level AS level;")
    val spiritLevels = query.list(spiritLevelParser)

    val spirit = {
      spiritLevels.find(p => p._1 == -1) match {
        case Some(x) =>
          new Spirit(soul, Spirit(x._2))
        case None =>
          new Spirit(soul)
      }
    }

    spiritLevels.filter(p => p._1 != -1).foreach {
      l => Universe.getPantheon(l._1) match {
        case Some(pantheon) =>
          spirit.setLevel(Spirit(l._2), pantheon)
        case None =>
          // TODO Log missing pantheon.
      }
    }

    spirit
  }
}

sealed abstract class SpiritPermission
case class Global(spiritLevel: Spirit.Value) extends SpiritPermission
case class Local(spiritLevel: Spirit.Value) extends SpiritPermission
