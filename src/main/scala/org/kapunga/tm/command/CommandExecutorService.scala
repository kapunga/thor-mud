package org.kapunga.tm.command

import akka.actor.Actor
import akka.event.Logging
import org.kapunga.tm.TabCompleteResult
import org.kapunga.tm.soul.Agent
import org.kapunga.tm.world.Room

import scala.io.Source._

/**
 * This is a naive implementation of a command dispatcher for the MUD, written quickly in order
 * to have some way of testing some other basic components.  It will be completely re-written shortly.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object CommandExecutorService {
  var commandMap: Map[String, Command] = Map()

  ControlCommands.registerCommands(registerCommand)
  CommunicationCommands.registerCommands(registerCommand)
  InteractionCommands.registerCommands(registerCommand)

  val help = Command("help", List(), showHelp, (context, subCommand) => {
      val agent = context.executor

      if (subCommand == null || subCommand == "") {
        showHelp(agent)
      } else if (commandMap.contains(subCommand)) {
        commandMap(subCommand).help(agent)
      } else {
        agent.tell("There is no help available for that topic.")
        agent.prompt()
      }
    })

  registerCommand(help)

  def command(rawCommand: String, context: CommandContext) = {
    val command = rawCommand.split(" ")(0)
    val subCommand = {
      if (command.length == rawCommand.length) {
        ""
      } else {
        rawCommand.substring(command.length).trim
      }
    }

    if (command == "") {
      context.executor.prompt()
    } else if (commandMap.contains(command)) {
      commandMap(command).exec(context, subCommand)
    } else {
      context.executor.tell(s"$command is not an availableCommand.\n")
      showHelp(context.executor)
    }
  }

  def registerCommand(command: Command) = {
    commandMap = commandMap + (command.name -> command)
    command.aliases.foreach(alias => commandMap = commandMap + (alias -> command))
  }

  private def showHelp(agent: Agent): Unit = {
    val lines = fromInputStream(getClass.getResourceAsStream("/help.txt")).getLines()

    lines.foreach(line => agent.tell(line))
    agent.prompt()
  }
}

class CommandExecutor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case x => log.debug(x.toString)
  }
}

trait CommandRegistry {
  def makeHelp(command: String): Agent => Unit = {
    val resource = getClass.getResourceAsStream(s"/help/$command.txt")

    val lines = {
      if (resource == null) {
        List("Sorry, help is not available for that topic.")
      } else {
        fromInputStream(resource).getLines()
      }
    }

    (agent) => {
      lines.foreach(line => agent.tell(line))
      agent.prompt()
    }
  }

  def registerCommands(register: Command => Unit)
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
case class CommandContext(executor: Agent, room: Room)

case class Command(name: String, aliases: List[String], help: Agent => Unit, exec: (CommandContext, String) => Unit,
                   tabComplete: CommandContext => TabCompleteResult = (context) => TabCompleteResult("", List()))
