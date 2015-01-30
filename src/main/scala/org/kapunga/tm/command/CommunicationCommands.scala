package org.kapunga.tm.command

/**
 * Created by kapunga on 1/29/15.
 */
object CommunicationCommands extends CommandRegistry {
  val say = Command("say", List(), makeHelp("say"), (context, subCommand) => {
    context.room.agents.filter(a => a != context.executor).foreach(a => {
      a.tell(s"${context.executor.name} said, '$subCommand'")

      a.prompt()
    })
    context.executor.tell(s"You said, '$subCommand'")
    context.executor.prompt()
  })

  var commandList: List[Command] = List(say)

  override def registerCommands(register: Command => Unit) = commandList.foreach(x => register(x))
}
