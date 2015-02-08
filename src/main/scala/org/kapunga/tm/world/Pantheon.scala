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

  def zone = pZone

  def verboseDesc = s"$name - $desc"
}
