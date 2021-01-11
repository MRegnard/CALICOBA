package fr.irit.smac.calicoba.mas.agents;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.mas.agents.data.ActionProposal;
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

  private double estimatesPrecision;

  private List<ActionProposal> actionProposals;

  /** Sensitivities of each parameter on this objective. */
  private Map<ParameterAgent, Double> sensitivities;

  private boolean computedCriticality;

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
    this.actionProposals = new LinkedList<>();
    this.computedCriticality = false;
  }

  public Map<ParameterAgent, Double> getSensitivities() {
    return this.sensitivities;
  }

  public void setInitialSensitivities(Map<ParameterAgent, Double> sensitivities) {
    this.sensitivities = sensitivities;
  }

  public void onActionProposal(ActionProposal ap) {
    this.actionProposals.add(ap);
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
    if (!this.computedCriticality) {
      this.criticality = this.computeCriticality(this.measureValue, true);
      this.computedCriticality = true;
      Logger.debug("criticality (" + this.getName() + "): " + this.criticality);
    } else {
      Map<Double, NavigableSet<ParameterAgent>> expectedCriticalities = new HashMap<>();

      // Avoid order bias
      Collections.shuffle(this.actionProposals);

      this.actionProposals.forEach(ap -> {
        ParameterAgent p = ap.getProposer();
        double expectedCriticality = ap.getExpectedCriticalities().get(this.name);
        double epislon = 1e-6;

        // Criticalities that are under estimatesPrecision away from a key are
        // considered the same
        Map<Double, Double> oldToNewKeys = expectedCriticalities.keySet().stream()
            // Keep all keys that are close to the expected criticality
            .filter(k -> Math.abs(k - expectedCriticality) <= this.estimatesPrecision + epislon)
            // Compute new keys (mean of current value and expected criticality)
            .collect(Collectors.toMap(Function.identity(), k -> (k + expectedCriticality) / 2));
        Logger.debug("oldToNewKeys (" + this.getName() + "): " + oldToNewKeys);

        if (oldToNewKeys.isEmpty()) {
          // Using TreeSet to keep agents sorted by decreasing criticality estimates
          NavigableSet<ParameterAgent> set = new TreeSet<>(
              (p1, p2) -> Double.compare(Math.abs(this.sensitivities.get(p2)), Math.abs(this.sensitivities.get(p1))));
          set.add(p);
          expectedCriticalities.put(expectedCriticality, set);

        } else {
          oldToNewKeys.entrySet().stream().forEach(e -> {
            double oldKey = e.getKey();
            double newKey = e.getValue();
            NavigableSet<ParameterAgent> agents = expectedCriticalities.remove(oldKey);
            agents.add(p);
            expectedCriticalities.put(newKey, agents);
          });
        }
      });

      // Reject all but the most sensitive parameter (first) in each list
      expectedCriticalities.values().forEach(agents -> {
        agents.first().onActionProposalConfirmation();
        agents.stream().skip(1).forEach(ParameterAgent::onActionProposalRejection);
      });

      this.actionProposals.clear();
      this.computedCriticality = false;
    }
  }

  /**
   * Computes the criticality as the absolute value of the distance between the
   * measure and the observation. The result is normalized using the all-time min
   * and max difference values.
   * 
   * @param measureValue The measure value to use in the calculation.
   * @param updateMinMax If true, minDiff and maxDiff will be updated.
   * @return The corresponding criticality.
   */
  private double computeCriticality(double measureValue, boolean updateMinMax) {
    double diff = Math.abs(measureValue - this.observationValue);

    if (updateMinMax) {
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
      this.estimatesPrecision = 1.0 / (this.maxDiff - this.minDiff);
      Logger.debug("precision (" + this.getName() + "): " + this.estimatesPrecision);
      Logger.debug("diff (" + this.getName() + "): " + diff);
//      Logger.debug("maxDiff (" + this.getName() + "): " + this.maxDiff + "; minDiff: " + this.minDiff);
//      Logger.debug("maxDiff - minDiff (" + this.getName() + "): " + (this.maxDiff - this.minDiff));
    }

    double minMaxDiff = this.maxDiff - this.minDiff;
    double criticality;
    if (minMaxDiff == 0) {
      criticality = diff != 0 ? 1 : 0;
    } else {
      criticality = (diff - this.minDiff) / minMaxDiff;
    }

    return criticality;
  }

  /**
   * Estimates the criticality of this agent for the given measure variation.
   * Min/Max nor the criticality will be updated after calling this method.
   * 
   * @param outputVariation The hypothetical variation of the current measure
   *                        value.
   * @return The estimated criticality.
   */
  public double estimateCriticality(double outputVariation) {
    return this.computeCriticality(this.measureValue + outputVariation, false);
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
