package org.kapunga.tm

import org.kapunga.tm.command.CommandContext
import org.kapunga.tm.soul.Agent

import scala.io.Source._

/**
 * This is a naive implementation of a command dispatcher for the MUD, written quickly in order
 * to have some way of testing some other basic components.  It will be completely re-written shortly.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object CommandExecutorService {
  def command(rawCommand: String, context: CommandContext) = {
    val command = rawCommand.split(" ")(0)
    val subCommand = {
      if (command.length == rawCommand.length) {
        ""
      } else {
        rawCommand.substring(command.length).trim
      }
    }
    command match {
      case "" =>
        context.executor.prompt()
      case "help" =>
        showHelp(context.executor)
      case "look" =>
        context.room.look(context.executor)
      case "quit" =>
        context.executor.quit()
      case "say" =>
        context.room.agents.filter(a => a != context.executor).foreach(a => {
          a.tell(s"${context.executor.name} said, '$subCommand'")

          a.prompt()
        })
        context.executor.tell(s"You said, '$subCommand'")
        context.executor.prompt()
      case _ =>
        context.executor.tell(s"$command is not an availableCommand.\n")
        showHelp(context.executor)
    }
  }

  private def showHelp(agent: Agent): Unit = {
    val lines = fromInputStream(getClass.getResourceAsStream("/help.txt")).getLines()

    lines.foreach(line => agent.tell(line))
    agent.prompt()
  }
}
