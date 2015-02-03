package org.kapunga.tm.command

import akka.actor.{ActorRef, Actor}
import org.kapunga.tm.TabCompleteResult
import org.kapunga.tm.soul.Agent
import org.kapunga.tm.world.Room

import scala.io.Source._

/**
 * This object maintains a collection of all available commands as well as a pool of actors responsible
 * for executing the commands.  This object needs to be initialized in ThorMud with an actor pool otherwise
 * it will not work.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object CommandExecutorService {
  var commandMap = Map[String, Command]()
  var executorPool: ActorRef = null

  ControlCommands.registerCommands(registerCommand)
  CommunicationCommands.registerCommands(registerCommand)
  InteractionCommands.registerCommands(registerCommand)

  /**
   * The help command is defined here as it relies on having access to the complete command map.
   */
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

  /**
   * Initializes the CommandExecutor pool.  This should only be done once,
   * if it is done more than once, it will not re-initialize the pool and return the
   * pool.
   *
   * @param pool An initialized executor pool.
   * @return true if the executor pool was successfully initialized, otherwise false.
   */
  def initExecutorPool(pool: ActorRef): Boolean = {
    if (executorPool == null) {
      executorPool = pool
      true
    } else {
      false
    }
  }

  /**
   * This method dispatches a command to be executed in the CommandExecutor pool.  It starts by parsing out the
   * primary command and checking whether it is registered before dispatching it to a CommandExecutor.
   *
   * @param rawCommand The raw string entered by the player.
   * @param context The command context associated with this command request.
   */
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
      executorPool ! ExecutionRequest(commandMap(command), context, subCommand)
    } else {
      context.executor.tell(s"$command is not an availableCommand.\n")
      showHelp(context.executor)
    }
  }

  /**
   * Register a command so it can be looked up and executed.
   *
   * @param command The command we want to register.
   */
  def registerCommand(command: Command) = {
    commandMap = commandMap + (command.name -> command)
    command.aliases.foreach(alias => commandMap = commandMap + (alias -> command))
  }

  /**
   * Output the default help file to an agent.
   * @param agent The agent to receive the help file.
   */
  def showHelp(agent: Agent): Unit = {
    val helpFile = getClass.getResourceAsStream("/help.txt")
    val lines = fromInputStream(helpFile).getLines().toList

    helpFile.close()

    lines.foreach(line => agent.tell(line))
    agent.prompt()
  }
}

/**
 * A simple actor to execute a command.  It is initialized in pools and runs commands so that each agent's input and
 * output is not blocked in the event that a command takes a while or hangs.  It should only receive ExecutionRequest
 * messages.
 */
class CommandExecutor extends Actor {
  def receive = {
    case ExecutionRequest(command, commandContext, subCommand) =>
      command.exec(commandContext, subCommand)
  }
}

/**
 * This trait is used primarily by objects that pose as registries for possible commands.
 * There is a convenience method to build a help function for a give command name, and a
 * method should be implemented that registers all of the commands in the registry with
 * whatever passes it a register argument, generally the CommandExecutorService.
 */
trait CommandRegistry {
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
   * Take a closure as an argument that registers each command in this registry with some outside entity.
   *
   * @param register Should be a closure that takes a Command as an argument and registers it.
   */
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

/**
 * Encapsulates a command that a player might enter.
 *
 * @param name The primary name of the command.
 * @param aliases Aliases that also resolve to this command.
 * @param help A function that outputs help information on this command to a player's agent.
 * @param exec The meat of the command, a function that actually performs the work of this command.
 * @param tabComplete A function that is called when a tab completion request is made with this command already typed
 *                    in.  It should return a tab completion event for subcommands if possible.  The default is to just
 *                    return an empty TabCompleteResult.
 */
case class Command(name: String, aliases: List[String], help: Agent => Unit, exec: (CommandContext, String) => Unit,
                   tabComplete: CommandContext => TabCompleteResult = (context) => TabCompleteResult("", List()))

/**
 * An encapsulation of a valid command input from a player.  This is used as a message to be passed off to
 * a CommandExecutor actor.
 *
 * @param command The command we want executed.
 * @param context The context corresponding to the an execution of this command.
 * @param subCommand The subCommand that goes along with this command.
 */
case class ExecutionRequest(command: Command, context: CommandContext, subCommand: String)
