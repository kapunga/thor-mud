package org.kapunga.tm.command

import org.kapunga.tm.world.Universe

/**
 * A command registry for commands that provide basic world interaction, such as
 * "look" or "take".
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object InteractionCommands extends CommandRegistry {
  val look = Root("look", Nil, makeHelp("look"), new ExecFunction((context, subCommand) => context.room.look(context.executor)))

  val pantheons = Root("pantheons", Nil, makeHelp("pantheons"), new ExecFunction((context, subCommand) => {
    Universe.allPantheons.foreach(p => context.executor.tell(p.verboseDesc))
    context.executor.prompt()
  }))

  val commandList = look :: pantheons :: Nil

  override def registerCommands(register: Root => Unit) = commandList.foreach(x => register(x))
}
