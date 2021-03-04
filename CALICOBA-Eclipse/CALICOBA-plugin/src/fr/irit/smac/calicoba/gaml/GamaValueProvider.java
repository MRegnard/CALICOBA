package fr.irit.smac.calicoba.gaml;

import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import msi.gama.metamodel.agent.IAgent;

/**
 * Implementation of {@link IValueProvider} for GAMA agents that only allows
 * attribute value reading.
 * 
 * @author Damien Vergnet
 *
 * @param <T> Type of agent’s attribute.
 */
public class GamaValueProvider<T> implements IValueProvider<T> {
  protected final IAgent agent;
  protected final String attributeName;

  /**
   * Creates a provider for the given GAMA agent.
   * 
   * @param agent         The GAMA agent.
   * @param attributeName The attribute name.
   */
  public GamaValueProvider(IAgent agent, String attributeName) {
    this.agent = agent;
    this.attributeName = attributeName;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T getValue() {
    return (T) this.agent.getAttribute(this.attributeName);
  }
}
