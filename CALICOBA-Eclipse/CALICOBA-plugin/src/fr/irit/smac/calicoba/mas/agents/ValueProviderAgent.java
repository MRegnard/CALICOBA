package fr.irit.smac.calicoba.mas.agents;

import java.util.HashSet;
import java.util.Set;

import fr.irit.smac.calicoba.ReadableAgentAttribute;
import fr.irit.smac.calicoba.mas.messages.FloatValueMessage;
import fr.irit.smac.calicoba.mas.messages.ValueRequestMessage;

/**
 * Base class for agents that can read an agent attribute and broadcast its
 * value.
 *
 * @author Damien Vergnet
 */
public abstract class ValueProviderAgent extends AgentWithAttribute<ReadableAgentAttribute, Void> {
  /** The list of agents that requested the value. */
  private Set<Agent<?>> requesters;
  private final FloatValueMessage.ValueNature messageType;

  /**
   * Creates a new provider agent.
   *
   * @param attribute The attribute this agent can read.
   */
  public ValueProviderAgent(ReadableAgentAttribute attribute, FloatValueMessage.ValueNature messageType) {
    super(attribute);
    this.requesters = new HashSet<>();
    this.messageType = messageType;
  }

  @Override
  public void perceive() {
    super.perceive();

    this.requesters.clear();
    this.iterateOverMessages(m -> {
      if (m instanceof ValueRequestMessage) {
        this.requesters.add(m.getSender());
      }
    });
  }

  /**
   * Sends a message containing the read attribute value to all target agents.
   */
  @Override
  public void decideAndAct() {
    super.decideAndAct();
    FloatValueMessage message = new FloatValueMessage(this, this.getAttributeValue(), this.messageType);
    this.requesters.forEach(a -> a.onMessage(message));
  }
}
