package org.kapunga.tm.command

import CommandHelpers._

/**
 * A command registry for commands that provide basic world interaction, such as
 * "look" or "take".
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
// TODO Allow look to take arguments.
object InteractionCommands extends CommandRegistry {
  val look = Command("look", List(), makeHelp("look"), (context, subCommand) => context.room.look(context.executor))

  var commandList: List[Command] = List(look)

  override def registerCommands(register: Command => Unit) = commandList.foreach(x => register(x))
}
