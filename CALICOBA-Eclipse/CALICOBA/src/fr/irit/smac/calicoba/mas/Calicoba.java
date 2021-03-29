package fr.irit.smac.calicoba.mas;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import fr.irit.smac.calicoba.mas.agents.Agent;
import fr.irit.smac.calicoba.mas.agents.AgentWithGamaAttribute;
import fr.irit.smac.calicoba.mas.agents.MeasureAgent;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.calicoba.mas.agents.SatisfactionAgent;
import fr.irit.smac.calicoba.mas.agents.data.CorrelationMatrix;
import fr.irit.smac.calicoba.mas.agents.data.CriticalityFunctionParameters;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProviderSetter;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.util.Logger;

/**
 * Main class of CALICOBA.
 *
 * @author Damien Vergnet
 */
public class Calicoba {
  public static final String OUTPUT_DIR = System.getProperty("user.dir") + File.separator + "calicoba_output"
      + File.separator;

  /** The list of last attributed IDs for each agent type. */
  private Map<Class<? extends Agent>, Integer> globalIds;
  /** Registry of all alive agents by type. */
  private Map<Class<? extends Agent>, List<Agent>> agentsRegistry;
  /** Registry of all alive agents by ID. */
  private Map<String, Agent> agentsIdsRegistry;
  /** Current simulation cycle. */
  private int cycle;

  /** Measure agents list for easier access. */
  private List<MeasureAgent> measureAgents;
  /** Parameter agents list for easier access. */
  private List<ParameterAgent> parameterAgents;
  /** Satisfaction agents list for easier access. */
  private List<SatisfactionAgent> satisfactionAgents;

  /** Correlation matrix used to initialise measure agents. */
  private CorrelationMatrix matrix;

  private int steps;

  private final Random rng;

  /**
   * Creates a new instance of CALICOBA.
   */
  public Calicoba() {
    this.globalIds = new HashMap<>();
    this.agentsRegistry = new HashMap<>();
    this.agentsIdsRegistry = new HashMap<>();
    this.rng = new Random();
  }

  /**
   * @return The current simulation cycle.
   */
  public int getCycle() {
    return this.cycle;
  }

  /**
   * @return The random numbers generator.
   */
  public Random getRNG() {
    return this.rng;
  }

  /**
   * Creates an new parameter agent for the given target model parameter.
   *
   * @param parameter A parameter of the target model.
   */
  public void addParameter(WritableModelAttribute<Double, IValueProviderSetter<Double>> parameter) {
    Logger.info(String.format("Creating parameter \"%s\".", parameter.getName()));
    this.addAgent(new ParameterAgent(parameter));
  }

  /**
   * Creates a new measure agent for the given target model output.
   *
   * @param measure An output of the target model.
   */
  public void addMeasure(final ReadableModelAttribute<Double, IValueProvider<Double>> measure) {
    Logger.info(String.format("Creating measure \"%s\".", measure.getName()));
    this.addAgent(new MeasureAgent(measure));
  }

  /**
   * Creates a new satisfaction agent.
   * 
   * @param name                          Name of the new agent.
   * @param criticalityFunctionParameters Parameters of the agent’s criticality
   *                                      function.
   * @param relativeAgentName             Name of the parameter/measure agent the
   *                                      new agent will send requests to.
   */
  public void addObjective(final String name, final CriticalityFunctionParameters criticalityFunctionParameters,
      final String relativeAgentName) {
    Logger.info(String.format("Creating objective \"%s\".", name));

    AgentWithGamaAttribute<?, ?> relativeAgent = this
        .<AgentWithGamaAttribute<?, ?>>getAgent(a -> a instanceof AgentWithGamaAttribute
            && ((AgentWithGamaAttribute<?, ?>) a).getAttributeName().equals(relativeAgentName))
        .orElseThrow(
            () -> new RuntimeException(String.format("agent with name '%s' does not exist", relativeAgentName)));

    this.addAgent(new SatisfactionAgent(name, relativeAgent, criticalityFunctionParameters));
  }

  /**
   * Defines the correlation matrix for measures and parameters.
   * 
   * @param matrix A map associating measure names to pairs of measure/parameter
   *               names and their corresponding variation direction (> 0 for +, <
   *               0 for -).
   */
  public void setCorrelationMatrix(Map<String, Map<String, Number>> matrix) {
    Map<MeasureAgent, Map<AgentWithGamaAttribute<?, ?>, Boolean>> columns = new HashMap<>();

    for (Map.Entry<String, Map<String, Number>> e : matrix.entrySet()) {
      Map<AgentWithGamaAttribute<?, ?>, Boolean> m = new HashMap<>();

      for (Map.Entry<String, Number> e2 : e.getValue().entrySet()) {
        Optional<AgentWithGamaAttribute<?, ?>> agent = this.getAgent( //
            a -> a instanceof AgentWithGamaAttribute
                && ((AgentWithGamaAttribute<?, ?>) a).getAttributeName().equals(e2.getKey()) //
        );
        double n = e2.getValue().doubleValue();

        if (agent != null && n != 0) {
          agent.ifPresent(a -> m.put(a, n > 0));
        }
      }

      if (!m.isEmpty()) {
        this.getAgentsForType(MeasureAgent.class).stream() //
            .filter(a -> a.getAttributeName().equals(e.getKey())) //
            .findFirst() //
            .ifPresent(a -> columns.put(a, m));
      }
    }

    this.matrix = new CorrelationMatrix(columns);
  }

  /**
   * Sets up the simulation.
   */
  public void setup() {
    Logger.info("Setting up CALICOBA…");

    this.measureAgents = this.getAgentsForType(MeasureAgent.class);
    this.parameterAgents = this.getAgentsForType(ParameterAgent.class);

    this.measureAgents.forEach(a -> a.init(this.matrix));

    this.satisfactionAgents = this.getAgentsForType(SatisfactionAgent.class);

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
  @SuppressWarnings("unchecked")
  public <T extends Agent> Optional<T> getAgent(Predicate<Agent> filter) {
    return (Optional<T>) this.agentsIdsRegistry.values().stream().filter(filter).findFirst();
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

    agent.setId(String.format("%s_%d", agentClass.getSimpleName(), this.globalIds.get(agentClass)));
    this.agentsIdsRegistry.put(agent.getId(), agent);

    agent.setWorld(this);
  }

  /**
   * Runs the system for 1 step.
   */
  public void step() {
    Logger.info(String.format("Cycle %d", this.cycle));
    if (this.steps == 0) {
      this.measureAgents.forEach(MeasureAgent::perceive);
      this.parameterAgents.forEach(ParameterAgent::perceive);
      this.satisfactionAgents.forEach(SatisfactionAgent::perceive);

      this.satisfactionAgents.forEach(SatisfactionAgent::decideAndAct);
      // Loop until no more requests have been sent
      do {
        this.measureAgents.forEach(MeasureAgent::decideAndAct);
      } while (this.measureAgents.stream().anyMatch(MeasureAgent::hasSentRequest));
      this.parameterAgents.forEach(ParameterAgent::decideAndAct);

      this.cycle++;
      this.steps = 0;
    } else {
      this.steps--;
    }
  }

  public SatisfactionAgent getMostCritical() {
    return this.satisfactionAgents.stream() //
        .max((a1, a2) -> Double.compare(a1.getCriticality(), a2.getCriticality())) //
        .get();
  }
}
