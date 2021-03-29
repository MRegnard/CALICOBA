package fr.irit.smac.calicoba.gaml;

import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProviderSetter;
import msi.gama.metamodel.agent.IAgent;

/**
 * Implementation of {@link IValueProvider} for GAMA agents that allows
 * attribute modification.
 * 
 * @author Damien Vergnet
 *
 * @param <T> Type of agent’s attribute.
 */
public class WritableGamaValueProvider<T> extends GamaValueProvider<T> implements IValueProviderSetter<T> {
  /**
   * Creates a writable provider for the given GAMA agent.
   * 
   * @param agent         The GAMA agent.
   * @param attributeName The attribute name.
   */
  public WritableGamaValueProvider(IAgent agent, String attributeName) {
    super(agent, attributeName);
  }

  @Override
  public void setValue(T value) {
    this.agent.setAttribute(this.attributeName, value);
  }
}
