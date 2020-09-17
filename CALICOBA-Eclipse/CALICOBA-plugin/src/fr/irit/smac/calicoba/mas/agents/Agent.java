package fr.irit.smac.calicoba.mas.agents;

import fr.irit.smac.calicoba.mas.Calicoba;

/**
 * Base class for all agents. Every agent has an ID and a message queue into
 * which it can receive messages from other agents.
 *
 * @author Damien Vergnet
 */
public abstract class Agent {
  private Calicoba world;
  private Mediator mediator;
  private String id;

  /**
   * @return The world this agent is in.
   */
  public Calicoba getWorld() {
    return this.world;
  }

  /**
   * Sets the world this agent is in.
   *
   * @param world The world.
   * @note This method should ONLY be called from
   *       {@link Calicoba#addAgent(Agent)}.
   */
  public void setWorld(Calicoba world) {
    this.world = world;
  }

  public Mediator getMediator() {
    return this.mediator;
  }

  public void setMediator(Mediator mediator) {
    this.mediator = mediator;
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
   * @note This method should ONLY be called from {@link World#addAgent(Agent)}.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Agents should implement this method to perceive their environment (read
   * received messages, gather data from their sensors…).
   */
  public void perceive() {
  }

  /**
   * Agents should implement this method to decide which action they are going to
   * perform on their environment and perform it (send messages, act on the
   * environment…).
   */
  public void decideAndAct() {
  }

  @Override
  public String toString() {
    return String.format("agent %s", this.getId());
  }
}
