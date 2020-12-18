package fr.irit.smac.calicoba.mas.agents;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.math3.util.Pair;

import fr.irit.smac.calicoba.WritableAgentAttribute;
import fr.irit.smac.calicoba.mas.agents.data.ActionProposal;
import fr.irit.smac.util.Logger;
import fr.irit.smac.util.Triplet;
import fr.irit.smac.util.ValueMap;

/**
 * Parameter agents control the parameters of the target model. Their actions
 * are based on the current objectives and passed situations.
 *
 * @author Damien Vergnet
 */
public class ParameterAgent extends AgentWithGamaAttribute<WritableAgentAttribute<Double>> {
  private List<ObjectiveAgent> objectiveAgents;
  /** The current objective values. */
  private ValueMap currentObjectivesValues;
  private List<Triplet<ValueMap, ValueMap, SituationAgent>> proposedActions;
  private boolean actionChosen;
  /** The latest performed action. */
  private Optional<Double> selectedAction;
  private Optional<SituationAgent> selectedSituation;

  private boolean hasExecutedAction;

  // TEMP
  private boolean manual;
  private Scanner sc = new Scanner(System.in);

  // TEMP fixed value for now
  private static final double AMOUNT = 1;

  /**
   * Creates a new Parameter agent for the given parameter.
   *
   * @param parameter The GAMA agent’s attribute/parameter.
   * @param isFloat   Wether the attribute is a floating point value.
   */
  public ParameterAgent(WritableAgentAttribute<Double> parameter, boolean isFloat) {
    super(parameter);
    this.currentObjectivesValues = new ValueMap();
    this.proposedActions = new LinkedList<>();
    this.actionChosen = false;
    this.selectedAction = Optional.empty();
    this.selectedSituation = Optional.empty();
    this.hasExecutedAction = false;
    this.manual = true;
  }

  public void suggestAction(ActionProposal proposal) {
    this.proposedActions.add(
        new Triplet<>(proposal.getActions(), proposal.getExpectedCriticalitiesVariations(), proposal.getProposer()));
  }

  /**
   * @return The latest performed action, the associated objective that it should
   *         help and the expected variation.
   */
  public Pair<Double, SituationAgent> getLastAction() {
    return new Pair<>(this.selectedAction.orElse(Double.NaN), this.selectedSituation.orElse(null));
  }

  public boolean hasExecutedAction() {
    return this.hasExecutedAction;
  }

  @Override
  public void perceive() {
    super.perceive();

    this.hasExecutedAction = false;
    this.actionChosen = false;
    this.proposedActions.clear();
    if (this.objectiveAgents == null) {
      this.objectiveAgents = this.getWorld().getAgentsForType(ObjectiveAgent.class);
    }
    this.objectiveAgents.forEach(oa -> this.currentObjectivesValues.put(oa.getName(), oa.getCriticality()));
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    if (!this.actionChosen) {
      this.discussActions();
    } else {
      this.addToParameterValue(this.selectedAction.get());
      this.hasExecutedAction = true;
    }
  }

  private void discussActions() {
    Pair<Double, SituationAgent> result = this.getAction();

    this.actionChosen = true;
    this.selectedAction = Optional.of(result.getFirst());
    this.selectedSituation = Optional.ofNullable(result.getSecond());
  }

  /**
   * Returns the best action to perform.
   *
   * @return The best action to perform, if any, and the associated situation.
   */
  private Pair<Double, SituationAgent> getAction() {
    Optional<Pair<Double, SituationAgent>> opt = this.selectAction(true);
    if (opt.isPresent()) {
      Logger.info("action found " + opt.get()); // DEBUG
      return opt.get();
    }

    // No action are beneficial to the most critical objective, perform the opposite
    // action that previously increased the most its criticality.
    opt = this.selectAction(false);
    if (opt.isPresent()) {
      Logger.info("anti-action found " + opt.get()); // DEBUG
      return new Pair<>(-opt.get().getFirst(), null);
    }

    // TEMP There is no action either, select a random action.
    double action;
    double r = Math.random();

    if (r < 0.33) {
      action = -AMOUNT;
    } else if (r > 0.66) {
      action = AMOUNT;
    } else {
      action = 0.0;
    }

    Logger.info("random action " + action); // DEBUG
    return new Pair<>(action, null);
  }

  private Optional<Pair<Double, SituationAgent>> selectAction(boolean getBest) {
    // Should always exist.
    String mostCritical = this.currentObjectivesValues.entrySet().stream()
        .max((e1, e2) -> e1.getValue().compareTo(e2.getValue())).map(e -> e.getKey()).get();

    Predicate<Double> filter;

    if (getBest) {
      // Filter out entries where the current most critical objective’s criticality
      // increased.
      filter = d -> d <= 0;
    } else {
      // Filter out entries where the current most critical objective’s criticality
      // decreased.
      filter = d -> d >= 0;
    }

    Comparator<Triplet<ValueMap, ValueMap, SituationAgent>> c = (a1, a2) -> a1.getSecond().get(mostCritical)
        .compareTo(a2.getSecond().get(mostCritical));

    Stream<Triplet<ValueMap, ValueMap, SituationAgent>> stream = this.proposedActions.stream()
        .filter(a -> filter.test(a.getSecond().get(mostCritical)));
    Optional<Triplet<ValueMap, ValueMap, SituationAgent>> opt;

    if (getBest) {
      // Get the entry where the criticality decreased the most.
      opt = stream.min(c);
    } else {
      // Get the entry where the criticality increased the most.
      opt = stream.max(c);
    }

    // TEMP set “by hand” the action to execute
    if (this.manual) {
      System.out.println("Action to execute:");
      String s = this.sc.nextLine();
      if (s.equals("m")) {
        this.manual = false;
        this.getWorld().setResetFlag();
      } else {
        return Optional.of(new Pair<>(Double.parseDouble(s), null));
      }
    }

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
}
