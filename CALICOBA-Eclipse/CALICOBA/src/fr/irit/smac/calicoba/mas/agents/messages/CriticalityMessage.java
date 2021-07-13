package fr.irit.smac.calicoba.mas.agents.messages;

import java.util.Locale;

import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;

/**
 * A message containing the criticality of a given objective agent.
 *
 * @author Damien Vergnet
 */
public class CriticalityMessage extends Message<ObjectiveAgent> {
  /** Current criticality of the objective agent. */
  public final double criticality;

  /**
   * Creates a new request.
   * 
   * @param sender      Sender agent.
   * @param criticality Sender agent’s criticality.
   */
  public CriticalityMessage(final ObjectiveAgent sender, final double criticality) {
    super(sender);
    this.criticality = criticality;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp = Double.doubleToLongBits(this.criticality);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + this.getSender().hashCode();
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
    return Double.doubleToLongBits(this.criticality) == Double.doubleToLongBits(other.criticality)
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
