package org.kapunga.tm.command

import CommandHelpers._
import org.kapunga.tm.world.Universe

/**
 * A command registry for commands that provide basic world interaction, such as
 * "look" or "take".
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
// TODO Allow look to take arguments.
object InteractionCommands extends CommandRegistry {
  val look = Command("look", Nil, makeHelp("look"), (context, subCommand) => context.room.look(context.executor))

  val pantheons = Command("pantheons", Nil, makeHelp("pantheons"), (context, subCommand) => {
    Universe.allPantheons.foreach(p => context.executor.tell(p.verboseDesc))
    context.executor.prompt()
  })

  var commandList = look :: pantheons :: Nil

  override def registerCommands(register: Command => Unit) = commandList.foreach(x => register(x))
}
