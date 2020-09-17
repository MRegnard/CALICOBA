package fr.irit.smac.calicoba.mas.agents;

import fr.irit.smac.calicoba.mas.agents.data.ActionProposal;
import fr.irit.smac.calicoba.mas.agents.data.ModelState;
import fr.irit.smac.util.ValueMap;

public class SituationAgent extends Agent {
  private ModelState modelState;
  private ValueMap parametersActions;
  private ValueMap expectedCriticalitiesVariations;
  private ValueMap actualCriticalitiesVariations;
  private PropositionState propositionState;

  public SituationAgent(ModelState modelState, ValueMap parametersActions, ValueMap expectedCriticalitiesVariations) {
    super();
    this.modelState = modelState.clone();
    this.parametersActions = parametersActions.clone();
    this.expectedCriticalitiesVariations = expectedCriticalitiesVariations.clone();
    this.propositionState = PropositionState.NO_PROPOSAL;
  }

  public ModelState getModelState() {
    return this.modelState.clone();
  }

  public boolean isChosen() {
    return this.propositionState == PropositionState.CHOSEN;
  }

  public void setChosen(boolean chosen) {
    this.propositionState = chosen ? PropositionState.CHOSEN : PropositionState.NO_PROPOSAL;
  }

  public boolean isRejected() {
    return this.propositionState == PropositionState.REJECTED;
  }

  public void setRejected(boolean rejected) {
    this.propositionState = rejected ? PropositionState.REJECTED : PropositionState.NO_PROPOSAL;
  }

  public ActionProposal getActionProposal() {
    return new ActionProposal(this, this.parametersActions, this.expectedCriticalitiesVariations);
  }

  @Override
  public void perceive() {
    super.perceive();

    if (this.isChosen()) {
      this.actualCriticalitiesVariations = this.getMediator().getCriticalitiesVariations();
    }
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    if (this.isChosen()) {
      // TODO corriger les prévisions
      this.setChosen(false);
    } else if (this.isRejected()) {
      // TODO corriger la plage de validité
      this.setRejected(false);
    }
  }

  private enum PropositionState {
    NO_PROPOSAL, CHOSEN, REJECTED;
  }
}
