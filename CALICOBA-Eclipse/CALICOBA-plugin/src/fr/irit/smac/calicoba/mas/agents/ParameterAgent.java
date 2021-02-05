package fr.irit.smac.calicoba.mas.agents;

import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
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
  /** The objective agent this agent has chosen to help. */
  private Optional<String> helpedObjective;
  /** The action this agent plans to execute. */
  private Optional<ActionProposal> actionProposal;

  private ValueMap estimatedParticipations;

  /** Sensitivities of this parameter on each objective. */
  private Map<ObjectiveAgent, Double> sensitivities;

  // TEMP fixed value for now
  private static final double VARIATION_AMOUNT = 1;
  // TEMP
  private static final Scanner SC = new Scanner(System.in);

  /**
   * Creates a new Parameter agent for the given model parameter.
   *
   * @param parameter The GAMA agent’s attribute/parameter.
   * @param isFloat   Wether the attribute is a floating point value.
   */
  public ParameterAgent(WritableAgentAttribute<Double> parameter) {
    super(parameter);
    this.currentObjectivesCriticalities = new ValueMap();
    this.helpedObjective = Optional.empty();
    this.actionProposal = Optional.empty();
    this.estimatedParticipations = new ValueMap();
  }

  public Map<ObjectiveAgent, Double> getSensitivities() {
    return this.sensitivities;
  }

  public void setInitialSensitivities(Map<ObjectiveAgent, Double> sensitivities) {
    this.sensitivities = sensitivities;
  }

  public Optional<ActionProposal> getExecutedActionProposal() {
    return this.actionProposal;
  }

  public boolean hasChosenAction() {
    return this.actionProposal.isPresent();
  }

  public Optional<String> getHelpedObjective() {
    return this.helpedObjective;
  }

  public void updateEstimatedVariationParticipation(ObjectiveAgent agent, double participation) {
    this.estimatedParticipations.put(agent.getName(), participation);
  }

  @Override
  public void perceive() {
    super.perceive();

    this.actionProposal = Optional.empty();
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

    Triplet<Double, Optional<String>, ValueMap> action = this.selectAction();
    this.helpedObjective = action.getSecond();
    this.actionProposal = Optional.of(new ActionProposal(this, action.getFirst(), action.getThird()));
    this.addToParameterValue(this.actionProposal.get().getAction());
    Logger.debug("executed action (" + this.getAttributeName() + ")");
  }

  /**
   * Returns the best action to perform.
   *
   * @return The selected action, and the associated situation (if any) and
   *         expected objectives variations.
   */
  private Triplet<Double, Optional<String>, ValueMap> selectAction() {
    double action = 0;

    Logger.debug("helped objective (" + this.getAttributeName() + "): " + this.helpedObjective);

    // Get overall most critical.
    String mostCritical = this.currentObjectivesCriticalities.entrySet().stream()
        .max((e1, e2) -> e1.getValue().compareTo(e2.getValue())) //
        .map(e -> e.getKey()) //
        .get(); // Should always exist

    Logger.debug("selected objective (" + this.getAttributeName() + "): " + mostCritical);

    ObjectiveAgent objective = this.objectiveAgents.get(mostCritical);
    double sensitivity = this.sensitivities.get(objective);
    double actionSign = 0;

    if (objective.isAboveSetpoint()) {
      actionSign = -Math.copySign(1, sensitivity);
    } else if (objective.isBelowSetpoint()) {
      actionSign = Math.copySign(1, sensitivity);
    }
    Logger.debug("action sign (" + this.getAttributeName() + "): " + actionSign);

    ValueMap expectedCriticalities = new ValueMap();
    // Estimate criticalities if all other agent execute the same action as last
    // cycle but this agent does nothing
    this.objectiveAgents.values().forEach(oa -> {
      String name = oa.getName();
      double variation = oa.getCriticality() - oa.getPreviousCriticality();
      expectedCriticalities.put(name, oa.getCriticality() + variation * (1 - this.estimatedParticipations.get(name)));
      Logger.debug("objective: " + name);
      Logger.debug("variation: " + variation);
      Logger.debug("estimated participation: " + this.estimatedParticipations.get(name));
    });
    Logger.debug("expected variations (" + this.getAttributeName() + "): " + expectedCriticalities);

    // Check if expected criticalities do not exceed current max criticality
    boolean ok = this.getWorld().getCycle() == 0 || !expectedCriticalities.entrySet().stream() //
        .filter(e -> e.getValue() > this.objectiveAgents.get(mostCritical).getCriticality()) //
        .findAny() //
        .isPresent();

    Optional<String> selectedObjective;
    if (!ok) {
      System.out.print("Choose action: ");
      System.out.flush();
      double v = SC.nextInt();
      if (v == 0) {
        actionSign = 0;
      } else {
        actionSign = Math.copySign(1.0, v); // TEMP ask for action to user
      }
      System.out.println();
      selectedObjective = Optional.empty();
    } else {
      selectedObjective = Optional.of(mostCritical);
    }

    action = actionSign * VARIATION_AMOUNT;

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
