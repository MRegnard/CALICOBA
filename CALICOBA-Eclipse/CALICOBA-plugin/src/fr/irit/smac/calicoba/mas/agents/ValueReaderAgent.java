package fr.irit.smac.calicoba.mas.agents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.irit.smac.calicoba.ReadableAgentAttribute;
import fr.irit.smac.calicoba.mas.FloatValueMessage;
import fr.irit.smac.calicoba.mas.Message;

/**
 * Base class for agents that can read an agent attribute.
 * 
 * @author Damien Vergnet
 */
public abstract class ValueReaderAgent extends AgentWithAttribute<ReadableAgentAttribute<Double>> {
  /**
   * The cached value of the attribute. Necessary to store the value between
   * perceive and act phases.
   */
  private double cachedAttributeValue;
  /** The list of agents to send the attribute value. */
  private List<Agent> targetAgents;

  /**
   * Creates a new reader agent.
   * 
   * @param attribute The attribute this agent can read.
   */
  public ValueReaderAgent(ReadableAgentAttribute<Double> attribute) {
    super(attribute);
    this.targetAgents = new ArrayList<>();
  }

  /**
   * Reads the value of the associated attribute.
   */
  @Override
  public void perceive() {
    super.perceive();
    this.cachedAttributeValue = this.getGamaAttribute().getValue();
  }

  /**
   * Sends a message containing the read attribute value to all target agents.
   */
  @Override
  public void decideAndAct() {
    super.decideAndAct();
    Message message = new FloatValueMessage(this, this.cachedAttributeValue);
    this.allTargetAgents().forEachRemaining(a -> a.sendMessage(message));
  }

  /**
   * Adds an agent that this agent will send its values to.
   * 
   * @param agent The agent to add.
   */
  public void addTargetAgent(Agent agent) {
    this.targetAgents.add(agent);
  }

  /**
   * @return An iterator over all agents this agent can send its values to.
   */
  public Iterator<Agent> allTargetAgents() {
    return this.targetAgents.iterator();
  }
}
