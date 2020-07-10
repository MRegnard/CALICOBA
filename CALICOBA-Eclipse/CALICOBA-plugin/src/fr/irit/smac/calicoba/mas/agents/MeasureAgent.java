package fr.irit.smac.calicoba.mas.agents;

import fr.irit.smac.calicoba.ReadableAgentAttribute;
import fr.irit.smac.calicoba.mas.messages.FloatValueMessage;

/**
 * A Measure agent reads the value of an output of the target model then sends
 * it to the corresponding Objective agent and all Parameter agents.
 *
 * @author Damien Vergnet
 */
public class MeasureAgent extends ValueProviderAgent {
  /**
   * Creates a new Measure agent.
   *
   * @param attribute The attribute this agent can read.
   */
  public MeasureAgent(ReadableAgentAttribute attribute) {
    super(attribute, FloatValueMessage.ValueNature.MEASURE_VALUE);
  }
}
