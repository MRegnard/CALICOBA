package fr.irit.smac.calicoba.mas.agents;

import fr.irit.smac.calicoba.ReadableAgentAttribute;

/**
 * An Observation entity reads the value of an output of the reference system to
 * make it available to other agents.
 *
 * @author Damien Vergnet
 */
public class ObservationEntity extends AgentWithGamaAttribute<ReadableAgentAttribute<Double>> {
  /**
   * Creates a new Observation entity.
   *
   * @param attribute The attribute this entity can read.
   */
  public ObservationEntity(ReadableAgentAttribute<Double> attribute) {
    super(attribute);
  }
}
