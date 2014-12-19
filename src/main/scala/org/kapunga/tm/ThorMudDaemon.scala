package org.kapunga.tm

import org.apache.commons.daemon.{DaemonContext, Daemon}

/**
 * This class is a Daemon wrapper for ThorMud so ThorMud can be managed
 * as a service using jsvc.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
class ThorMudDaemon extends Daemon {
  val thorMud = new ThorMud()

  override def init(context: DaemonContext): Unit = {

  }

  override def start(): Unit = {
    thorMud.start()
  }

  override def stop(): Unit = {
    thorMud.stop()
  }

  override def destroy(): Unit = {

  }
}
