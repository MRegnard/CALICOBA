package fr.irit.smac.calicoba.mas.agents;

import fr.irit.smac.calicoba.mas.Calicoba;

/**
 * Base class for all agents.
 *
 * @author Damien Vergnet
 */
public abstract class Agent {
  private Calicoba world;
  private String id;

  /**
   * @return The world this agent is in.
   */
  public Calicoba getWorld() {
    return this.world;
  }

  /**
   * Sets the world for this agent.
   *
   * @param world The world.
   * @note This method should ONLY be called from
   *       {@link Calicoba#addAgent(Agent)}.
   */
  public void setWorld(Calicoba world) {
    this.world = world;
  }

  /**
   * @return This agent’s ID.
   */
  public String getId() {
    return this.id;
  }

  /**
   * Sets the ID for this agent.
   *
   * @param id This agent’s ID.
   * @note This method should ONLY be called from
   *       {@link Calicoba#addAgent(Agent)}.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Agents should implement this method to perceive their environment (read
   * received messages, gather data from their sensors, etc.).
   */
  public void perceive() {
  }

  /**
   * Agents should implement this method to decide which action they are going to
   * perform on their environment then perform it (send messages, act on the
   * environment, etc.).
   */
  public void decideAndAct() {
  }

  @Override
  public String toString() {
    return String.format("agent %s", this.getId());
  }
}
