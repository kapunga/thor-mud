package org.kapunga.tm.command

import org.kapunga.tm.soul.AgentManager
import CommandHelpers._

/**
 * This object is a CommandRegistry for control commands such as "quit" and "restart"
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object ControlCommands extends CommandRegistry {
  val quit = Command("quit", "exit" :: Nil, makeHelp("quit"), (context, subCommand) => context.executor.quit())

  val who = Command("who", "list" :: "players" :: Nil, makeHelp("who"), (context, subCommand) => {
    context.executor.tell("Players\n=-=-=-=")
    val agents = AgentManager.getAgents
    agents.foreach(agent => context.executor.tell(s"${agent.name}"))
    context.executor.tell(s"====================\nTotal: ${agents.size} players\n")
    context.executor.prompt()
  })

  var commandList = quit :: who :: Nil

  override def registerCommands(register: Command => Unit) = commandList.foreach(x => register(x))
}
