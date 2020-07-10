package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.calicoba.mas.agents.SituationAgent;

public class PerformedActionMessage extends Message<ParameterAgent> {
  private final double action;
  private final SituationAgent chosenSituation;

  public PerformedActionMessage(final ParameterAgent sender, final double action,
      final SituationAgent chosenSituation) {
    super(sender);
    this.action = action;
    this.chosenSituation = chosenSituation;
  }

  public double getAction() {
    return this.action;
  }

  public SituationAgent getChosenSituation() {
    return this.chosenSituation;
  }
}
