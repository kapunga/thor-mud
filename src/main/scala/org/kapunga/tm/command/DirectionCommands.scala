package org.kapunga.tm.command

import org.kapunga.tm.EmptyTabComplete
import org.kapunga.tm.command.CommandHelpers._
import org.kapunga.tm.soul.Agent
import org.kapunga.tm.world.{Universe, WorldDAO}

/**
 * @author Paul J Thordarson kapunga@gmail.com
 */
object DirectionCommands extends CommandRegistry {
  val WARP_PANTHEON = "pantheon"
  val WARP_PLAYER = "player"
  val WARP_ZONE = "zone"

  val directions = WorldDAO.getAllDirections.sorted

  val directionList = directions.mkString(" ")

  val helpString = s"<direction>\n-----------\nPossible directions: $directionList\n" +
    "usage: <direction>\n\nLeave a room in a specific direction if possible."

  val helpCommand: (Agent => Unit) = (agent) => {
    agent.tell(helpString)
    agent.prompt()
  }

  val warp = Command("warp", Nil, makeHelp("warp"), (context, subCommand) => {
    import context.{executor, room};

    val split = splitCommand(subCommand)
    split.command match {
      case WARP_PANTHEON =>
        executor.tell("Warping to pantheons is not yet implemented.")
        executor.prompt()
      case WARP_PLAYER =>
        executor.tell("Warping to players is not yet implemented.")
        executor.prompt()
      case WARP_ZONE =>
        Universe.allZones.find(z => z.getName == split.subCommand.trim) match {
          case Some(zone) =>
            zone.warp(executor, room)
          case None =>
            executor.tell("There is no zone by that name.")
            executor.prompt()
        }
      case _ =>
        executor.tell("That is not a valid warp target.")
        makeHelp("warp")(executor)
    }
  }, (context, subCommand) => {
    val warpTypes = WARP_PANTHEON :: WARP_PLAYER :: WARP_ZONE :: Nil
    val split = splitCommand(subCommand)

    if (!split.hasSubCommand) {
      warpTypes.find(s => s == split.command) match {
        case Some(string) =>
          doComplete("", Universe.allZones.map(z => z.getName))
        case None =>
          doComplete(split.command, warpTypes)
      }
    } else {
      split.command match {
        case WARP_ZONE =>
          doComplete(split.subCommand, Universe.allZones.map(z => z.getName))
        case _ =>
          EmptyTabComplete
      }
    }
  })

  val commandList = warp :: Nil

  override def registerCommands(register: Command => Unit) = {
    directions.foreach(direction => {
      register(Command(direction, List(direction.charAt(0).toString), helpCommand,
        (context, subCommand) => context.room.move(context.executor, direction)))
    })
    commandList.foreach(command => register(command))
  }
}
