package fr.irit.smac.calicoba.mas.agents;

import java.util.LinkedList;
import java.util.List;

import fr.irit.smac.calicoba.mas.messages.ActionProposalMessage;
import fr.irit.smac.calicoba.mas.messages.ActionProposalRequestMessage;
import fr.irit.smac.calicoba.mas.messages.Message;
import fr.irit.smac.util.ValueMap;

public class SituationAgent extends Agent<SituationAgent.State> {
  private ModelState modelState;
  private ValueMap parametersActions;
  private ValueMap expectedCriticalitiesVariations;
  private List<Agent<?>> requesters;

  public SituationAgent(ModelState modelState, ValueMap parametersActions,
      ValueMap expectedCriticalitiesVariations) {
    super();
    this.modelState = modelState.clone();
    this.parametersActions = parametersActions.clone();
    this.expectedCriticalitiesVariations = expectedCriticalitiesVariations.clone();
    this.requesters = new LinkedList<>();
  }

  public ModelState getModelState() {
    return this.modelState.clone();
  }

  @Override
  public void perceive() {
    super.perceive();

    this.requesters.clear();

    Message<?> m;

    while ((m = this.getMessage()) != null) {
      if (m instanceof ActionProposalRequestMessage) {
        this.setState(State.PROPOSAL_REQUESTED);
        this.requesters.add(m.getSender());
      }
    }
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    if (this.hasState(State.PROPOSAL_REQUESTED)) {
      this.requesters.forEach(r -> r.onMessage(
          new ActionProposalMessage(this, this.parametersActions, this.expectedCriticalitiesVariations)));
      this.setState(State.IDLE);
    }
  }

  public enum State {
    IDLE, PROPOSAL_REQUESTED;
  }
}
