package org.kapunga.tm.command

import org.kapunga.tm.soul.{NullAgent, NoSuchAgent, Agent}
import org.kapunga.tm.world._

/**
 * Created by kapunga on 5/8/15.
 */
object NewNavigationCommands extends NewCommandRegistry{
  val WARP_PANTH = "pantheon"
  val WARP_PLAYER = "player"
  val WARP_ZONE = "zone"

  def doWarpPantheon(context: ExecContext, args: List[Any]) = {
    import context.{executor, room}

    val pantheon = if (args.length > 0 && args(0).isInstanceOf[Pantheon]) args(0).asInstanceOf[Pantheon] else null

    if (pantheon == null) throw new IllegalArgumentException // TODO: Maybe log an error here?

    pantheon match {
      case NullPantheon =>
        executor.tell("What pantheon do you wish to warp to?")
        executor.prompt()
      case NoSuchPantheon =>
        executor.tell("There is no pantheon by that name.")
        executor.prompt()
      case _ =>
        pantheon.zone.warp(executor, room)
    }
  }

  def doWarpPlayer(context: ExecContext, args: List[Any]) = {
    import context.{executor, room}

    val target = if (args.length > 0 && args(0).isInstanceOf[Agent]) args(0).asInstanceOf[Agent] else null

    if (target == null) throw new IllegalArgumentException // TODO: Maybe log an error here?

    target match {
      case NullAgent =>
        executor.tell("Which player do you wish to warp to?")
        executor.prompt()
      case NoSuchAgent =>
        executor.tell("There is no player by that name logged in.")
        executor.prompt()
      case _ =>
        // TODO: Push room and warp down a level.
        room.vacate(executor, "disappears in a puff of smoke.")
        executor.tell("You feel your body get tugged through the fabric of space.")
        target.location.enter(executor, "appears in a puff of smoke.")
    }
  }

  def doWarpZone(context: ExecContext, args: List[Any]) = {
    import context.{executor, room}

    val zone = if (args.length > 0 && args(0).isInstanceOf[Zone]) args(0).asInstanceOf[Zone] else null

    if (zone == null) throw new IllegalArgumentException // TODO: Maybe log an error here?

    zone match {
      case NullZone =>
        executor.tell("What zone do you wish to warp to?")
        executor.prompt()
      case NoSuchZone =>
        executor.tell("There is no zone by that name.")
        executor.prompt()
      case _ =>
        zone.warp(executor, room)
    }
  }

  val warpPanth = new Arg(PantheonArgFinder) +> new ExecFunction(doWarpPantheon)

  val warpPlayer = new Arg(LoggedInPlayerArgFinder) +> new ExecFunction(doWarpPlayer)

  val warpZone = new Arg(ZoneArgFinder) +> new ExecFunction(doWarpZone)

  val warpTree = new Fork +> (WARP_PANTH, warpPanth) +> (WARP_PLAYER, warpPlayer) +> (WARP_ZONE, warpZone)

  val warp = Root("warp", Nil, makeHelp("warp"), warpTree)

  val directions = WorldDAO.getAllDirections.sorted

  val directionList = directions.mkString(" ")

  val directionHelpString = s"<direction>\n-----------\nPossible directions: $directionList\n" +
    "usage: <direction>\n\nLeave a room in a specific direction if possible."

  val directionHelp: (Agent => Unit) = (agent) => {
    agent.tell(directionHelpString)
    agent.prompt()
  }

  val commandList =  warp :: Nil

  override def registerCommands(register: Root => Unit) = {
    directions.foreach(direction => {
      register(Root(direction, List(direction.charAt(0).toString), directionHelp,
        new ExecFunction((context, subCommand) => context.room.move(context.executor, direction))))
    })
    commandList.foreach(command => register(command))
  }
}
