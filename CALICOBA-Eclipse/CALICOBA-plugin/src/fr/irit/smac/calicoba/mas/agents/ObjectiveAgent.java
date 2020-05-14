package fr.irit.smac.calicoba.mas.agents;

import java.util.HashSet;
import java.util.Set;

import fr.irit.smac.calicoba.mas.FloatValueMessage;
import fr.irit.smac.calicoba.mas.Message;

/**
 * An Objective agent gets a Measure and Observation then computes a criticality
 * value and finally sends it to all Parameter agents.
 * 
 * @author Damien Vergnet
 */
public class ObjectiveAgent extends Agent {
  private MeasureAgent measureAgent;
  private ObservationAgent observationAgent;
  /** The set of agents to send the criticality to. */
  private Set<ParameterAgent> parameterAgents;
  /** The current observation value. */
  private double observationValue;
  /** The current measure value. */
  private double measureValue;
  /** The criticality. */
  private double criticality;

  /**
   * Creates a new Objective agent for the given Measure and Observation agents.
   * 
   * @param measureAgent     The Measure agent.
   * @param observationAgent The Observation agent.
   */
  public ObjectiveAgent(MeasureAgent measureAgent, ObservationAgent observationAgent) {
    this.measureAgent = measureAgent;
    this.observationAgent = observationAgent;
    this.parameterAgents = new HashSet<>();
  }

  /**
   * Adds a parameter agent.
   * 
   * @param agent The Parameter agent.
   */
  public void addParameterAgent(ParameterAgent agent) {
    this.parameterAgents.add(agent);
  }

  /**
   * Reads the value of the measure and the observation then stores it for use in
   * the act step.
   */
  @Override
  public void perceive() {
    super.perceive();

    Message m;

    while ((m = this.getMessage()) != null) {
      if (m instanceof FloatValueMessage) {
        FloatValueMessage fm = (FloatValueMessage) m;

        if (m.getSender() == this.measureAgent) {
          this.measureValue = fm.getValue();
        }
        if (m.getSender() == this.observationAgent) {
          this.observationValue = fm.getValue();
        }
      }
    }
  }

  /**
   * Computes the criticality then sends it to all Parameter agents.
   */
  @Override
  public void decideAndAct() {
    super.decideAndAct();

    this.computeCriticality();
    Message message = new FloatValueMessage(this, this.getCriticality());
    this.parameterAgents.forEach(a -> a.sendMessage(message));
  }

  /**
   * Computes the criticality as the absolute value of the distance between the
   * measure and the observation.
   */
  private void computeCriticality() {
    this.criticality = Math.abs(this.measureValue - this.observationValue);
  }

  /**
   * @return The criticality.
   * @see #computeCriticality()
   */
  public double getCriticality() {
    return this.criticality;
  }
}
