package fr.irit.smac.calicoba.mas.agents.messages;

import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;

public class OscillationDetectedMessage extends Message<ObjectiveAgent> {
  public OscillationDetectedMessage(ObjectiveAgent sender) {
    super(sender);
  }

  @Override
  public String getSenderName() {
    return this.getSender().getName();
  }
}
