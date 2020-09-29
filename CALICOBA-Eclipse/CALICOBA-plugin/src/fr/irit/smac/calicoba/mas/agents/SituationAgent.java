package fr.irit.smac.calicoba.mas.agents;

import java.util.List;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.data.ActionProposal;
import fr.irit.smac.calicoba.mas.agents.data.ModelState;
import fr.irit.smac.util.ValueMap;

public class SituationAgent extends Agent {
  private ModelState modelState;

  private ValueMap parametersActions;

  private ValueMap expectedCriticalitiesVariations;
  private ValueMap actualCriticalitiesVariations;

  private boolean selected;
  private boolean chosen;
  private boolean rejected;

  private List<ParameterAgent> parameters;

  public SituationAgent(ModelState modelState, ValueMap parametersActions, ValueMap objectiveVariations) {
    this.modelState = modelState;
    this.parametersActions = parametersActions;
    this.expectedCriticalitiesVariations = objectiveVariations;
  }

  public ModelState getModelState() {
    return this.modelState.clone();
  }

  public boolean isSelected() {
    return this.selected;
  }

  public void select() {
    this.selected = true;
  }

  public boolean isChosen() {
    return this.selected && this.chosen;
  }

  public void choose() {
    this.chosen = true;
    this.rejected = false;
  }

  public boolean isRejected() {
    return this.selected && this.rejected;
  }

  public void reject() {
    this.rejected = true;
    this.chosen = false;
  }

  @Override
  public void perceive() {
    super.perceive();

    this.parameters = this.getWorld().getAgentsForType(ParameterAgent.class);

    if (this.isChosen()) {
      this.actualCriticalitiesVariations = Calicoba.instance().getCurrentSituation().getCriticalitiesVariations();
    }
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

//    if (this.isChosen()) {
//      // TODO corriger les prévisions
//      this.chosen = false;
//      this.selected = false;
//    } else if (this.isRejected()) {
//      // TODO ?
//      this.selected = false;
//    } else
    if (this.isSelected()) {
      ActionProposal proposal = new ActionProposal(this, this.parametersActions, this.expectedCriticalitiesVariations);
      this.parameters.forEach(p -> p.suggestAction(proposal));
      this.selected = false;
    }
  }

  @Override
  public String toString() {
    return String.format("SituationAgent{modelState=%s,vars=%s}", this.modelState,
        this.expectedCriticalitiesVariations);
  }
}
