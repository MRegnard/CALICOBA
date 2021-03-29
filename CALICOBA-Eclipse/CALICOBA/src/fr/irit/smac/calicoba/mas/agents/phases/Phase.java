package fr.irit.smac.calicoba.mas.agents.phases;

import java.util.HashMap;
import java.util.Map;

import fr.irit.smac.calicoba.mas.agents.data.Direction;

/**
 * A phase is a succession of contiguous steps during which the value of a
 * parameter agent varies in the same direction.
 *
 * @author Damien Vergnet
 */
public class Phase {
  /** This phase’s variation direction. */
  private final Direction direction;
  /** Associate step objects to their step number. */
  private Map<Integer, PhaseStep> steps;
  /** Step when the parameter agent’s value was the most extreme. */
  private int stepForMostExtremeValue;
  /** Step when the satisfaction agent’s criticality was the most extreme. */
  private int stepForMostExtremeCriticality;

  /**
   * Creates a new phase for the given direction.
   * 
   * @param direction This phase’s variation direction.
   */
  public Phase(Direction direction) {
    this.direction = direction;
    this.steps = new HashMap<>();
  }

  /**
   * @return This phase’s variation direction.
   */
  public Direction getDirection() {
    return this.direction;
  }

  /**
   * Adds a new step to this phase.
   * 
   * @param agentCriticality Criticality of the satisfaction agent.
   * @param agentValue       Value of the parameter agent.
   * @param worldCycle       Step number.
   */
  public void update(double agentCriticality, double agentValue, int worldCycle) {
    if (this.steps.isEmpty()) {
      this.stepForMostExtremeCriticality = this.stepForMostExtremeValue = worldCycle;
    } else {
      int lastStep = this.steps.keySet().stream().mapToInt(Integer::intValue).max().getAsInt();
      if (lastStep != worldCycle - 1) {
        throw new IllegalArgumentException(String.format("expected cycle %d, got %d", lastStep, worldCycle));
      }
    }
    this.steps.put(worldCycle, new PhaseStep(agentCriticality, agentValue));
    if (Math.abs(agentValue) >= Math.abs(this.steps.get(this.stepForMostExtremeValue).value)) {
      this.stepForMostExtremeValue = worldCycle;
    }
    if (Math.abs(agentCriticality) >= Math.abs(this.steps.get(this.stepForMostExtremeCriticality).senderCriticality)) {
      this.stepForMostExtremeCriticality = worldCycle;
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
