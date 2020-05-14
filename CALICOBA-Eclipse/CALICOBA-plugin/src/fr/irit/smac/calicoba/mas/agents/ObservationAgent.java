package fr.irit.smac.calicoba.mas.agents;

import fr.irit.smac.calicoba.ReadableAgentAttribute;

/**
 * An Observation agent reads the value of an output of the reference system
 * then sends it to the corresponding Objective agent.
 * 
 * @author Damien Vergnet
 */
public class ObservationAgent extends ValueReaderAgent {
  /**
   * Creates a new Observation agent.
   * 
   * @param attribute The attribute this agent can read.
   */
  public ObservationAgent(ReadableAgentAttribute<Double> attribute) {
    super(attribute);
  }
}
