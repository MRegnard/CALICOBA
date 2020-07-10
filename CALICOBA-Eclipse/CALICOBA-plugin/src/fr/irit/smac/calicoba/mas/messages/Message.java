package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.Agent;

/**
 * Base class for messages exchanged between agents.
 *
 * @author Damien Vergnet
 */
public abstract class Message<T extends Agent<?>> {
  /** The agent that sent this message. */
  private final T sender;

  /**
   * Creates a new message with given sender.
   *
   * @param sender The agent that sent this message.
   */
  public Message(final T sender) {
    this.sender = sender;
  }

  /**
   * Returns the agent that sent this message.
   */
  public T getSender() {
    return this.sender;
  }
}
