package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.CurrentSituationAgent;

public class PerformedActionRequestMessage extends Message<CurrentSituationAgent> {
  public PerformedActionRequestMessage(final CurrentSituationAgent sender) {
    super(sender);
  }
}
