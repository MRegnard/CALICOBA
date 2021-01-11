package fr.irit.smac.calicoba.mas.agents;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
  private Map<String, ObjectiveAgent> objectiveAgents;
  private ValueMap currentObjectivesCriticalities;
  private boolean actionChosen;
  /** The objective agent this agent has chosen to help. */
  private Optional<String> helpedObjective;
  /** The action this agent plans to execute. */
  private Optional<ActionProposal> actionProposal;
  /** The latest performed action. */
  private Optional<ActionProposal> executedActionProposal;
  private boolean rejected;

  /** Sensitivities of this parameter on each objective. */
  private Map<ObjectiveAgent, Double> sensitivities;

  // TEMP fixed value for now
  private static final double VARIATION_AMOUNT = 1;

  /**
   * Creates a new Parameter agent for the given model parameter.
   *
   * @param parameter The GAMA agent’s attribute/parameter.
   * @param isFloat   Wether the attribute is a floating point value.
   */
  public ParameterAgent(WritableAgentAttribute<Double> parameter) {
    super(parameter);
    this.currentObjectivesCriticalities = new ValueMap();
    this.actionChosen = false;
    this.helpedObjective = Optional.empty();
    this.actionProposal = Optional.empty();
    this.executedActionProposal = Optional.empty();
    this.rejected = false;
  }

  public Map<ObjectiveAgent, Double> getSensitivities() {
    return this.sensitivities;
  }

  public void setInitialSensitivities(Map<ObjectiveAgent, Double> sensitivities) {
    this.sensitivities = sensitivities;
  }

  public Optional<ActionProposal> getExecutedActionProposal() {
    return this.executedActionProposal;
  }

  public Optional<String> getHelpedObjective() {
    return this.helpedObjective;
  }

  public void onActionProposalRejection() {
    this.rejected = true;
    Logger.debug("rejected (" + this.getAttributeName() + ")");
  }

  public void onActionProposalConfirmation() {
    this.rejected = false;
    Logger.debug("confirmed (" + this.getAttributeName() + ")");
  }

  @Override
  public void perceive() {
    super.perceive();

    this.actionChosen = false;
    this.rejected = false;
    if (this.objectiveAgents == null) {
      this.objectiveAgents = this.getWorld().getAgentsForType(ObjectiveAgent.class).stream()
          .collect(Collectors.toMap(ObjectiveAgent::getName, Function.identity()));
    }
    this.currentObjectivesCriticalities.clear();
    this.objectiveAgents.values()
        .forEach(oa -> this.currentObjectivesCriticalities.put(oa.getName(), oa.getCriticality()));
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    if (!this.actionChosen) {
      Triplet<Double, Optional<String>, ValueMap> action = this.selectAction(false);
      this.helpedObjective = action.getSecond();
      this.actionProposal = Optional.of(new ActionProposal(this, action.getFirst(), action.getThird()));
      this.actionChosen = true;
      // Send only to selected objective
      this.helpedObjective.ifPresent(o -> this.objectiveAgents.get(o).onActionProposal(this.actionProposal.get()));

    } else {
      Logger.debug("rejected (" + this.getAttributeName() + "): " + this.rejected);
      if (this.rejected) {
        Triplet<Double, Optional<String>, ValueMap> action = this.selectAction(true);
        this.helpedObjective = action.getSecond();
        this.actionProposal = Optional.of(new ActionProposal(this, action.getFirst(), action.getThird()));
        Logger.debug("new action proposal (" + this.getAttributeName() + "): " + this.actionProposal);
      }

      this.addToParameterValue(this.actionProposal.get().getAction());
      this.executedActionProposal = this.actionProposal;
      this.actionProposal = Optional.empty();
      Logger.debug("executed action (" + this.getAttributeName() + ")");
    }
  }

  /**
   * Returns the best action to perform.
   *
   * @param ignoreRejecter If true, the currently selected objective will be
   *                       ignored as it has rejected this agent’s action
   *                       proposal.
   * @return The selected action, and the associated situation (if any) and
   *         expected objectives variations.
   */
  private Triplet<Double, Optional<String>, ValueMap> selectAction(boolean ignoreRejecter) {
    ValueMap expectedCriticalities = new ValueMap();
    List<String> ignored = new LinkedList<>();
    Optional<String> selectedObjective = Optional.empty();
    double action = 0;
    boolean tryOpposite = false;

    Logger.debug("helped objective (" + this.getAttributeName() + "): " + this.helpedObjective);
    if (ignoreRejecter && this.helpedObjective.isPresent()) {
      ignored.add(this.helpedObjective.get());
    }

    // Get overall most critical.
    String mostCritical = this.currentObjectivesCriticalities.entrySet().stream()
        .max((e1, e2) -> e1.getValue().compareTo(e2.getValue())) //
        .map(e -> e.getKey()) //
        .get(); // Should always exist

    boolean actionFound = false;
    while (!actionFound) {
      // Get most critical that is not ignored.
      selectedObjective = this.currentObjectivesCriticalities.entrySet().stream()
          .filter(e -> !ignored.contains(e.getKey())) //
          .max((e1, e2) -> e1.getValue().compareTo(e2.getValue())) //
          .map(e -> e.getKey());
      Logger.debug("selected objective (" + this.getAttributeName() + "): " + selectedObjective);

      if (selectedObjective.isPresent()) {
        ObjectiveAgent objective = this.objectiveAgents.get(selectedObjective.get());
        double sensitivity = this.sensitivities.get(objective);
        double actionSign = 0;

        if (objective.isAboveSetpoint()) {
          actionSign = -Math.copySign(1, sensitivity);
        } else if (objective.isBelowSetpoint()) {
          actionSign = Math.copySign(1, sensitivity);
        }
        if (tryOpposite) {
          actionSign = -actionSign;
        }
        Logger.debug("action sign (" + this.getAttributeName() + "): " + actionSign);
        action = actionSign * VARIATION_AMOUNT;

        final double a = action * sensitivity;
        this.objectiveAgents.values()
            // FIXME estimateCriticality pas terrible
            .forEach(agent -> expectedCriticalities.put(agent.getName(), agent.estimateCriticality(a)));
        Logger.debug("expected variations (" + this.getAttributeName() + "): " + expectedCriticalities);

        // Check if expected criticalities do not exceed current max criticality
        actionFound = !expectedCriticalities.entrySet().stream() //
            .filter(e -> e.getValue() > this.objectiveAgents.get(mostCritical).getCriticality()) //
            .findAny() //
            .isPresent();
        if (!actionFound) {
          tryOpposite = !tryOpposite;
          if (!tryOpposite) {
            ignored.add(selectedObjective.get());
          }
        }

      } else { // Cannot help any objective, do nothing
        action = 0;
        actionFound = true;
      }
    }

    return new Triplet<>(action, selectedObjective, expectedCriticalities);
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
