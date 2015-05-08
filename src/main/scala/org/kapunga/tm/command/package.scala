package org.kapunga.tm

import org.kapunga.tm.soul.{NoSuchAgent, NullAgent, AgentManager, Agent}


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
      val sr = commandSplit(command, options())

      (Some(sr._1), sr._2)
    }

    override def options = () => setFetcher()
  }

  object LoggedInPlayerArgFinder extends ArgFinder[Agent] {
    override def getArg(command: String, context: ExecContext): (Option[Agent], String) = {
      val player = if (command.indexOf(" ") != -1) command.substring(0, command.indexOf(" ")) else command
      val remainder = command.substring(player.length).trim

      AgentManager.getAgent(player) match {
        case Some(playerAgent) =>
          (Some(playerAgent), remainder)
        case None =>
          if (player.length > 0) (Some(NoSuchAgent), remainder) else (Some(NullAgent), remainder)
      }
    }

    override def options = () => AgentManager.getAgents.map(a => a.name)
  }

  object RemainderArgFinder extends ArgFinder[String] {
    override def getArg(command: String, context: ExecContext): (Option[String], String) = (Some(command), "")

    override def nibble(command: String, context: ExecContext): String = ""

    override def options = () => Nil

    override def doComplete(command: String, context: ExecContext): Option[TabResult] = Some(EmptyTab)
  }
}
