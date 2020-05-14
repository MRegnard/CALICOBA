package fr.irit.smac.calicoba.mas.agents;

import java.util.Objects;

import fr.irit.smac.calicoba.ReadableAgentAttribute;

/**
 * Base class for agents associated to a GAMA agent attribute.
 * 
 * @author Damien Vergnet
 */
public abstract class AgentWithAttribute<T extends ReadableAgentAttribute<?>> extends Agent {
  /** The attribute this agent has access to. */
  private final T attribute;

  /**
   * Creates a new agent for the given GAMA agent attribute.
   * 
   * @param attribute The GAMA agent attribute.
   */
  public AgentWithAttribute(T attribute) {
    this.attribute = Objects.requireNonNull(attribute);
  }

  /**
   * @return The GAMA agent attribute.
   */
  protected T getGamaAttribute() {
    return this.attribute;
  }

  /**
   * @return The name of the GAMA agent attribute.
   */
  public String getAttributeName() {
    return this.attribute.getName();
  }
}
