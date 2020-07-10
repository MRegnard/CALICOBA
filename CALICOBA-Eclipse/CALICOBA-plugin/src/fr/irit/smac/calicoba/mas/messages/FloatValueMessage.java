package fr.irit.smac.calicoba.mas.messages;

import fr.irit.smac.calicoba.mas.agents.Agent;

/**
 * A message containing a single floating point value.
 *
 * @author Damien Vergnet
 */
public class FloatValueMessage extends Message<Agent<?>> {
  private final double value;
  private final ValueNature type;

  /**
   * Creates a new message with the given float value.
   *
   * @param sender The sender.
   * @param value  The value to send.
   * @param nature The nature the value parameter.
   */
  public FloatValueMessage(final Agent<?> sender, final double value, final ValueNature nature) {
    super(sender);
    this.value = value;
    this.type = nature;
  }

  /**
   * @return The float value.
   */
  public double getValue() {
    return this.value;
  }

  /**
   * @return The nature of the value.
   */
  public ValueNature getValueNature() {
    return this.type;
  }

  public enum ValueNature {
    CRITICALITY, MEASURE_VALUE, PARAM_VALUE, OBS_VALUE;
  }
}
