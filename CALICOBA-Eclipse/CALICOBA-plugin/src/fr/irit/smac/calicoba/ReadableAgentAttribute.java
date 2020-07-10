package fr.irit.smac.calicoba;

import msi.gama.metamodel.agent.IAgent;

/**
 * This class represents a float attribute of a GAMA agent whose value can be
 * read.
 *
 * @author Damien Vergnet
 */
public class ReadableAgentAttribute {
  /** This attribute’s name. */
  private final String name;
  /** The GAMA agent this attribute belongs to. */
  protected final IAgent agent;

  /**
   * Creates a readable attribute for the given agent.
   *
   * @param agent The agent whose attribute this object represents.
   * @param name  The name of the attribute represented by this object.
   */
  public ReadableAgentAttribute(IAgent agent, String name) {
    super();
    this.agent = agent;
    this.name = name;
  }

  /**
   * @return This attribute’s name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * @return This attribute’s value.
   */
  public double getValue() {
    return (double) this.agent.getAttribute(this.getName());
  }

  @Override
  public String toString() {
    return String.format("RAttribute{name=%s,value=%s}", this.getName(), this.getValue());
  }
}
