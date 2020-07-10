package fr.irit.smac.calicoba;

import msi.gama.metamodel.agent.IAgent;

/**
 * This class represents a float attribute of a GAMA agent whose value can be
 * read and set.
 *
 * @author Damien Vergnet
 */
public class WritableAgentAttribute extends ReadableAgentAttribute {
  /**
   * Creates a writable attribute for the given agent.
   *
   * @param agent The agent whose attribute this object represents.
   * @param name  The name of the attribute represented by this object.
   */
  public WritableAgentAttribute(IAgent agent, String name) {
    super(agent, name);
  }

  /**
   * Sets the value of this attribute.
   *
   * @param value The new value.
   */
  public void setValue(double value) {
    this.agent.setAttribute(this.getName(), value);
  }

  @Override
  public String toString() {
    return "W" + super.toString();
  }
}
