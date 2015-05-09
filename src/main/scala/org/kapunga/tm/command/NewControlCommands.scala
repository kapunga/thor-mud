package org.kapunga.tm.command

import org.kapunga.tm.soul.AgentManager

/**
 * Created by kapunga on 5/3/15.
 */
object NewControlCommands extends NewCommandRegistry {
  val quit = Root("quit", "exit" :: Nil, makeHelp("quit"), new ExecFunction((context, args) => context.executor.quit()))

  val who = Root("who", "list" :: "players" :: Nil, makeHelp("who"), new ExecFunction((context, args) => {
    context.executor.tell("Players\n=-=-=-=")
    val agents = AgentManager.getAgents
    agents.foreach(agent => context.executor.tell(s"${agent.who}"))
    context.executor.tell(s"====================\nTotal: ${agents.size} players\n")
    context.executor.prompt()
  }))

  /*
  val PROMOTE = "promote"
  val DEMOTE = "demote"
  val SET = "set"
  val SPIRIT_COMMANDS = PROMOTE :: DEMOTE :: SET :: Nil

  val spirit = Command("spirit", Nil, makeHelp("spirit"), (context, subCommand) => {
    import context.executor
    val split = splitCommand(subCommand)
    SPIRIT_COMMANDS.find(p => p == split.command) match {
      case None =>
        executor.tell("That is not a valid spirit command.")
        makeHelp("spirit")(executor)
      case Some(comm) =>
        context.executor.tell("'spirit' command is unimplemented.")
        context.executor.prompt()
    }
  }, (context, command) => {
    EmptyTab
  })
   */

  val commandList = quit :: who :: Nil

  override def registerCommands(register: Root => Unit) = commandList.foreach(x => register(x))
}
