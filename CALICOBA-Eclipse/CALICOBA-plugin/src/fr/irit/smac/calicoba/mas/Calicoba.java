package fr.irit.smac.calicoba.mas;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.ReadableAgentAttribute;
import fr.irit.smac.calicoba.WritableAgentAttribute;
import fr.irit.smac.calicoba.mas.agents.Agent;
import fr.irit.smac.calicoba.mas.agents.MeasureEntity;
import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;
import fr.irit.smac.calicoba.mas.agents.ObservationEntity;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.util.Logger;

/**
 * Main class of CALICOBA.
 *
 * @author Damien Vergnet
 */
public class Calicoba {
  public static final String OUTPUT_DIR = System.getProperty("user.dir") + File.separator + "calicoba_output"
      + File.separator;

  /** Single instance. */
  private static Calicoba instance;

  /**
   * Tells whether CALICOBA has been initialized.
   *
   * @return true if and only if CALICOBA has been initialized.
   */
  public static boolean isInitialized() {
    return instance != null;
  }

  /**
   * Initializes CALICOBA.
   *
   * @param stepInterval The number of GAMA iterations between each step.
   */
  public static void init(int stepInterval) {
    instance = new Calicoba(stepInterval);
  }

  /**
   * Returns CALICOBA’s unique instance.
   *
   * @return CALICOBA instance.
   * @throws RuntimeException if CALICOBA has not been initialized.
   * @see #init()
   * @see #isInitialized()
   */
  public static Calicoba instance() {
    if (!isInitialized()) {
      throw new RuntimeException("CALICOBA not initialized.");
    }
    return instance;
  }

  /** The list of last attributed IDs for each agent type. */
  private Map<Class<? extends Agent>, Integer> globalIds;
  /** Registry of all alive agents by type. */
  private Map<Class<? extends Agent>, List<Agent>> agentsRegistry;
  /** Registry of all alive agents by ID. */
  private Map<String, Agent> agentsIdsRegistry;

  /** Number of GAMA iterations between each step. */
  private final int stepInterval;
  /** Number of remaining steps until the next iteration. */
  private int stepCountdown;

  private int cycle;

  private boolean resetFlag;

  private List<MeasureEntity> measures;
  private List<ObservationEntity> observations;
  private List<ObjectiveAgent> objectives;
  private List<ParameterAgent> parameters;

  private ReadableAgentAttribute<Boolean> shouldReset;
  private WritableAgentAttribute<Boolean> reset;

  /**
   * Creates a new instance of CALICOBA.
   *
   * @param stepInterval The number of GAMA iterations between each step.
   */
  private Calicoba(int stepInterval) {
    this.globalIds = new HashMap<>();
    this.agentsRegistry = new HashMap<>();
    this.agentsIdsRegistry = new HashMap<>();

    this.stepInterval = stepInterval;
    this.stepCountdown = 0;
  }

  public int getCycle() {
    return this.cycle;
  }

  public void setResetFlag() {
    this.resetFlag = true;
  }

  public void setShouldResetPredicate(ReadableAgentAttribute<Boolean> shouldReset) {
    this.shouldReset = shouldReset;
  }

  public void setResetAttribute(WritableAgentAttribute<Boolean> reset) {
    this.reset = reset;
  }

  /**
   * Creates an new agent for the given target model parameter.
   *
   * @param parameter A parameter of the target model.
   * @param isFloat   Whether the parameter is a float or an int.
   */
  public void addParameter(WritableAgentAttribute<Double> parameter) {
    Logger.info(String.format("Creating parameter \"%s\".", parameter.getName()));
    this.addAgent(new ParameterAgent(parameter));
  }

  /**
   * Creates a new agent for the given target model output.
   *
   * @param measure An output of the target model.
   */
  public void addMeasure(ReadableAgentAttribute<Double> measure) {
    Logger.info(String.format("Creating measure \"%s\".", measure.getName()));
    this.addAgent(new MeasureEntity(measure));
  }

  /**
   * Creates a new agent for the given reference system output.
   *
   * @param observation An output of the reference system.
   */
  public void addObservation(ReadableAgentAttribute<Double> observation) {
    Logger.info(String.format("Creating observation \"%s\".", observation.getName()));
    this.addAgent(new ObservationEntity(observation));
  }

