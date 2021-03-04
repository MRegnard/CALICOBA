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
  public final Way way;

  /**
   * Creates a new request.
   * 
   * @param senderName  Sender agent’s name.
   * @param criticality Sender agent’s criticality.
   * @param increase    Desired variation direction.
   */
  public VariationRequest(final String senderName, final double criticality, final boolean increase) {
    this.senderName = senderName;
    this.criticality = criticality;
    this.way = new Way(increase);
  }

  /**
   * @return A new request object with the opposite variation direction.
   */
  public VariationRequest getOppositeRequest() {
    return new VariationRequest(this.senderName, this.criticality, !this.way.increase());
  }

  @Override
  public String toString() {
    return String.format(Locale.US, "{%s;%f;%s}", this.senderName, this.criticality, this.way);
  }
}
