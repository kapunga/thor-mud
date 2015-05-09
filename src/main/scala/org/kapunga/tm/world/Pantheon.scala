package org.kapunga.tm.world

/**
 *
 * @author Paul J Thordarson kapunga@gmail.com
 *
 * @param tmId
 * @param name
 * @param desc
 */
class Pantheon(tmId: Int, name: String, desc: String) extends Persistent {
  private var pZone: Zone = null

  def id = tmId

  def setZone(zone: Zone): Boolean = {
    if (pZone == null) {
      pZone = zone
      pZone.load()
      true
    } else {
      false
    }
  }

  def getName = name

  def zone = pZone

  def verboseDesc = s"$name - $desc"
}

object Universal extends Pantheon(-1, "The Universal Pantheon", "The default for all pantheons.")

object NullPantheon extends Pantheon(-2, "NULL", "Program error, you should never see this.")

object NoSuchPantheon extends Pantheon(-2, "NULL", "Program error, you should never see this.")
