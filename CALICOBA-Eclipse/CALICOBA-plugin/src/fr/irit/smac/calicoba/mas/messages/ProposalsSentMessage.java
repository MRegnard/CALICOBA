package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.MediatorAgent;

public class ProposalsSentMessage extends Message<MediatorAgent> {
  public ProposalsSentMessage(final MediatorAgent sender) {
    super(sender);
  }
}
