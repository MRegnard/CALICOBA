package fr.irit.smac.calicoba.mas.agents;

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
  private double minAbsoluteCriticality;
  /** The all-time maximum absolute criticality. */
  private double maxAbsoluteCriticality;

  /**
   * Creates a new Objective agent with the given name.
   *
   * @param name This objective’s name.
   */
  public ObjectiveAgent(final String name, MeasureEntity measureAgent, ObservationEntity observationAgent) {
    this.name = name;
    this.measureAgent = measureAgent;
    this.observationAgent = observationAgent;
    this.minAbsoluteCriticality = Double.NaN;
    this.maxAbsoluteCriticality = Double.NaN;
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
    double absoluteCriticality = Math.abs(this.measureValue - this.observationValue);

    if (Double.isNaN(this.minAbsoluteCriticality)) {
      this.minAbsoluteCriticality = absoluteCriticality;
    } else {
      this.minAbsoluteCriticality = Math.min(absoluteCriticality, this.minAbsoluteCriticality);
    }

    if (Double.isNaN(this.maxAbsoluteCriticality)) {
      this.maxAbsoluteCriticality = absoluteCriticality;
    } else {
      this.maxAbsoluteCriticality = Math.max(absoluteCriticality, this.maxAbsoluteCriticality);
    }

    // DEBUG
//      Logger.debug(absoluteCriticality);
//      Logger.debug(this.maxAbsoluteCriticality + " " + this.minAbsoluteCriticality);
//      Logger.debug(this.maxAbsoluteCriticality - this.minAbsoluteCriticality);
    double diff = this.maxAbsoluteCriticality - this.minAbsoluteCriticality;
    if (diff == 0) {
      this.criticality = absoluteCriticality;
    } else {
      this.criticality = absoluteCriticality / diff;
    }
    // DEBUG
//      Logger.debug(this.criticality);
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
}
