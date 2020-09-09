package fr.irit.smac.calicoba.mas.agents;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import fr.irit.smac.calicoba.WritableAgentAttribute;
import fr.irit.smac.calicoba.mas.messages.ActionProposalMessage;
import fr.irit.smac.calicoba.mas.messages.FloatValueMessage;
import fr.irit.smac.calicoba.mas.messages.Message;
import fr.irit.smac.calicoba.mas.messages.PerformedActionRequestMessage;
import fr.irit.smac.calicoba.mas.messages.ProposalsSentMessage;
import fr.irit.smac.calicoba.mas.messages.ValueRequestMessage;
import fr.irit.smac.util.Logger;
import fr.irit.smac.util.Triplet;
import fr.irit.smac.util.ValueMap;

/**
 * Parameter agents control the parameters of the target model. Their actions
 * are based on the current objectives and passed situations.
 *
 * @author Damien Vergnet
 */
public class ParameterAgent extends AgentWithGamaAttribute<WritableAgentAttribute, ParameterAgent.State> {
  private List<ObjectiveAgent> objectiveAgents;
  /** The current objective values. */
  private ValueMap currentObjectivesValues;
  private ValueMap tempObjectives;
  private List<Triplet<ValueMap, ValueMap, SituationAgent>> proposedActions;
  /** The latest performed action. */
  private double selectedAction;
  private SituationAgent selectedSituation;

  private Set<Agent<?>> valueRequesters;
  private Set<Agent<?>> performedActionRequesters;

  private int remainingValuesToUpdate;

  // TEMP
  private boolean manual;
  private Scanner sc = new Scanner(System.in);

  // TEMP fixed value for now
  private static final double AMOUNT = 0.01;

  /**
   * Creates a new Parameter agent for the given parameter.
   *
   * @param parameter The GAMA agent’s attribute/parameter.
   * @param isFloat   Wether the attribute is a floating point value.
   */
  public ParameterAgent(WritableAgentAttribute parameter, boolean isFloat) {
    super(parameter);
    this.valueRequesters = new HashSet<>();
    this.performedActionRequesters = new HashSet<>();
    this.currentObjectivesValues = new ValueMap();
    this.proposedActions = new LinkedList<>();
    this.manual = true;
    this.setState(State.REQUESTING_OBJ);
  }

  @Override
  public void onGamaCycleBegin() {
    super.onGamaCycleBegin();

    if (this.objectiveAgents == null) {
      this.objectiveAgents = this.getWorld().getAgentsForType(ObjectiveAgent.class);
      Logger.debug(this.objectiveAgents); // DEBUG
    }
    this.setState(State.REQUESTING_OBJ);
  }

  /*
   * Perception
   */

  @Override
  public void perceive() {
    super.perceive();

    switch (this.getState()) {
      case REQUESTING_OBJ:
        this.onRequestingObjectives();
        break;

      case UPDATING_OBJ:
        this.onUpdatingObjectives();
        break;

      case AWAITING_PROPOSALS:
        this.onAwaitingProposals();
        break;

      case DISCUSSING:
        this.onDiscussing();
        break;

      case EXECUTING:
        this.onExecuting();
        break;
    }
  }

  private void onRequestingObjectives() {
    this.tempObjectives = new ValueMap();

    this.iterateOverMessages(this::handleRequest);
  }

  private void onUpdatingObjectives() {
    this.iterateOverMessages(m -> {
      this.handleRequest(m);
      if (m instanceof FloatValueMessage) {
        FloatValueMessage fm = (FloatValueMessage) m;
        Agent<?> sender = m.getSender();
        double value = fm.getValue();

        switch (fm.getValueNature()) {
          case CRITICALITY:
            this.tempObjectives.put(((ObjectiveAgent) sender).getName(), value);
            this.remainingValuesToUpdate--;
            break;

          default:
            break;
        }
      }
    });
  }

  private void onAwaitingProposals() {
    this.iterateOverMessages(m -> {
      this.handleRequest(m);
      if (m instanceof ActionProposalMessage) {
        ActionProposalMessage apm = (ActionProposalMessage) m;
        this.proposedActions.add(new Triplet<>(apm.getActions(), apm.getExpectedCriticalitiesVariations(),
            apm.getSender()));
      }
      else if (m instanceof ProposalsSentMessage) {
        this.setState(State.DISCUSSING);
      }
    });
  }

  private void onDiscussing() {
    this.iterateOverMessages(this::handleRequest);
  }

  private void onExecuting() {
    this.iterateOverMessages(this::handleRequest);
  }

