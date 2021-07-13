package fr.irit.smac.calicoba.mas.agents.messages;

import fr.irit.smac.calicoba.mas.agents.Agent;

public abstract class Message<T extends Agent> {
  private final T sender;

  public Message(final T sender) {
    this.sender = sender;
  }

  public T getSender() {
    return this.sender;
  }

  public abstract String getSenderName();
}
