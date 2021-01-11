package fr.irit.smac.calicoba.mas.agents.data;

import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.util.ValueMap;

public class ActionProposal {
  private final ParameterAgent proposer;
  private final double action;
  private final ValueMap expectedCriticalities;

  public ActionProposal(final ParameterAgent proposer, final double action, final ValueMap expectedCriticalities) {
    this.proposer = proposer;
    this.action = action;
    this.expectedCriticalities = expectedCriticalities.clone();
  }

  public ParameterAgent getProposer() {
    return this.proposer;
  }

  public double getAction() {
    return this.action;
  }

  public ValueMap getExpectedCriticalities() {
    return this.expectedCriticalities.clone();
  }

  @Override
  public String toString() {
    return String.format("ActionProposal[action=%f,proposer=%s,criticalities=%s]", this.action, this.proposer,
        this.expectedCriticalities);
  }
}
