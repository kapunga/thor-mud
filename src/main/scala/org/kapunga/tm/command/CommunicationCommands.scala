package org.kapunga.tm.command

import org.kapunga.tm.soul.{NoSuchAgent, NullAgent, Agent, AgentManager}

/**
 * This object is CommandRegistry for communication commands such as
 * "say", "tell", and "shout"
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object CommunicationCommands extends CommandRegistry {
  def doSay(context: ExecContext, args: List[Any]) = {
    import context.executor

    val words = if (args.length > 0 && args(0).isInstanceOf[String]) args(0).asInstanceOf[String] else ""

    if (words.trim == "") {
      executor.tell("What do you want to say?")
      executor.prompt()
    } else {
      context.room.agents.filter(a => a != executor).foreach(a => {
        a.tell(s"${executor.name} says, '${words.trim}'")
        a.prompt()
      })

      executor.tell(s"You say, '${words.trim}'")
      executor.prompt()
    }
  }

  def doShout(context: ExecContext, args: List[Any]) = {
    import context.executor

    val words = if (args.length > 0 && args(0).isInstanceOf[String]) args(0).asInstanceOf[String] else ""

    if (words.trim == "") {
      executor.tell("What do you want to shout?")
      executor.prompt()
    } else {
      val agents = AgentManager.getAgents.filter(agent => agent != executor)

      agents.filter(a => a != executor).foreach(a => {
        a.tell(s"${executor.name} shouts, '${words.trim}'")
        a.prompt()
      })

      executor.tell(s"You shout, '${words.trim}'")
      executor.prompt()
    }
  }

  def doTell(context: ExecContext, args: List[Any]) = {
    import context.executor

    val target = if (args.length > 0 && args(0).isInstanceOf[Agent]) args(0).asInstanceOf[Agent] else null
    val words = if (args.length > 1 && args(1).isInstanceOf[String]) args(1).asInstanceOf[String] else ""

    if (target == null) throw new IllegalArgumentException // TODO: Maybe log an error here?

    target match {
      case NullAgent =>
        executor.tell("Who do you want to tell something to?")
        executor.prompt()
      case NoSuchAgent =>
        executor.tell("A player by that name is not available.")
        executor.prompt()
      case _ =>
        if (target.name == executor.name) {
          executor.tell("Talk to yourself often?")
          executor.prompt()
        } else {
          if (words.length == 0) {
            executor.tell(s"What would you like to tell ${target.name}?")
            executor.prompt()
          } else {
            target.tell(s"${executor.name} tells you, '$words'")
            target.prompt()
            executor.tell(s"You tell ${target.name}, '$words'")
            executor.prompt()
          }
        }
    }
  }

  val say = Root("say", Nil, makeHelp("say"), new Arg(RemainderArgFinder) +> new ExecFunction(doSay))

  val shout = Root("shout", Nil, makeHelp("shout"), new Arg(RemainderArgFinder) +> new ExecFunction(doShout))

  val tell = Root("tell", "whisper" :: Nil, makeHelp("tell"),
    new Arg(LoggedInPlayerArgFinder) +> (new Arg(RemainderArgFinder) +> new ExecFunction(doTell)))

  val commandList = say :: shout :: tell :: Nil

  override def registerCommands(register: Root => Unit) = commandList.foreach(x => register(x))
}
