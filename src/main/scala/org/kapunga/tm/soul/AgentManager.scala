package org.kapunga.tm.soul

/**
 * This Object is a system-wide handle to all currently spawned agents.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
object AgentManager {
  private var agents = Set[Agent]()

  /**
   * Add an agent to the registry if it's not already added.  This should only be called by
   * agent.spawn()
   *
   * @param agent The agent we are adding.
   * @return true if the agent wasn't present and is added otherwise false.
   */
  def add(agent: Agent): Boolean = {
    if (agents.contains(agent)) {
      false
    } else {
      agents = agents + agent
      true
    }
  }

  /**
   * Remove an agent from the registry if it is present.  This should only be called by
   * agent.despawn()
   *
   * @param agent The agent we are removing.
   * @return true if the agent is present and removed, otherwise false.
   */
  def remove(agent: Agent): Boolean = {
    if (agents.contains(agent)) {
      agents = agents - agent
      true
    } else {
      false
    }
  }

  /**
   * Gets an option of a specific agent by name.
   * @param name The name of the agent we are looking for.
   * @return An instance of Some containing an agent with the given name if it is present, otherwise None.
   */
  def getAgent(name: String): Option[Agent] = {
    val matches = agents.filter(agent => agent.name == name)

    if (matches.size == 0) {
      None
    } else {
      // There should be only one agent per name, so this SHOULD be safe unless something else is broken.
      Some(matches.toList(0))
    }
  }

  /**
   * @return The set of agents currently registered.
   */
  def getAgents = agents
}
