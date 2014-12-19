package org.kapunga.tm.command

import org.kapunga.tm.soul.Agent
import org.kapunga.tm.world.Room

/**
 * This class is meant to encapsulate a command to make it easily extensible.  At the moment it is unimplemented
 * as we are going with a bare bones CommandExecutor while we get basic functionality working.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
class Command {

}

/**
 * CommandContext represents everything a command might possibly need to execute.  Currently this contains
 * the Agent executing the command and the room the command is executed in.  It offers valid targets for
 * actions, as well as context specific available commands, such as directions that lead out of the room
 * the agent is in.
 *
 * @param executor A reference to the agent that is executing the command.
 * @param room The room the agent is currently in.
 */
case class CommandContext(executor: Agent, room: Room)
