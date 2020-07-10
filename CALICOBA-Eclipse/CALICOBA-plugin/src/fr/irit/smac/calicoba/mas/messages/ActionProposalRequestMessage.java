package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.CurrentSituationAgent;

public class ActionProposalRequestMessage extends Message<CurrentSituationAgent> {
  public ActionProposalRequestMessage(final CurrentSituationAgent sender) {
    super(sender);
  }
}
