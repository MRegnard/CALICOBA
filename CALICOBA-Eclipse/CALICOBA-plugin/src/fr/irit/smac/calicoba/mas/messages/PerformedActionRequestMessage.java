package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.MediatorAgent;

public class PerformedActionRequestMessage extends Message<MediatorAgent> {
  public PerformedActionRequestMessage(final MediatorAgent sender) {
    super(sender);
  }
}
