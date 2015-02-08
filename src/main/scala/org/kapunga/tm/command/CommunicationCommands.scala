package org.kapunga.tm.command

import org.kapunga.tm.soul.AgentManager
import CommandHelpers._

/**
 * This object is CommandRegistry for communication commands such as
 * "say", "tell", and "shout"
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object CommunicationCommands extends CommandRegistry {
  val say = Command("say", Nil, makeHelp("say"), (context, subCommand) => {
    import context.executor

    if (subCommand.trim == "") {
      executor.tell("What do you want to say?")
      executor.prompt()
    } else {
      context.room.agents.filter(a => a != executor).foreach(a => {
        a.tell(s"${context.executor.name} says, '${subCommand.trim}'")
        a.prompt()
      })

      executor.tell(s"You say, '$subCommand'")
      executor.prompt()
    }
  })

  val shout = Command("shout", Nil, makeHelp("shout"), (context, subCommand) => {
    import context.executor

    if (subCommand.trim == "") {
      executor.tell("What do you want to shout?")
      executor.prompt()
    } else {
      val agents = AgentManager.getAgents.filter(agent => agent != executor)

      agents.foreach(agent => {
        agent.tell(s"${executor.name} shouts, '$subCommand'")
        agent.prompt()
      })

      executor.tell(s"You shout, '$subCommand'")
      executor.prompt()
    }
  })

  val tell = Command("tell", List("whisper"), makeHelp("tell"), (context, subCommand) => {
    import context.executor

    if (subCommand == "") {
      executor.tell("Who do you want to tell something to?")
      executor.prompt()
    } else {
      val target = subCommand.split(" ")(0)
      if (target == executor.name) {
        executor.tell("Talk to yourself often?")
        executor.prompt()
      } else {
        AgentManager.getAgent(target) match {
          case Some(agent) =>
            val sentence = subCommand.substring(target.length).trim
            if (sentence.length == 0) {
              executor.tell(s"What would you like to tell ${agent.name}?")
              executor.prompt()
            } else {
              agent.tell(s"${executor.name} tells you, '$sentence'")
              agent.prompt()
              executor.tell(s"You tell ${agent.name}, '$sentence'")
              executor.prompt()
            }
          case None =>
            executor.tell(s"A player by that name is not available.")
            executor.prompt()
        }
      }
    }
  }, completeSubCommand(() => AgentManager.getAgents.map(agent => agent.name)))

  var commandList: List[Command] = List(say, shout, tell)

  override def registerCommands(register: Command => Unit) = commandList.foreach(x => register(x))
}
