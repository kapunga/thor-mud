package org.kapunga.tm.command

import org.kapunga.tm.command.CommandHelpers.{splitCommand, doComplete}
import org.kapunga.tm.TabCompleteResult
import org.kapunga.tm.soul.{AgentManager, Agent}

abstract class CommandMatcher[A] {
  def matchCommand(command: String, context: CommandContext): (Option[A], Option[String])
  def completeCommand(command: String, context: CommandContext): Option[TabCompleteResult]
}

object MatchAnyPlayer extends CommandMatcher[Agent] {
  override def matchCommand(command: String, context: CommandContext): (Option[Agent], Option[String]) = {
    val commandSplit = splitCommand(command)

    (AgentManager.getAgent(commandSplit.command), commandSplit.subCommandOption)
  }

  override def completeCommand(command: String, context: CommandContext): Option[TabCompleteResult] = {
    val commandSplit = splitCommand(command)

    commandSplit.subCommandOption match {
      case Some(subCommand) =>
        None
      case None =>
        Some(doComplete(commandSplit.command, AgentManager.getAgents.map(a => a.name)))
    }
  }
}

class StringListMatcher(stringList: List[String]) extends CommandMatcher[String] {
  override def matchCommand(command: String, context: CommandContext): (Option[String], Option[String]) = {
    val commandSplit = splitCommand(command)

    (stringList.find(p => p == commandSplit.command), commandSplit.subCommandOption)
  }

  override def completeCommand(command: String, context: CommandContext): Option[TabCompleteResult] = {
    val commandSplit = splitCommand(command)

    commandSplit.subCommandOption match {
      case Some(subCommand) =>
        None
      case None =>
        Some(doComplete(commandSplit.command, stringList))
    }
  }
}

