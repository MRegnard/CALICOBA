package fr.irit.smac.calicoba.mas.agents;

import java.util.Collections;
import java.util.Map;

/**
 * A parameter agent’s entry is composed of an action and the variations of all
 * objectives after said action was performed.
 * 
 * @author Damien Vergnet
 */
public class ParameterAgentMemoryEntry {
  /** The performed action. */
  private final double action;
  /** The variations of all objectives after the action was performed. */
  private final Map<String, Double> objectivesVariations;

  /**
   * Creates a new memory entry.
   * 
   * @param action               The performed action.
   * @param objectivesVariations The variations of the objectives after the action
   *                             was performed.
   */
  public ParameterAgentMemoryEntry(double action, Map<String, Double> objectivesVariations) {
    this.action = action;
    this.objectivesVariations = Collections.unmodifiableMap(objectivesVariations);
  }

  /**
   * @return The action.
   */
  public double getAction() {
    return this.action;
  }

  /**
   * @return The variations of all objectives after the action was performed.
   */
  public Map<String, Double> getObjectivesVariations() {
    return this.objectivesVariations;
  }
}
