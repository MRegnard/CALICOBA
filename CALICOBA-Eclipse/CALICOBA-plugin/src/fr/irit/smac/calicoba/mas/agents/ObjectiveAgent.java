package fr.irit.smac.calicoba.mas.agents;

import fr.irit.smac.util.Logger;

/**
 * An Objective agent gets a Measure and Observation then computes a criticality
 * value and finally sends it to all Parameter agents.
 *
 * @author Damien Vergnet
 */
public class ObjectiveAgent extends Agent {
  private final String name;

  private MeasureEntity measureAgent;
  private ObservationEntity observationAgent;
  /** The current observation value. */
  private double observationValue;
  /** The current measure value. */
  private double measureValue;
  /** The criticality. */
  private double criticality;

  /** The all-time minimum absolute criticality. */
  private double minDiff;
  /** The all-time maximum absolute criticality. */
  private double maxDiff;

  /**
   * Creates a new Objective agent with the given name.
   *
   * @param name This objective’s name.
   */
  public ObjectiveAgent(final String name, MeasureEntity measureAgent, ObservationEntity observationAgent) {
    this.name = name;
    this.measureAgent = measureAgent;
    this.observationAgent = observationAgent;
    this.minDiff = Double.NaN;
    this.maxDiff = Double.NaN;
  }

  /**
   * Reads the value of the measure and the observation then stores it for use in
   * the act step.
   */
  @Override
  public void perceive() {
    super.perceive();
    this.observationValue = this.observationAgent.getAttributeValue();
    this.measureValue = this.measureAgent.getAttributeValue();
  }

  /**
   * Computes the criticality then sends it to all Parameter agents.
   */
  @Override
  public void decideAndAct() {
    super.decideAndAct();
    this.computeCriticality();
  }

  /**
   * Computes the criticality as the absolute value of the distance between the
   * measure and the observation.
   */
  private void computeCriticality() {
    double diff = Math.abs(this.measureValue - this.observationValue);

    if (Double.isNaN(this.minDiff)) {
      this.minDiff = diff;
    } else {
      this.minDiff = Math.min(diff, this.minDiff);
    }

    if (Double.isNaN(this.maxDiff)) {
      this.maxDiff = diff;
    } else {
      this.maxDiff = Math.max(diff, this.maxDiff);
    }

    Logger.debug(diff);
    Logger.debug(this.maxDiff + " " + this.minDiff);
    Logger.debug(this.maxDiff - this.minDiff);
    double minMaxDiff = this.maxDiff - this.minDiff; // TODO pas bon
    if (minMaxDiff == 0) {
      this.criticality = diff;
    } else {
      this.criticality = diff / minMaxDiff;
    }
    Logger.debug(this.criticality);
  }

  /**
   * @return The name of this agent.
   */
  public String getName() {
    return this.name;
  }

  public double getCriticality() {
    return this.criticality;
  }

  @Override
  public String toString() {
    return String.format("obj_%s(%f)", this.getName(), this.criticality);
  }
}
