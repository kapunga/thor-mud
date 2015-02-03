package org.kapunga.tm.command

import akka.actor.{ActorRef, Actor}
import org.kapunga.tm.{EmptyTabComplete, TabCompleteResult}
import org.kapunga.tm.soul.Agent
import org.kapunga.tm.world.Room

import scala.io.Source._

import CommandHelpers._

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
    }, completeSubCommand(() => commandMap.keys))

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
    val commandPair = splitCommand(rawCommand)

    if (commandPair.command == "") {
      context.executor.prompt()
    } else if (commandMap.contains(commandPair.command)) {
      executorPool ! ExecutionRequest(commandMap(commandPair.command), context, commandPair.subCommand.trim())
    } else {
      context.executor.tell(s"${commandPair.command} is not an availableCommand.\n")
      showHelp(context.executor)
    }
  }

  /**
   * This method handles tab completion of partial commands.
   *
   * @param partialCommand The partial command we wish to complete.
   * @param context The command context for the partial command.
   * @return The TabCompleteResult for a tab completion request.
   */
  def tabComplete(partialCommand: String, context: CommandContext): TabCompleteResult = {
    val commandPair = splitCommand(partialCommand)

    if(commandPair.hasSubCommand || partialCommand.endsWith(" ")) {
      if (commandMap.contains(commandPair.command)) {
        commandMap(commandPair.command).tabComplete(context, commandPair.subCommand)
      } else {
        EmptyTabComplete
      }
    } else {
      doComplete(partialCommand, commandMap.keys)
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
 * A case class representing a command input split up into a command and a subCommand.
 * @param command The command part of the pair.
 * @param subCommand The subCommand part of the pair.
 */
case class CommandPair(command: String, subCommand: String) {
  def hasSubCommand: Boolean = subCommand != null && subCommand != ""
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
                   tabComplete: (CommandContext, String) => TabCompleteResult =
                   (context, subCommand) => TabCompleteResult("", List()))

/**
 * An encapsulation of a valid command input from a player.  This is used as a message to be passed off to
 * a CommandExecutor actor.
 *
 * @param command The command we want executed.
 * @param context The context corresponding to the an execution of this command.
 * @param subCommand The subCommand that goes along with this command.
 */
case class ExecutionRequest(command: Command, context: CommandContext, subCommand: String)

/**
 * This object is a holder for a bunch of convenience methods used in the creation of commands.
 */
object CommandHelpers {
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
   * Split a command string into a command and a subCommand.
   *
   * @param commandString The input command string
   * @return The command pair created by splitting the commandString.  Command and subCommand components
   *         default to an empty string.
   */
  def splitCommand(commandString: String): CommandPair = {
    val command = if (commandString.split(" ").length > 0) commandString.split(" ")(0) else ""
    val subCommand = {
      if (command.length == commandString.length) {
        ""
      } else {
        commandString.substring(command.length + 1)
      }
    }

    CommandPair(command, subCommand)
  }

  /**
   * Performs tab completion on a generic subCommand.
   *
   * @param possible A generator function for an iterable of possible commands.
   * @param context The command context for a tab completion request.
   * @param subCommand A subCommand of a tab completion request.
   * @return The TabCompleteResult for this tab completion request.
   */
  def completeSubCommand(possible: () => Iterable[String])(context: CommandContext, subCommand: String): TabCompleteResult = {
    val commandPair = splitCommand(subCommand)

    if(commandPair.hasSubCommand || subCommand.endsWith(" ")) {
      EmptyTabComplete
    } else {
      doComplete(commandPair.command, possible())
    }
  }

  /**
   * Perform the matching for a tab completion.  Takes a fragment of a command that is trying to be matched
   * and a list of acceptable targets to match against.
   *
   * @param commFrag The fragment of a command we are matching.
   * @param possible The list of possible choices we need to match the fragment against.
   * @return A TabCompleteResult for this particular command fragment.
   */
  def doComplete(commFrag: String, possible: Iterable[String]): TabCompleteResult = {
    val possibleCommands = possible.filter(command => command.indexOf(commFrag) == 0)

    possibleCommands.size match {
      case 0 =>
        EmptyTabComplete
      case 1 =>
        TabCompleteResult(possibleCommands.toList(0).substring(commFrag.size) + " ", List())
      case _ =>
        val first: String = possibleCommands.toList(0)

        var subSeq = commFrag

        for (i <- 0 until first.size) {
          if (possibleCommands.forall(c => c.indexOf(first.substring(0, i)) == 0)) {
            subSeq = first.substring(0, i)
          }
        }

        TabCompleteResult(subSeq.substring(commFrag.size), possibleCommands.toList)
    }
  }
}
