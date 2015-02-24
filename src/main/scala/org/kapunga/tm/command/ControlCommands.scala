package org.kapunga.tm.command

import org.kapunga.tm.EmptyTabComplete
import org.kapunga.tm.soul.AgentManager
import CommandHelpers._

/**
 * This object is a CommandRegistry for control commands such as "quit" and "restart"
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object ControlCommands extends CommandRegistry {
  val PROMOTE = "promote"
  val DEMOTE = "demote"
  val SET = "set"
  val SPIRIT_COMMANDS = PROMOTE :: DEMOTE :: SET :: Nil

  val quit = Command("quit", "exit" :: Nil, makeHelp("quit"), (context, subCommand) => context.executor.quit())

  val who = Command("who", "list" :: "players" :: Nil, makeHelp("who"), (context, subCommand) => {
    context.executor.tell("Players\n=-=-=-=")
    val agents = AgentManager.getAgents
    agents.foreach(agent => context.executor.tell(s"${agent.who}"))
    context.executor.tell(s"====================\nTotal: ${agents.size} players\n")
    context.executor.prompt()
  })

  val spirit = Command("spirit", Nil, makeHelp("spirit"), (context, subCommand) => {
    import context.executor
    val split = splitCommand(subCommand)
    SPIRIT_COMMANDS.find(p => p == split.command) match {
      case None =>
        executor.tell("That is not a valid spirit command.")
        makeHelp("spirit")(executor)
      case Some(comm) =>
        context.executor.tell("'spirit' command is unimplemented.")
        context.executor.prompt()
    }
  }, (context, command) => {
    val split = splitCommand(command)
    if (split.hasSubCommand) {
      split.subCommand match {
        case PROMOTE | DEMOTE =>
          EmptyTabComplete
        case SET =>
          EmptyTabComplete
        case _ =>
          EmptyTabComplete
      }
    } else {
      doComplete(split.command, SPIRIT_COMMANDS)
    }
  })

  val commandList = quit :: who :: spirit :: Nil

  override def registerCommands(register: Command => Unit) = commandList.foreach(x => register(x))
}

object SpiritSubCommandMatcher extends StringListMatcher(ControlCommands.SPIRIT_COMMANDS)