  private void handleRequest(Message<?> m) {
    if (m instanceof ValueRequestMessage) {
      this.valueRequesters.add(m.getSender());
    }
    else if (m instanceof PerformedActionRequestMessage) {
      this.performedActionRequesters.add(m.getSender());
    }
  }

  /*
   * Decision and action
   */

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    switch (this.getState()) {
      case REQUESTING_OBJ:
        this.requestObjectives();
        break;

      case UPDATING_OBJ:
        this.checkUpdateFinished();
        break;

      case DISCUSSING:
        this.discussAction();
        break;

      case EXECUTING:
        this.executeAction();
        break;

      default:
        break;
    }

    FloatValueMessage message = new FloatValueMessage(this, this.getAttributeValue(),
        FloatValueMessage.ValueNature.PARAM_VALUE);
    this.valueRequesters.forEach(a -> a.onMessage(message));
  }

  private void requestObjectives() {
    ValueRequestMessage m = new ValueRequestMessage(this);

    this.objectiveAgents.forEach(o -> o.onMessage(m));
    this.remainingValuesToUpdate = this.objectiveAgents.size();

    this.setState(State.UPDATING_OBJ);
  }

  private void checkUpdateFinished() {
    if (this.remainingValuesToUpdate <= 0) {
      this.currentObjectivesValues = this.tempObjectives;
      this.proposedActions.clear();
      this.setState(State.AWAITING_PROPOSALS);
    }
  }

  private void discussAction() {
    Pair<Double, SituationAgent> result = this.getAction();

    this.selectedAction = result.getFirst();
    this.selectedSituation = result.getSecond();

    this.setState(State.EXECUTING);
  }

  private void executeAction() {
    this.addToParameterValue(this.selectedAction);
    this.getWorld().restoreGama();
  }

  /**
   * Returns the best action to perform.
   *
   * @return The best action to perform, if any, the objective for which the
   *         action is beneficial and the expected variation.
   */
  private Pair<Double, SituationAgent> getAction() {
    Optional<Pair<Double, SituationAgent>> opt = this.selectAction();

    if (opt.isPresent()) {
      return opt.get();
    }

    double defaultAction = 0;
    double random = Math.random();

    if (random < 0.33) {
      defaultAction = -AMOUNT;
    }
    else if (random > 0.66) {
      defaultAction = AMOUNT;
    }

    return new Pair<>(defaultAction, null);
  }

  private Optional<Pair<Double, SituationAgent>> selectAction() {
    System.out.println(this.currentObjectivesValues); // DEBUG
    // Should always exist.
    String mostCritical = this.currentObjectivesValues.entrySet().stream()
        .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
        .map(e -> e.getKey()).get();

    Optional<Triplet<ValueMap, ValueMap, SituationAgent>> opt = //
        this.proposedActions.stream()
            // Filter out entries where the current most critical objective’s criticality
            // increased.
            .filter(a -> a.getSecond().get(mostCritical) <= 0)
            // Get the entry where the criticality decreased the most.
            .min((a1, a2) -> a1.getSecond().get(mostCritical)
                .compareTo(a2.getSecond().get(mostCritical)));

    // TEMP
    if (this.manual) {
      System.out.println("Action to execute:");
      String s = this.sc.nextLine();
      if (s.equals("m")) {
        this.manual = false;
      }
      else {
        return Optional.of(new Pair<>(Double.parseDouble(s), null));
      }
    }

    // TEMP
//    Logger.debug("Selected situation: " + opt);
//    if (this.manual) {
//      this.proposedActions.forEach(System.out::println);
//      System.out.println("Situation to select:");
//      int i = this.sc.nextInt();
//      opt = Optional.of(this.proposedActions.get(i));
//    }

    return opt.map(t -> new Pair<>(t.getFirst().get(this.getAttributeName()), t.getThird()));
  }

  /**
   * Adds the given value to the parameter.
   *
   * @param value The value to add.
   */
  private void addToParameterValue(double value) {
    this.getGamaAttribute().setValue(this.getGamaAttribute().getValue() + value);
  }

  /**
   * @return The latest performed action, the associated objective that it should
   *         help and the expected variation.
   */
  public Pair<Double, SituationAgent> getLastAction() {
    return new Pair<>(this.selectedAction, this.selectedSituation);
  }

  @Override
  public String toString() {
    return super.toString() + String.format("{parameter=%s}", this.getGamaAttribute());
  }

  public enum State {
    REQUESTING_OBJ, UPDATING_OBJ, AWAITING_PROPOSALS, DISCUSSING, EXECUTING;
  }
}
