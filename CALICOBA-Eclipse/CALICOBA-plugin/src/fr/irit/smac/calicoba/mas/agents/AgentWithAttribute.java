package fr.irit.smac.calicoba.mas.agents;

import java.util.Objects;

import fr.irit.smac.calicoba.ReadableAgentAttribute;

/**
 * Base class for agents associated to a GAMA agent attribute.
 * 
 * @author Damien Vergnet
 */
public abstract class AgentWithAttribute<T extends ReadableAgentAttribute<Double>> extends Agent {
  /** The attribute this agent has access to. */
  private final T attribute;

  /**
   * The cached value of the attribute. Necessary to store the value between
   * perceive and act phases.
   */
  private double cachedAttributeValue;
  /** The all-time minimum value. */
  private double min;
  /** The all-time maximum value. */
  private double max;

  /**
   * Creates a new agent for the given GAMA agent attribute.
   * 
   * @param attribute The GAMA agent attribute.
   */
  public AgentWithAttribute(T attribute) {
    this.attribute = Objects.requireNonNull(attribute);
    this.min = Double.NaN;
    this.max = Double.NaN;
  }

  /**
   * Reads the value of the associated attribute.
   */
  @Override
  public void perceive() {
    super.perceive();
    this.cachedAttributeValue = this.getGamaAttribute().getValue();
    this.min = Double.isNaN(this.min) ? this.cachedAttributeValue : Math.min(this.cachedAttributeValue, this.min);
    this.max = Double.isNaN(this.max) ? this.cachedAttributeValue : Math.max(this.cachedAttributeValue, this.max);
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

  /**
   * @return The value of the GAMA agent attribute.
   */
  public double getAttributeValue() {
    return this.cachedAttributeValue;
  }

  /**
   * @return The all-time minimum value of the GAMA agent attribute.
   */
  public double getAttributeMinValue() {
    return this.min;
  }

  /**
   * @return The all-time maximum value of the GAMA agent attribute.
   */
  public double getAttributeMaxValue() {
    return this.max;
  }
}
