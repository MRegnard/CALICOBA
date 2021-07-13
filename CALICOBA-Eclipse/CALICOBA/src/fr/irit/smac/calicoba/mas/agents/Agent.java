package fr.irit.smac.calicoba.mas.agents;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.messages.Message;

/**
 * Base class for all agents.
 *
 * @author Damien Vergnet
 */
public abstract class Agent {
  private Calicoba world;
  private String id;

  /** The set of received variation requests. */
  protected final Set<Message<? extends Agent>> messages;

  public Agent() {
    this.messages = new HashSet<>();
  }

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
   * This method is called whenever this agent receives a message.
   * 
   * @param message The message.
   */
  public void onMessage(Message<? extends Agent> message) {
    this.messages.add(message);
  }

  /**
   * Fetches all messages for the given type.
   * 
   * @param <T>         Messages’ type.
   * @param messageType The class to check for.
   * @return All messages for the type.
   */
  public <T extends Message<? extends Agent>> Set<T> getMessageForType(Class<T> messageType) {
    return this.messages.stream() //
        .filter(m -> messageType.isInstance(m)) //
        .map(m -> messageType.cast(m)) //
        .collect(Collectors.toSet());
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
