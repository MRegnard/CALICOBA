package fr.irit.smac.calicoba.mas;

import fr.irit.smac.calicoba.mas.agents.Agent;

/**
 * Base class for messages exchanged between agents.
 * 
 * @author Damien Vergnet
 */
public abstract class Message {
  /** The agent that sent this message. */
  private final Agent sender;

  /**
   * Creates a new message with given sender.
   * 
   * @param sender The agent that sent this message.
   */
  public Message(Agent sender) {
    this.sender = sender;
  }

  /**
   * Returns the agent that sent this message.
   */
  public Agent getSender() {
    return this.sender;
  }
}
