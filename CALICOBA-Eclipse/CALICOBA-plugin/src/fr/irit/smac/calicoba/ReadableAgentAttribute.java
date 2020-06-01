package fr.irit.smac.calicoba;

import msi.gama.metamodel.agent.IAgent;

/**
 * This class represents an attribute of a GAMA agent whose value can be read.
 * 
 * @author Damien Vergnet
 */
public class ReadableAgentAttribute<T> {
  /** This attribute’s name. */
  private final String name;
  /** This attribute’s type. */
  private final Class<T> type;
  /** The agent this attribute belongs to. */
  protected final IAgent agent;

  /**
   * Creates a readable attribute for the given agent.
   * 
   * @param agent The agent whose attribute this object represents.
   * @param name  The name of the attribute represented by this object.
   * @param type  The type of the attribute.
   */
  public ReadableAgentAttribute(IAgent agent, String name, Class<T> type) {
    super();
    this.agent = agent;
    this.name = name;
    this.type = type;
  }

  /**
   * @return This attribute’s name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * @return This attribute’s type.
   */
  public Class<T> getType() {
    return this.type;
  }

  /**
   * @return This attribute’s value.
   */
  @SuppressWarnings("unchecked")
  public T getValue() {
    return (T) this.agent.getAttribute(this.getName());
  }

  @Override
  public String toString() {
    return String.format("RAttribute{name=%s,type=%s,value=%s}", this.getName(), this.getType(), this.getValue());
  }
}
