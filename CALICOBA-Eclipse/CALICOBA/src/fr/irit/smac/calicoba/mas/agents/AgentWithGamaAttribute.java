package fr.irit.smac.calicoba.mas.agents;

import java.util.Objects;

import fr.irit.smac.calicoba.mas.agents.messages.CriticalityMessage;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;

/**
 * Base class for agents associated to an attribute of the target model. This
 * type of agents can receive messages through its
 * {@link #onMessage(CriticalityMessage)} method.
 *
 * @author Damien Vergnet
 * 
 * @param <T> The type of attribute this agent is associated to.
 */
public abstract class AgentWithGamaAttribute<T extends ReadableModelAttribute<Double, P>, P extends IValueProvider<Double>>
    extends Agent {
  /** The attribute this agent has access to. */
  private final T attribute;
  /**
   * The cached value of the attribute. Necessary to store the value between
   * perceive and act phases.
   */
  private double cachedAttributeValue;

  /**
   * Creates a new agent for the given model attribute.
   *
   * @param attribute The model attribute.
   */
  public AgentWithGamaAttribute(T attribute) {
    this.attribute = Objects.requireNonNull(attribute);
  }

  /**
   * Reads the value of the associated attribute.
   */
  @Override
  public void perceive() {
    super.perceive();
    this.cachedAttributeValue = this.getAttribute().getValue();
  }

  /**
   * @return The model attribute.
   */
  protected T getAttribute() {
    return this.attribute;
  }

  /**
   * @return The name of this attribute.
   */
  public String getAttributeName() {
    return this.attribute.getName();
  }

  /**
   * @return The value of the model attribute.
   */
  public double getAttributeValue() {
    return this.cachedAttributeValue;
  }

  /**
   * @return The minimum allowed value of the model attribute.
   */
  public double getAttributeMinValue() {
    return this.attribute.getMin();
  }

  /**
   * @return The maximum allowed value of the model attribute.
   */
  public double getAttributeMaxValue() {
    return this.attribute.getMax();
  }

  @Override
  public String toString() {
    return String.format("%s(%f)", this.getAttributeName(), this.cachedAttributeValue);
  }
}
