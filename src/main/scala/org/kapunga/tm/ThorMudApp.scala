package org.kapunga.tm

/**
 * This is a simple wrapper App that can be used to fire up a ThurMUD instance
 * directly.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object ThorMudApp extends App {
  val thorMud = new ThorMud()

  thorMud.start()
}
