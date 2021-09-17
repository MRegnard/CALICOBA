package fr.irit.smac.calicoba.mas.agents.messages;

import java.util.Locale;

import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;
import fr.irit.smac.calicoba.mas.agents.actions.Direction;

/**
 * A message containing the criticality of a given objective agent.
 *
 * @author Damien Vergnet
 */
public class CriticalityMessage extends Message<ObjectiveAgent> {
  /** Current criticality of the objective agent. */
  private final double criticality;
  private final Direction variationDirection;

  /**
   * Creates a new request.
   * 
   * @param sender      Sender agent.
   * @param criticality Sender agent’s criticality.
   */
  public CriticalityMessage(final ObjectiveAgent sender, final double criticality, final Direction variationDirection) {
    super(sender);
    this.criticality = criticality;
    this.variationDirection = variationDirection;
  }

  public double getCriticality() {
    return this.criticality;
  }

  public Direction getVariationDirection() {
    return this.variationDirection;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(this.criticality);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + this.variationDirection.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || this.getClass() != obj.getClass()) {
      return false;
    }
    CriticalityMessage other = (CriticalityMessage) obj;
    return this.criticality == other.criticality && this.variationDirection == other.variationDirection
        && this.getSender() == other.getSender();
  }

  @Override
  public String getSenderName() {
    return this.getSender().getName();
  }

  @Override
  public String toString() {
    return String.format(Locale.US, "{%s;%f}", this.getSenderName(), this.criticality);
  }
}
