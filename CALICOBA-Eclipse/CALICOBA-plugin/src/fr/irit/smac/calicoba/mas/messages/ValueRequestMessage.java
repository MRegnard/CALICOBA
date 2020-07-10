package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.Agent;

public class ValueRequestMessage extends Message<Agent<?>> {
  public ValueRequestMessage(final Agent<?> sender) {
    super(sender);
  }
}
