package fr.irit.smac.calicoba.mas.agents;

import java.util.HashSet;
import java.util.Set;

import fr.irit.smac.calicoba.mas.messages.FloatValueMessage;
import fr.irit.smac.calicoba.mas.messages.Message;
import fr.irit.smac.calicoba.mas.messages.ValueRequestMessage;

/**
 * An Objective agent gets a Measure and Observation then computes a criticality
 * value and finally sends it to all Parameter agents.
 *
 * @author Damien Vergnet
 */
public class ObjectiveAgent extends Agent<ObjectiveAgent.State> {
  private final String name;
  /** The set of agents to send the criticality to. */
  private Set<Agent<?>> targetAgents;
  private MeasureAgent measureAgent;
  private ObservationAgent observationAgent;
  /** The current observation value. */
  private double observationValue;
  /** The current measure value. */
  private double measureValue;
  /** The criticality. */
  private double criticality;

  private boolean observationUpdated;
  private boolean measureUpdated;

  /** The all-time minimum absolute criticality. */
  private double minAbsoluteCriticality;
  /** The all-time maximum absolute criticality. */
  private double maxAbsoluteCriticality;

  /**
   * Creates a new Objective agent with the given name.
   *
   * @param name This objective’s name.
   */
  public ObjectiveAgent(final String name, MeasureAgent measureAgent, ObservationAgent observationAgent) {
    this.name = name;
    this.measureAgent = measureAgent;
    this.observationAgent = observationAgent;
    this.targetAgents = new HashSet<>();
    this.minAbsoluteCriticality = Double.NaN;
    this.maxAbsoluteCriticality = Double.NaN;
  }

  @Override
  public void onGamaCycleBegin() {
    super.onGamaCycleBegin();

    this.observationUpdated = false;
    this.measureUpdated = false;
    this.setState(State.REQUESTING);
  }

  /*
   * Perception
   */

  /**
   * Reads the value of the measure and the observation then stores it for use in
   * the act step.
   */
  @Override
  public void perceive() {
    super.perceive();

    switch (this.getState()) {
      case REQUESTING:
        this.onRequesting();
        break;

      case UPDATING:
        this.onUpdating();
        break;

      case UPDATED:
        this.onUpdated();
        break;
    }
  }

  private void onRequesting() {
    this.iterateOverMessages(this::handleRequest);
  }

  private void onUpdating() {
    this.iterateOverMessages(m -> {
      this.handleRequest(m);
      if (m instanceof FloatValueMessage) {
        FloatValueMessage fm = (FloatValueMessage) m;
        double value = fm.getValue();

        switch (fm.getValueNature()) {
          case MEASURE_VALUE:
            this.measureValue = value;
            this.measureUpdated = true;
            break;

          case OBS_VALUE:
            this.observationValue = value;
            this.observationUpdated = true;
            break;

          default:
            break;
        }
      }
    });
  }

  private void onUpdated() {
    this.iterateOverMessages(this::handleRequest);
  }

  private void handleRequest(Message<?> m) {
    if (m instanceof ValueRequestMessage) {
      this.targetAgents.add(m.getSender());
    }
  }

  /*
   * Decision and action
   */

  /**
   * Computes the criticality then sends it to all Parameter agents.
   */
  @Override
  public void decideAndAct() {
    super.decideAndAct();

    switch (this.getState()) {
      case REQUESTING:
        this.requestValues();
        break;

      case UPDATING:
        this.computeCriticality();
        break;

      case UPDATED:
        this.answerRequests();
        break;
    }
  }

  private void requestValues() {
    ValueRequestMessage message = new ValueRequestMessage(this);
    this.measureAgent.onMessage(message);
    this.observationAgent.onMessage(message);
    this.setState(State.UPDATING);
  }

  /**
   * Computes the criticality as the absolute value of the distance between the
   * measure and the observation.
   */
  private void computeCriticality() {
    if (this.measureUpdated && this.observationUpdated) {
      double absoluteCriticality = Math.abs(this.measureValue - this.observationValue);

      if (Double.isNaN(this.minAbsoluteCriticality)) {
        this.minAbsoluteCriticality = absoluteCriticality;
      }
      else {
        this.minAbsoluteCriticality = Math.min(absoluteCriticality, this.minAbsoluteCriticality);
      }

      if (Double.isNaN(this.maxAbsoluteCriticality)) {
        this.maxAbsoluteCriticality = absoluteCriticality;
      }
      else {
        this.maxAbsoluteCriticality = Math.max(absoluteCriticality, this.maxAbsoluteCriticality);
      }

      // DEBUG
//      Logger.debug(absoluteCriticality);
//      Logger.debug(this.maxAbsoluteCriticality + " " + this.minAbsoluteCriticality);
//      Logger.debug(this.maxAbsoluteCriticality - this.minAbsoluteCriticality);
      double diff = this.maxAbsoluteCriticality - this.minAbsoluteCriticality;
      if (diff == 0) {
        this.criticality = absoluteCriticality;
      }
      else {
        this.criticality = absoluteCriticality / diff;
      }
      // DEBUG
//      Logger.debug(this.criticality);

      this.setState(State.UPDATED);
    }
  }

  private void answerRequests() {
    FloatValueMessage message = new FloatValueMessage(this, this.criticality,
        FloatValueMessage.ValueNature.CRITICALITY);
    this.targetAgents.forEach(a -> a.onMessage(message));
  }

  /**
   * @return The name of this agent.
   */
  public String getName() {
    return this.name;
  }

  // Only for GlobalSkill.
  public double getCriticality() {
    return this.criticality;
  }

  public enum State {
    REQUESTING, UPDATING, UPDATED;
  }
}
