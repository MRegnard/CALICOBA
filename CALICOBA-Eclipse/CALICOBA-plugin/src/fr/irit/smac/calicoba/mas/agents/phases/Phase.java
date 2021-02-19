package fr.irit.smac.calicoba.mas.agents.phases;

import java.util.HashMap;
import java.util.Map;

import fr.irit.smac.calicoba.mas.agents.data.Way;

/**
 * A phase is a succession of contiguous steps during which the value of a
 * parameter agent varies in the same direction.
 *
 * @author Damien Vergnet
 */
public class Phase {
  /** This phase’s variation direction. */
  private final Way way;
  /** Associate step objects to their step number. */
  private Map<Integer, PhaseStep> steps;
  /** Step when the parameter agent’s value was the most extreme. */
  private int stepForMostExtremeValue;
  /** Step when the satisfaction agent’s criticality was the most extreme. */
  private int stepForMostExtremeCriticality;

  /**
   * Creates a new phase for the given direction.
   * 
   * @param way This phase’s variation direction.
   */
  public Phase(Way way) {
    this.way = way;
    this.steps = new HashMap<>();
  }

  /**
   * @return This phase’s variation direction.
   */
  public Way getWay() {
    return this.way;
  }

  /**
   * Adds a new step to this phase.
   * 
   * @param agentCriticality Criticality of the satisfaction agent.
   * @param agentValue       Value of the parameter agent.
   * @param stepNb           Step number.
   */
  public void update(double agentCriticality, double agentValue, int stepNb) {
    if (this.steps.isEmpty()) {
      this.stepForMostExtremeCriticality = this.stepForMostExtremeValue = stepNb;
    }
    this.steps.put(stepNb, new PhaseStep(agentCriticality, agentValue));
    if (Math.abs(agentValue) >= Math.abs(this.steps.get(this.stepForMostExtremeValue).value)) {
      this.stepForMostExtremeValue = stepNb;
    }
    if (Math.abs(agentCriticality) >= Math.abs(this.steps.get(this.stepForMostExtremeCriticality).senderCriticality)) {
      this.stepForMostExtremeCriticality = stepNb;
    }
  }

  /**
   * @return The step when the parameter agent’s value was the most extreme.
   */
  public int getStepForMostExtremeValue() {
    return this.stepForMostExtremeValue;
  }

  /**
   * @return The step when the satisfaction agent’s criticality was the most
   *         extreme.
   */
  public int getStepForMostExtremeCriticality() {
    return this.stepForMostExtremeCriticality;
  }
}