  /**
   * Sets up the simulation. Creates links between agents, creates new agents when
   * needed and adds them to the world.
   */
  public void setup() {
    Logger.info("Setting up CALICOBA…");

    this.measures = this.getAgentsForType(MeasureEntity.class);
    this.observations = this.getAgentsForType(ObservationEntity.class);
    this.parameters = this.getAgentsForType(ParameterAgent.class);

    Map<String, ObservationEntity> observationAgents = this.observations.stream()
        .collect(Collectors.toMap(ObservationEntity::getAttributeName, Function.identity()));

    for (MeasureEntity measureAgent : this.measures) {
      ObservationEntity obsAgent = observationAgents.get(measureAgent.getAttributeName());
      ObjectiveAgent objAgent = new ObjectiveAgent(obsAgent.getAttributeName(), measureAgent, obsAgent);

      Logger.info(String.format("Creating objective for measure \"%s\" and observation \"%s\".",
          measureAgent.getAttributeName(), obsAgent.getAttributeName()));

      this.addAgent(objAgent);
    }
    this.objectives = this.getAgentsForType(ObjectiveAgent.class);

//    this.parameters.forEach(p -> p
//        .setInitialSensitivities(this.objectives.stream().collect(Collectors.toMap(Function.identity(), o -> 1.0))));
//    this.objectives.forEach(o -> o
//        .setInitialSensitivities(this.parameters.stream().collect(Collectors.toMap(Function.identity(), p -> 1.0))));
    // TEMP fixed values for testing
    this.parameters.forEach(
        p -> p.setInitialSensitivities(this.objectives.stream().collect(Collectors.toMap(Function.identity(), o -> {
          switch (o.getName()) {
            case "out_1":
              if (p.getAttributeName().equals("param_x")) {
                return 2.0;
              } else {
                return -3.0;
              }
            case "out_2":
              if (p.getAttributeName().equals("param_x")) {
                return -5.0;
              } else {
                return 1.0;
              }
          }
          throw new RuntimeException(); // Should not happen
        }))));
    this.objectives.forEach(
        o -> o.setInitialSensitivities(this.parameters.stream().collect(Collectors.toMap(Function.identity(), p -> {
          switch (o.getName()) {
            case "out_1":
              if (p.getAttributeName().equals("param_x")) {
                return 2.0;
              } else {
                return -3.0;
              }
            case "out_2":
              if (p.getAttributeName().equals("param_x")) {
                return -5.0;
              } else {
                return 1.0;
              }
          }
          throw new RuntimeException(); // Should not happen
        }))));

    this.cycle = 0;

    Logger.info("CALICOBA set up finished.");
  }

  /**
   * Returns a list of all alive agents of the given type.
   *
   * @param <T>  Agents’ type.
   * @param type Agents’ class.
   * @return The list of agents for the given type.
   */
  @SuppressWarnings("unchecked")
  public <T extends Agent> List<T> getAgentsForType(Class<T> type) {
    if (!this.agentsRegistry.containsKey(type)) {
      return Collections.emptyList();
    }
    return (List<T>) new ArrayList<>(this.agentsRegistry.get(type));
  }

  /**
   * Return the agent with the given ID.
   *
   * @param agentId The agent’s ID.
   * @return The agent or null if not match.
   */
  public Agent getAgentById(String agentId) {
    return this.agentsIdsRegistry.get(agentId);
  }

  /**
   * Returns the first agent matching the given filter.
   *
   * @param filter The filter the agent has to match.
   * @return The first agent that fulfills the filter, or null if none did.
   */
  public Agent getAgent(Predicate<Agent> filter) {
    return this.agentsIdsRegistry.values().stream().filter(filter).findFirst().orElse(null);
  }

  /**
   * Adds an agent to this world and sets its ID.
   *
   * @param agent The agent to add.
   */
  public void addAgent(Agent agent) {
    Class<? extends Agent> agentClass = agent.getClass();

    if (!this.globalIds.containsKey(agentClass)) {
      this.globalIds.put(agentClass, 0);
    }
    this.globalIds.put(agentClass, this.globalIds.get(agentClass) + 1);

    if (!this.agentsRegistry.containsKey(agentClass)) {
      this.agentsRegistry.put(agentClass, new LinkedList<>());
    }
    this.agentsRegistry.get(agentClass).add(agent);

    this.agentsIdsRegistry.put(agent.getId(), agent);

    agent.setId(String.format("%s_%d", agentClass.getSimpleName(), this.globalIds.get(agentClass)));
    agent.setWorld(this);
  }

  /**
   * Runs the simulation for 1 step. If a custom schedule has been set, for each
   * agent type, all agents of said type perceive their environment then decide
   * and act. Otherwise, all agents (in creation order) perceive their environment
   * then they decide and act.
   */
  public void step() {
    if (this.stepCountdown == 0) {
      Logger.info(String.format("Cycle %d", this.cycle));

      boolean reset = this.shouldReset.getValue();

      if (!reset) {
        // Shuffle parameter agents list to avoid insertion order bias
        Collections.shuffle(this.parameters);

        this.measures.forEach(MeasureEntity::perceive);
        this.measures.forEach(MeasureEntity::decideAndAct);

        this.observations.forEach(ObservationEntity::perceive);
        this.observations.forEach(ObservationEntity::decideAndAct);

        // Update objective agents
        this.objectives.forEach(ObjectiveAgent::perceive);
        this.objectives.forEach(ObjectiveAgent::decideAndAct);

        // Update parameter agents
        this.parameters.forEach(ParameterAgent::perceive);
        this.parameters.forEach(ParameterAgent::decideAndAct);

        reset = this.resetFlag;
      }

      if (reset) {
        this.reset.setValue(true);
        this.resetFlag = false;
      }

      this.stepCountdown = this.stepInterval;
      this.cycle++;
    } else {
      this.stepCountdown--;
      Logger.debug(String.format("Waiting. %d steps remaining.", this.stepCountdown));
    }
  }
}
