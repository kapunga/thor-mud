package org.kapunga.tm.command

/**
 * Created by kapunga on 1/29/15.
 */
// TODO Allow look to take arguments.
object InteractionCommands extends CommandRegistry {
  val look = Command("look", List(), makeHelp("look"), (context, subCommand) => context.room.look(context.executor))

  var commandList: List[Command] = List(look)

  override def registerCommands(register: Command => Unit) = commandList.foreach(x => register(x))
}
