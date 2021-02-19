package fr.irit.smac.calicoba.mas.agents.phases;

/**
 * A phase step represents an agent’s criticality and another agent’s value at a
 * given time.
 * 
 * @author Damien Vergnet
 */
public class PhaseStep {
  /** Criticality of the satisfaction agent that sent the variation request. */
  public final double senderCriticality;
  /** Value of the agent that created this step. */
  public final double value;

  /**
   * Creates a new step.
   * 
   * @param senderCriticality Criticality of the satisfaction agent that sent the
   *                          variation request.
   * @param value             Value of the agent that created this step.
   */
  public PhaseStep(double senderCriticality, double value) {
    this.senderCriticality = senderCriticality;
    this.value = value;
  }
}
