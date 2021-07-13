package fr.irit.smac.calicoba.mas.agents.phases;

import fr.irit.smac.calicoba.mas.agents.messages.CriticalityMessage;

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
   * @param worldCycle Current simulation step.
   */
  public void update(CriticalityMessage request, double agentValue, int worldCycle) {
    // TEMP
//    if (this.currentPhase == null || !this.currentPhase.getDirection().equals(request.direction)) {
//      this.previousPreviousPhase = this.previousPhase;
//      this.previousPhase = this.currentPhase;
//      this.currentPhase = new Phase(request.direction);
//    }
    this.currentPhase.update(request.criticality, agentValue, worldCycle);
  }

  /**
   * Estimates the wait delay for actions based on the 3 phases.
   * 
   * @return The estimated delay.
   */
  public int estimateDelay() {
    if (this.previousPreviousPhase == null || this.previousPhase == null) {
      return 0;
    }
    return this.previousPhase.getStepForMostExtremeCriticality()
        - this.previousPreviousPhase.getStepForMostExtremeValue();
  }
}
