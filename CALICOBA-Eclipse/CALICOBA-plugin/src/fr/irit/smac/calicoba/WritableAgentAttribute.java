package fr.irit.smac.calicoba;

import msi.gama.metamodel.agent.IAgent;

/**
 * This class represents an attribute of a GAMA agent whose value can be read
 * and set.
 * 
 * @author Damien Vergnet
 */
public class WritableAgentAttribute<T> extends ReadableAgentAttribute<T> {
  /**
   * Creates a writable attribute for the given agent.
   * 
   * @param agent The agent whose attribute this object represents.
   * @param name  The name of the attribute represented by this object.
   * @param type  The type of the attribute.
   */
  public WritableAgentAttribute(IAgent agent, String name, Class<T> type) {
    super(agent, name, type);
  }

  /**
   * Sets the value of this attribute.
   * 
   * @param value The new value.
   */
  public void setValue(T value) {
    this.agent.setAttribute(this.getName(), value);
  }

  @Override
  public String toString() {
    return "W" + super.toString();
  }
}
