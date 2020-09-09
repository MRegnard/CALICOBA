package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.SituationAgent;
import fr.irit.smac.util.ValueMap;

public class ActionProposalMessage extends Message<SituationAgent> {
  private final ValueMap actions;
  private final ValueMap expectedCriticalitiesVariations;

  public ActionProposalMessage(final SituationAgent sender, final ValueMap actions,
      final ValueMap expectedCriticalitiesVariations) {
    super(sender);
    this.actions = actions.clone();
    this.expectedCriticalitiesVariations = expectedCriticalitiesVariations.clone();
  }

  public ValueMap getActions() {
    return this.actions.clone();
  }

  public ValueMap getExpectedCriticalitiesVariations() {
    return this.expectedCriticalitiesVariations.clone();
  }
}
