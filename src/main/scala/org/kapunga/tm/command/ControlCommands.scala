package org.kapunga.tm.command

/**
 * Created by kapunga on 1/29/15.
 */
object ControlCommands extends CommandRegistry {
  val quit = Command("quit", List("exit"), makeHelp("quit"), (context, subCommand) => context.executor.quit())

  var commandList: List[Command] = List(quit)

  override def registerCommands(register: Command => Unit) = commandList.foreach(x => register(x))
}
