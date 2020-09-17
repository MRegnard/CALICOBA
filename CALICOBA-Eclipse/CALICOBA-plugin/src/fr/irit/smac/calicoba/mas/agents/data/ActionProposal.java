package fr.irit.smac.calicoba.mas.agents.data;

import fr.irit.smac.calicoba.mas.agents.SituationAgent;
import fr.irit.smac.util.ValueMap;

public class ActionProposal {
  private final SituationAgent proposer;
  private final ValueMap actions;
  private final ValueMap expectedCriticalitiesVariations;

  public ActionProposal(final SituationAgent proposer, final ValueMap actions,
      final ValueMap expectedCriticalitiesVariations) {
    this.proposer = proposer;
    this.actions = actions.clone();
    this.expectedCriticalitiesVariations = expectedCriticalitiesVariations.clone();
  }

  public SituationAgent getProposer() {
    return this.proposer;
  }

  public ValueMap getActions() {
    return this.actions.clone();
  }

  public ValueMap getExpectedCriticalitiesVariations() {
    return this.expectedCriticalitiesVariations.clone();
  }
}
