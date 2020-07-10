package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.SituationAgent;
import fr.irit.smac.util.ValueMap;

public class ActionProposalMessage extends Message<SituationAgent> {
  private final ValueMap actions;
  private final ValueMap expectedObjectivesVariations;

  public ActionProposalMessage(final SituationAgent sender, final ValueMap actions,
      final ValueMap expectedObjectivesVariations) {
    super(sender);
    this.actions = actions.clone();
    this.expectedObjectivesVariations = expectedObjectivesVariations.clone();
  }

  public ValueMap getActions() {
    return this.actions.clone();
  }

  public ValueMap getExpectedObjectivesVariations() {
    return this.expectedObjectivesVariations.clone();
  }
}
