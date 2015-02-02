package org.kapunga.tm.command

/**
 * This object is a CommandRegistry for control commands such as "quit" and "restart"
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object ControlCommands extends CommandRegistry {
  val quit = Command("quit", List("exit"), makeHelp("quit"), (context, subCommand) => context.executor.quit())

  var commandList: List[Command] = List(quit)

  override def registerCommands(register: Command => Unit) = commandList.foreach(x => register(x))
}
