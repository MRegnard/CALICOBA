package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.MediatorAgent;

public class ActionProposalRequestMessage extends Message<MediatorAgent> {
  public ActionProposalRequestMessage(final MediatorAgent sender) {
    super(sender);
  }
}
