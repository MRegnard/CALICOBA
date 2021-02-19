package fr.irit.smac.calicoba.mas.agents.phases;

import fr.irit.smac.calicoba.mas.agents.data.VariationRequest;

/**
 * This class represents the current and two last phases for a given
 * satisfaction agent.
 * 
 * @author Damien Vergnet
 */
public class ClientRepresentation {
  private Phase currentPhase;
  private Phase previousPhase;
  private Phase previousPreviousPhase;

  /**
   * Adds a new step to this representation. If the request’s direction is
   * different from the current phase’s, all phases are shifted and a new one is
   * created for the new direction.
   * 
   * @param request    The variation request.
   * @param agentValue The value of the parameter agent this representation
   *                   belongs to.
   * @param stepNb     Current simulation step.
   */
  public void update(VariationRequest request, double agentValue, int stepNb) {
    if (this.currentPhase == null || !this.currentPhase.getWay().equals(request.way)) {
      this.previousPreviousPhase = this.previousPhase;
      this.previousPhase = this.currentPhase;
      this.currentPhase = new Phase(request.way);
    }
    this.currentPhase.update(request.criticality, agentValue, stepNb);
  }

  /**
   * Estimates the wait delay for actions based on the 3 phases.
   * 
   * @return The estimated delay.
   */
  public int estimateDelay() {
    if (this.previousPreviousPhase == null || this.previousPhase == null) {
      return 1; // TODO comprendre pourquoi 1 et pas 0
    }
    return this.previousPhase.getStepForMostExtremeCriticality()
        - this.previousPreviousPhase.getStepForMostExtremeValue();
  }
}
