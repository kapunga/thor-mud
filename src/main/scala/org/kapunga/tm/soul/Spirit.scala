package org.kapunga.tm.soul

import org.kapunga.tm.world.{Universal, Pantheon}

class Spirit(defaultLevel: Spirit.Value = Spirit.Whisper) {
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
}

sealed abstract class SpiritPermission
case class Global(spiritLevel: Spirit.Value) extends SpiritPermission
case class Local(spiritLevel: Spirit.Value) extends SpiritPermission
