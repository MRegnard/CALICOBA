package fr.irit.smac.calicoba.mas.agents;

import java.util.LinkedList;
import java.util.Queue;

import fr.irit.smac.calicoba.mas.Message;
import fr.irit.smac.calicoba.mas.World;

/**
 * Base class for all agents. Every agent has an ID and a message queue into
 * which it can receive messages from other agents.
 * 
 * @author Damien Vergnet
 */
public abstract class Agent {
  private World world;
  private String id;
  private Queue<Message> messages;

  /**
   * Creates a new agent with an empty message queue.
   */
  public Agent() {
    this.messages = new LinkedList<>();
  }

  /**
   * @return The world this agent is in.
   */
  public World getWorld() {
    return this.world;
  }

  /**
   * Sets the world this agent is in.
   * 
   * @param world The world.
   * @note This method should ONLY be called from {@link World#addAgent(Agent)}.
   */
  public void setWorld(World world) {
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

  /**
   * Other agents can call this method to send a message to this agent.
   * 
   * @param message The message.
   */
  public void sendMessage(Message message) {
    this.messages.add(message);
  }

  /**
   * Pops the oldest message in the queue then returns it.
   * 
   * @return The oldest received message.
   */
  protected Message getMessage() {
    return this.messages.poll();
  }

  @Override
  public String toString() {
    return String.format("agent %s", this.getId());
  }
}
