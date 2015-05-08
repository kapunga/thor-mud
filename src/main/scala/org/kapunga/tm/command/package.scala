package org.kapunga.tm

/**
 * Created by kapunga on 4/30/15.
 */
package object command {
  def commandSplit(comm: String, options: Iterable[String]): (String, String) = {
    val arg = options.filter(s => comm.startsWith(s)).fold("")((a, b) => if (a.length > b.length) a else b)

    val remainder = if (comm.length > arg.length) comm.substring(arg.length + 1) else comm.substring(arg.length)

    (arg, remainder.replaceAll("^\\s+", ""))
  }

  class StringArgFinder(setFetcher: () => Iterable[String]) extends ArgFinder[String] {
    override def getArg(command: String, context: ExecContext): (Option[String], String) = {
      val sr = commandSplit(command, setFetcher())

      (Some(sr._1), sr._2)
    }

    override def nibble(command: String, context: ExecContext): String = commandSplit(command, options)._2

    override def options = setFetcher()
  }
}
