package fr.irit.smac.calicoba.mas.agents;

import fr.irit.smac.calicoba.ReadableAgentAttribute;

/**
 * A Measure entity reads the value of an output of the target model then makes
 * it available to other agents.
 *
 * @author Damien Vergnet
 */
public class MeasureEntity extends AgentWithGamaAttribute<ReadableAgentAttribute<Double>> {
  /**
   * Creates a new Measure entity.
   *
   * @param attribute The attribute this entity can read.
   */
  public MeasureEntity(ReadableAgentAttribute<Double> attribute) {
    super(attribute);
  }
}
