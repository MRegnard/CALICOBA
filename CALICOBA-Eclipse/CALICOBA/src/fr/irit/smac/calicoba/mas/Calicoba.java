package fr.irit.smac.calicoba.mas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.mas.agents.Agent;
import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;
import fr.irit.smac.calicoba.mas.agents.OutputAgent;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.calicoba.mas.agents.criticality.CriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProviderSetter;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.util.Logger;
import fr.irit.smac.util.QuadFunction;

/**
 * Main class of CALICOBA.
 *
 * @author Damien Vergnet
 */
public class Calicoba {
  public static final String ROOT_OUTPUT_DIR = System.getProperty("user.dir") + File.separator + "calicoba_output"
      + File.separator;

  /** The list of last attributed IDs for each agent type. */
  private Map<Class<? extends Agent>, Integer> globalIds;
  /** Registry of all alive agents by type. */
  private Map<Class<? extends Agent>, List<Agent>> agentsRegistry;
  /** Registry of all alive agents by ID. */
  private Map<String, Agent> agentsIdsRegistry;
  /** Current simulation cycle. */
  private int cycle;

  /** Output agents list for easier access. */
  private List<OutputAgent> outputAgents;
  /** Parameter agents list for easier access. */
  private List<ParameterAgent> parameterAgents;
  /** Objective agents list for easier access. */
  private List<ObjectiveAgent> objectiveAgents;

  private int steps;

  private final Random rng;

  private final String dumpDirectory;
  private final boolean dumpCSV;

  private final boolean learnInfluences;
  private final double alpha;

  private final boolean manualActions;

  private QuadFunction<String, Double, String, Double, Double> getInfluenceForParamAndObj;

  /**
   * Creates a new instance of CALICOBA.
   * 
   * @param dump If true, agents will dump data to CSV files.
   */
  public Calicoba(final boolean dump, final String dumpDir, final boolean learnInfluences, final double alpha,
      final boolean manualActions) {
    this.globalIds = new HashMap<>();
    this.agentsRegistry = new HashMap<>();
    this.agentsIdsRegistry = new HashMap<>();
    this.rng = new Random();
    this.dumpCSV = dump;
    this.dumpDirectory = dumpDir;
    this.learnInfluences = learnInfluences;
    if (alpha < 0 || alpha > 1) {
      throw new IllegalArgumentException("alpha must be in [0, 1]");
    }
    this.alpha = alpha;
    this.manualActions = manualActions;

    if (dump) {
      Path path = Paths.get(this.dumpDirectory());
      if (!Files.exists(path)) {
        try {
          Files.createDirectories(path);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public void setRNGSeed(long seed) {
    this.rng.setSeed(seed);
  }

  public QuadFunction<String, Double, String, Double, Double> getInfluenceFunction() {
    return this.getInfluenceForParamAndObj;
  }

  /**
   * The passed function will be used by each parameter agent to get their true
   * influence on each objective. Its first two parameter are the parameter
   * agent’s name and current value; the two last are the objective agent’s name
   * and current criticality.
   */
  public void setInfluenceFunction(QuadFunction<String, Double, String, Double, Double> f) {
    this.getInfluenceForParamAndObj = f;
  }

  public boolean learnsInfluences() {
    return this.learnInfluences;
  }

  public double getAlpha() {
    return this.alpha;
  }

  public boolean manualActions() {
    return this.manualActions;
  }

  public String dumpDirectory() {
    return ROOT_OUTPUT_DIR + (this.dumpDirectory != null ? this.dumpDirectory + File.separator : "");
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
   * @return True if the system can dump data to files.
   */
  public boolean canDumpData() {
    return this.dumpCSV;
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
   * Creates a new output agent for the given target model output.
   *
   * @param output An output of the target model.
   */
  public void addOutput(final ReadableModelAttribute<Double, IValueProvider<Double>> output) {
    Logger.info(String.format("Creating output \"%s\".", output.getName()));
    this.addAgent(new OutputAgent(output));
  }

  /**
   * Creates a new objective agent. The function’s parameter names must correspond
   * to output agents’ names.
   * 
   * @param name     Name of the new agent.
   * @param function The criticality function.
   */
  public void addObjective(final String name, final CriticalityFunction function) {
    Logger.info(String.format("Creating objective \"%s\".", name));

    List<OutputAgent> relativeAgent = function.getParameterNames().stream() //
        .map(n -> (OutputAgent) this
            .getAgent(a -> a instanceof OutputAgent && ((OutputAgent) a).getAttributeName().equals(n))
            .orElseThrow(() -> new RuntimeException(String.format("agent with name '%s' does not exist", n)))) //
        .collect(Collectors.toList());

    this.addAgent(new ObjectiveAgent(name, function, relativeAgent.toArray(new OutputAgent[relativeAgent.size()])));
  }

  /**
   * Sets up the simulation.
   */
  public void setup() {
    Logger.info("Setting up CALICOBA…");

    this.outputAgents = this.getAgentsForType(OutputAgent.class);
    this.parameterAgents = this.getAgentsForType(ParameterAgent.class);

    this.objectiveAgents = this.getAgentsForType(ObjectiveAgent.class);

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
      this.outputAgents.forEach(OutputAgent::perceive);
      this.parameterAgents.forEach(ParameterAgent::perceive);
      this.objectiveAgents.forEach(ObjectiveAgent::perceive);

      this.objectiveAgents.forEach(ObjectiveAgent::decideAndAct);
      this.outputAgents.forEach(OutputAgent::decideAndAct);

      // DEBUG
      this.parameterAgents
          .forEach(pa -> Logger.debug(String.format("Param %s: %f", pa.getAttributeName(), pa.getAttributeValue())));
      this.objectiveAgents.forEach(oa -> Logger.debug(String.format("Obj %s: %f", oa.getName(), oa.getCriticality())));

      this.parameterAgents.forEach(ParameterAgent::decideAndAct);

      this.cycle++;
      this.steps = 0;
    } else {
      this.steps--;
    }
  }
}
