package fr.irit.smac.calicoba.mas.agents;

import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;

/**
 * This type of agent represents a float output of the target model.
 * 
 * @author Damien Vergnet
 */
public class OutputAgent
    extends AgentWithGamaAttribute<ReadableModelAttribute<Double, IValueProvider<Double>>, IValueProvider<Double>> {

  /**
   * Creates new a output agent for a given model output.
   * 
   * @param attribute The associated model attribute.
   */
  public OutputAgent(final ReadableModelAttribute<Double, IValueProvider<Double>> attribute) {
    super(attribute);
  }
}
