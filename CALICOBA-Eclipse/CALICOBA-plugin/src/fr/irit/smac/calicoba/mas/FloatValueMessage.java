package fr.irit.smac.calicoba.mas;

import fr.irit.smac.calicoba.mas.agents.Agent;

/**
 * A message containing a single floating point value.
 * 
 * @author Damien Vergnet
 */
public class FloatValueMessage extends Message {
  private final double value;

  /**
   * Creates a new message with the given float value.
   * 
   * @param agent The sender.
   * @param value The value to send.
   */
  public FloatValueMessage(Agent agent, double value) {
    super(agent);
    this.value = value;
  }

  /**
   * Returns the float value.
   */
  public double getValue() {
    return this.value;
  }
}
