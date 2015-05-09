package org.kapunga.tm.command

import org.kapunga.tm.TabResult
import akka.actor.{Actor, ActorRef}
import org.kapunga.tm.soul.Agent

/**
 * This object maintains a collection of all available commands as well as a pool of actors responsible
 * for executing the commands.  This object needs to be initialized in ThorMud with an actor pool otherwise
 * it will not work.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
// TODO Make help just list topics available for help.
object NewCommandExecutorService extends TabCompleter {
  var commandMap = Map[String, Root]()
  var executorPool: ActorRef = null

  val helpExecFunction = new ExecFunction(doHelp)
  val helpArgFinder = new StringArgFinder(() => commandMap.keys)
  val help = Root("help", Nil, helpHelp, new Arg(helpArgFinder) +> helpExecFunction)

  registerCommand(help)
  NewControlCommands.registerCommands(registerCommand)
  NewCommunicationCommands.registerCommands(registerCommand)
  NewInteractionCommands.registerCommands(registerCommand)
  NewNavigationCommands.registerCommands(registerCommand)

  /**
   * Initializes the CommandExecutor pool.  This should only be done once,
   * if it is done more than once, it will not re-initialize the pool and return the
   * pool.
   *
   * @param pool An initialized executor pool.
   * @return true if the executor pool was successfully initialized, otherwise false.
   */
  def initExecutorPool(pool: ActorRef): Boolean = if (executorPool == null) { executorPool = pool ; true } else false

  /**
   * Register a command so it can be looked up and executed.
   *
   * @param command The command we want to register.
   */
  def registerCommand(command: Root) = {
    if (command.isValid) {
      command.names.foreach(a => commandMap = commandMap + (a -> command))
    } else {
      throw new IllegalArgumentException("You cannot register an invalid command.")
    }
  }

  /**
   * This method dispatches a command to be executed in the CommandExecutor pool.  It starts by parsing out the
   * primary command and checking whether it is registered before dispatching it to a CommandExecutor.
   *
   * @param input The raw string entered by the player.
   * @param context The command context associated with this command request.
   */
  def command(input: String, context: ExecContext) = {
    val commandPair = commandSplit(input, options())

    if (input.trim == "") {
      context.executor.prompt()
    } else if (commandMap.contains(commandPair._1)) {
      executorPool ! ExecRequest(commandMap(commandPair._1), context, commandPair._2.trim())
    } else {
      val command = if (input.trimLead.indexOf(" ") == -1) input else input.substring(input.indexOf(" "))
      context.executor.tell(s"\'$command\' is not an available command.\n")
      helpHelp(context.executor)
    }
  }

  /**
   * This method handles tab completion of partial commands.
   *
   * @param partialCommand The partial command we wish to complete.
   * @param context The command context for the partial command.
   * @return The TabCompleteResult for a tab completion request.
   */
  def tabComplete(partialCommand: String, context: ExecContext): TabResult = {
    doComplete(partialCommand, context) match {
      case None =>
        val splits = commandSplit(partialCommand, options())

        if (splits._1 == partialCommand) TabResult(" ", Nil) else commandMap(splits._1).complete(splits._2, context)
      case Some(tabResult) =>
        tabResult
    }
  }

  override def options = () => commandMap.keys

  def helpHelp(agent: Agent) = {
    agent.tell("What would you like help with?")
    agent.prompt()
  }

  def doHelp(context: ExecContext, args: List[Any]) = {
    if (args.length > 0) {
      args(0) match {
        case arg: String =>
          if (commandMap.contains(arg)) commandMap(arg).doHelp(context.executor) else helpHelp(context.executor)
        case _ =>
          helpHelp(context.executor)
      }
    }
  }
}

/**
 * This trait is used primarily by objects that pose as registries for possible commands.
 * There is a convenience method to build a help function for a give command name, and a
 * method should be implemented that registers all of the commands in the registry with
 * whatever passes it a register argument, generally the CommandExecutorService.
 */
trait NewCommandRegistry {
  /**
   * Take a closure as an argument that registers each command in this registry with some outside entity.
   *
   * @param register Should be a closure that takes a Root ExecTree instance as an argument and registers it.
   */
  def registerCommands(register: Root => Unit)
}

/**
 * A simple actor to execute a command.  It is initialized in pools and runs commands so that each agent's input and
 * output is not blocked in the event that a command takes a while or hangs.  It should only receive ExecutionRequest
 * messages.
 */
class NewCommandExecutor extends Actor {
  def receive = {
    case ExecRequest(command, commandContext, subCommand) =>
      command.execute(subCommand, commandContext)
  }
}

/**
 * An encapsulation of a valid command input from a player.  This is used as a message to be passed off to
 * a CommandExecutor actor.
 *
 * @param command The command we want executed.
 * @param context The context corresponding to the an execution of this command.
 * @param subCommand The subCommand that goes along with this command.
 */
case class ExecRequest(command: Root, context: ExecContext, subCommand: String)


