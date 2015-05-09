package org.kapunga.tm

import org.kapunga.tm.soul.{NoSuchAgent, NullAgent, AgentManager, Agent}
import org.kapunga.tm.world._

import scala.io.Source._
import scala.language.implicitConversions


/**
 * Created by kapunga on 4/30/15.
 */
package object command {
  implicit def stringToLeadTrim(string: String): LeadingStringTrimmer = new LeadingStringTrimmer(string)

  class LeadingStringTrimmer(string: String) {
    def trimLead = string.replaceAll("^\\s+", "")
  }

  def commandSplit(comm: String, options: Iterable[String]): (String, String) = {
    val arg = options.filter(s => comm == s || comm.startsWith(s + " ")).fold("")((a, b) => if (a.length > b.length) a else b)

    val remainder = if (comm.length > arg.length) comm.substring(arg.length + 1) else comm.substring(arg.length)

    (arg, remainder.trimLead)
  }

  def completedCommand(comm: String, options: Iterable[String]): Option[String] = {
    val completed = commandSplit(comm, options)._1

    if (completed == "") None else Some(completed)
  }

  /**
   * Create a function that outputs the help file for a command to an Agent.
   *
   * @param command The command who's help file we need to look up.
   * @return A function that takes an agent as an argument and when called prints out a help file to it.
   *         In the event that a help file is unavailable, it will inform the agent that help is not available
   *         for that specific topic.
   */
  def makeHelp(command: String): Agent => Unit = {
    val resource = getClass.getResourceAsStream(s"/help/$command.txt")

    val lines = {
      if (resource == null) {
        List("Sorry, help is not available for that topic.")
      } else {
        fromInputStream(resource).getLines().toList
      }
    }

    resource.close()

    (agent) => {
      lines.foreach(line => agent.tell(line))
      agent.prompt()
    }
  }

  /**
   * CommandContext represents everything a command might possibly need to execute.  Currently this contains
   * the Agent executing the command and the room the command is executed in.  It offers valid targets for
   * actions, as well as context specific available commands, such as directions that lead out of the room
   * the agent is in.
   *
   * @param executor A reference to the agent that is executing the command.
   * @param room The room the agent is currently in.
   */
  case class ExecContext(executor: Agent, room: Room)

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

  object PantheonArgFinder extends ArgFinder[Pantheon] {
    override def getArg(command: String, context: ExecContext): (Option[Pantheon], String) = {
      Universe.allPantheons.find(p => command.startsWith(p.getName)) match {
        case Some(pantheon) =>
          (Some(pantheon), command.substring(pantheon.getName.length).trimLead)
        case None =>
          if (command.length > 0) (Some(NoSuchPantheon), command) else (Some(NullPantheon), "")
      }
    }

    override def options = () => Universe.allPantheons.map(p => p.getName)
  }

  object ZoneArgFinder extends ArgFinder[Zone] {
    override def getArg(command: String, context: ExecContext): (Option[Zone], String) = {
      Universe.allZones.find(z => command.startsWith(z.getName)) match {
        case Some(zone) =>
          (Some(zone), command.substring(zone.getName.length).trimLead)
        case None =>
          if (command.length > 0) (Some(NoSuchZone), command) else (Some(NullZone), "")
      }
    }

    override def options = () => Universe.allZones.map(z => z.getName)
  }

  object RemainderArgFinder extends ArgFinder[String] {
    override def getArg(command: String, context: ExecContext): (Option[String], String) = (Some(command), "")

    override def nibble(command: String, context: ExecContext): String = ""

    override def options = () => Nil

    override def doComplete(command: String, context: ExecContext): Option[TabResult] = Some(EmptyTab)
  }
}
