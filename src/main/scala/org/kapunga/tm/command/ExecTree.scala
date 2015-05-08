package org.kapunga.tm.command

import org.kapunga.tm.{EmptyTab, TabResult}
import org.kapunga.tm.soul.Agent

/**
 * Created by kapunga on 2/24/15.
 */
trait ExecTree {
  def complete(command: String, context: ExecContext): TabResult

  def execute(command: String, context: ExecContext, args: List[Any] = Nil): Unit

  def isValid: Boolean

  def +>(next: ExecTree): ExecTree

  def +>(next: (String, ExecTree)): ExecTree
}

case class Root(name: String, aliases: List[String], help: Agent => Unit, next: ExecTree = null) extends ExecTree {
  require(next != null && next.isValid, "Root classes can only be instantiated with a valid ExecTree.")

  def complete(command: String, context: ExecContext): TabResult = next.complete(command, context)

  def execute(command: String, context: ExecContext, args: List[Any] = Nil) = {
    try {
      next.execute(command, context)
    } catch {
      case iae: IllegalArgumentException =>
        help(context.executor)
    }
  }

  def +>(next: ExecTree) = throw new IllegalArgumentException("This ExecTree already has a next.")

  def +>(next: (String, ExecTree)) = throw new IllegalArgumentException("You cannot append Map to a Root type ExecTree")

  def isValid = if (next == null) false else next.isValid

  def names: List[String] = aliases :+ name

  def doHelp(agent: Agent) = help(agent)
}

class Arg[A](af: ArgFinder[A], next: ExecTree = null) extends ExecTree {
  def complete(command: String, context: ExecContext): TabResult = {
    af.doComplete(command, context) match {
      case Some(tabResult) =>
        tabResult
      case None =>
        next.complete(af.nibble(command, context), context)
    }
  }

  def execute(command: String, context: ExecContext, args: List[Any] = Nil) = {
    af.getArg(command, context) match {
      case (Some(arg), subCommand) =>
        next.execute(subCommand, context, args :+ arg)
      case (None, subCommand) =>
        throw new IllegalArgumentException
    }
  }

  def +>(next: ExecTree) = {
    if (this.next == null) {
      new Arg(af, next)
    } else {
      throw new IllegalArgumentException("This ExecTree already has a next.")
    }
  }

  def +>(next: (String, ExecTree)) = throw new IllegalArgumentException("Arg type ExecTrees cannot append a map entry.")

  def isValid = if (next == null) false else next.isValid
}

class Fork(branches: Map[String, ExecTree] = Map()) extends ExecTree {
  def complete(command: String, context: ExecContext): TabResult = {
    val sC = CommandHelpers.splitCommand(command)

    sC.subCommandOption match {
      case None =>
        CommandHelpers.doComplete(sC.command, branches.keys)
      case Some(arg) =>
        if (branches.contains(sC.command)) branches(sC.command).complete(command, context) else EmptyTab
    }
  }

  def execute(command: String, context: ExecContext, args: List[Any] = Nil) = {
    val sC = CommandHelpers.splitCommand(command)

    if (branches.contains(sC.command)) {
      branches(sC.command).execute(sC.subCommand, context, args)
    } else {
      context.executor.tell(s"Unknown argument: ${sC.command}")
    }
  }

  def +>(next: ExecTree) = throw new IllegalArgumentException("Fork ExecTree can only append map entries.")

  def +>(next: (String, ExecTree)) = {
    if (!branches.contains(next._1)) {
      new Fork(branches + next)
    } else {
      throw new IllegalArgumentException("Duplicate map entry.")
    }
  }

  def isValid = if (branches.size == 0) false else branches.values.forall(b => b.isValid)
}

class ExecFunction(func: (ExecContext, List[Any]) => Unit) extends ExecTree {
  def complete(command: String, context: ExecContext) = EmptyTab

  def execute(command: String, context: ExecContext, args: List[Any] = Nil) =
    if (command != null) func(context, args :+ command) else func(context, args)

  def +>(next: ExecTree) = throw new IllegalArgumentException("ExecFunction cannot have next.")

  def +>(next: (String, ExecTree)) = throw new IllegalArgumentException("ExecFunction cannot have next.")

  def isValid = true
}

trait ArgFinder[A] extends TabCompleter {
  def getArg(command: String, context: ExecContext): (Option[A], String)

  def nibble(command: String, context: ExecContext): String
}

trait TabCompleter {
  def options: Traversable[String]

  def doComplete(command: String, context: ExecContext): Option[TabResult] = {
    val optionList = options

    optionList.find(o => (o + " ").equals(command)) match {
      case Some(x) =>
        None
      case None =>
        val possible = optionList.filter(o => o.startsWith(command) || command.trim == "")

        if (possible.size == 0) {
          Some(EmptyTab)
        } else if (possible.size == 1) {
          Some(TabResult(possible.toList(0).substring(command.length) + " ", Nil))
        } else {
          if (optionList.exists(p => p == command)) {
            Some(TabResult("", possible.toList))
          } else {
            val complete = possible.reduce((a, b) => (a, b).zipped.takeWhile(Function.tupled(_ == _)).unzip._1.mkString)

            Some(TabResult(complete.substring(command.length), possible.toList))
          }
        }
    }
  }

}


