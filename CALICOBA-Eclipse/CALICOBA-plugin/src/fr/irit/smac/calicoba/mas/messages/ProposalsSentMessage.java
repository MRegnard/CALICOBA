package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.CurrentSituationAgent;

public class ProposalsSentMessage extends Message<CurrentSituationAgent> {
  public ProposalsSentMessage(final CurrentSituationAgent sender) {
    super(sender);
  }
}
