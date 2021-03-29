package fr.irit.smac.calicoba.mas.agents.data;

import java.util.Locale;

/**
 * A request is sent by satisfaction agents to measure/parameter agents to ask
 * them to change their value in the given direction.
 *
 * @author Damien Vergnet
 */
public class VariationRequest {
  /** Name of the agent that sent this request. */
  public final String senderName;
  /** Current criticality of the sender agent. */
  public final double criticality;
  /** Desired variation direction. */
  public final Direction direction;
  /** Name of the last measure agent that re-sent this request. */
  private String lastAgent;

  /**
   * Creates a new request.
   * 
   * @param senderName  Sender agent’s name.
   * @param criticality Sender agent’s criticality.
   * @param direction   Desired variation direction.
   */
  public VariationRequest(final String senderName, final double criticality, final Direction direction) {
    this.senderName = senderName;
    this.criticality = criticality;
    this.direction = direction;
  }

  public void setLastAgent(String lastAgent) {
    this.lastAgent = lastAgent;
  }

  public String getLastAgent() {
    return this.lastAgent;
  }

  /**
   * @return A new request object with the opposite variation direction.
   */
  public VariationRequest getOppositeRequest() {
    return new VariationRequest(this.senderName, this.criticality, this.direction.getOpposite());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp = Double.doubleToLongBits(this.criticality);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((this.direction == null) ? 0 : this.direction.hashCode());
    result = prime * result + ((this.senderName == null) ? 0 : this.senderName.hashCode());
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
    VariationRequest other = (VariationRequest) obj;
    return Double.doubleToLongBits(this.criticality) == Double.doubleToLongBits(other.criticality)
        && this.direction == other.direction
        && ((this.senderName == null && other.senderName == null) || this.senderName.equals(other.senderName));
  }

  @Override
  public String toString() {
    return String.format(Locale.US, "{%s;%f;%s}", this.senderName, this.criticality, this.direction);
  }
}
