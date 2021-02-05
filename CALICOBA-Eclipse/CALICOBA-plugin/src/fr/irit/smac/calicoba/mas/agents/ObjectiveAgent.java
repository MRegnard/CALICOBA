package fr.irit.smac.calicoba.mas.agents;

import java.util.Map;

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
  /** The criticality during the previous step. */
  private double previousCriticality;

  /** The all-time minimum absolute criticality. */
  private double minDiff;
  /** The all-time maximum absolute criticality. */
  private double maxDiff;

  /** Sensitivities of each parameter on this objective. */
  private Map<ParameterAgent, Double> sensitivities;

  /**
   * Creates a new Objective agent with the given name.
   *
   * @param name This objective’s name.
   */
  public ObjectiveAgent(final String name, MeasureEntity measureAgent, ObservationEntity observationAgent) {
    this.name = name;
    this.measureAgent = measureAgent;
    this.observationAgent = observationAgent;
    this.minDiff = 0; // TEMP
    this.maxDiff = 30; // TEMP
  }

  public Map<ParameterAgent, Double> getSensitivities() {
    return this.sensitivities;
  }

  public void setInitialSensitivities(Map<ParameterAgent, Double> sensitivities) {
    this.sensitivities = sensitivities;
  }

  @Override
  public void perceive() {
    super.perceive();
    this.observationValue = this.observationAgent.getAttributeValue();
    this.measureValue = this.measureAgent.getAttributeValue();
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();
    this.previousCriticality = this.criticality;
    this.criticality = this.computeCriticality(this.measureValue);
    Logger.debug("criticality (" + this.getName() + "): " + this.criticality);

    // Estimate participation on variation of each parameter (sensitivity / (sum of
    // sensitivities))
    double sum = this.sensitivities.values().stream().mapToDouble(Math::abs).sum();
    // FIXME attention, si action passée = 0 => participation nulle
    this.getWorld().getAgentsForType(ParameterAgent.class)
        .forEach(pa -> pa.updateEstimatedVariationParticipation(this, Math.abs(this.sensitivities.get(pa)) / sum));
  }

  /**
   * Computes the criticality as the absolute value of the distance between the
   * measure and the observation. The result is normalized using the all-time min
   * and max difference values.
   * 
   * @param measureValue The measure value to use in the calculation.
   * @return The corresponding criticality.
   */
  private double computeCriticality(double measureValue) {
    double diff = Math.abs(measureValue - this.observationValue);

//      Logger.debug("measureValue (" + this.getName() + "): " + measureValue);
//      Logger.debug("observationValue (" + this.getName() + "): " + this.observationValue);
//      if (Double.isNaN(this.minDiff)) {
//        this.minDiff = diff;
//      } else {
//        this.minDiff = Math.min(diff, this.minDiff);
//      }

//      if (Double.isNaN(this.maxDiff)) {
//        this.maxDiff = diff;
//      } else {
//        this.maxDiff = Math.max(diff, this.maxDiff);
//      }
//      Logger.debug("maxDiff (" + this.getName() + "): " + this.maxDiff + "; minDiff: " + this.minDiff);
//      Logger.debug("maxDiff - minDiff (" + this.getName() + "): " + (this.maxDiff - this.minDiff));

    double minMaxDiff = this.maxDiff - this.minDiff;
    double criticality;
    if (Math.abs(minMaxDiff) < 1e-6) {
      criticality = diff != 0 ? 1 : 0;
    } else {
      criticality = (diff - this.minDiff) / minMaxDiff;
    }

    return criticality;
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

  public double getPreviousCriticality() {
    return this.previousCriticality;
  }

  public boolean isAboveSetpoint() {
    return this.measureValue > this.observationValue;
  }

  public boolean isBelowSetpoint() {
    return this.measureValue < this.observationValue;
  }

  @Override
  public String toString() {
    return String.format("obj_%s(%f)", this.getName(), this.criticality);
  }
}
