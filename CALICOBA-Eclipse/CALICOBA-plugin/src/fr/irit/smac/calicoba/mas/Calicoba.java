package fr.irit.smac.calicoba.mas;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.ReadableAgentAttribute;
import fr.irit.smac.calicoba.WritableAgentAttribute;
import fr.irit.smac.calicoba.mas.agents.MeasureAgent;
import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;
import fr.irit.smac.calicoba.mas.agents.ObservationAgent;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;

/**
 * Main class of CALICOBA.
 * 
 * @author Damien Vergnet
 */
public class Calicoba {
  /** Single instance. */
  private static Calicoba instance;

  /**
   * Tells wether CALICOBA has been initialized.
   * 
   * @return true if and only if CALICOBA has been initialized.
   */
  public static boolean isInitialized() {
    return instance != null;
  }

  /**
   * Initializes CALICOBA.
   */
  public static void init() {
    instance = new Calicoba();
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

  /** The world agents will live in. */
  private World world;

  /**
   * Creates a new instance of CALICOBA.
   */
  private Calicoba() {
    this.world = new World(
        Arrays.asList(MeasureAgent.class, ObservationAgent.class, ObjectiveAgent.class, ParameterAgent.class));
  }

  /**
   * Creates an new agent for the given target model parameter.
   * 
   * @param parameter A parameter of the target model.
   * @param isFloat   Wether the parameter is a float or an int.
   */
  public void addParameter(WritableAgentAttribute<Double> parameter, boolean isFloat) {
    this.world.addAgent(new ParameterAgent(parameter, isFloat));
  }

  /**
   * Creates a new agent for the given target model output.
   * 
   * @param measure An output of the target model.
   */
  public void addMeasure(ReadableAgentAttribute<Double> measure) {
    this.world.addAgent(new MeasureAgent(measure));
  }

  /**
   * Creates a new agent for the given reference system output.
   * 
   * @param observation An output of the reference system.
   */
  public void addObservation(ReadableAgentAttribute<Double> observation) {
    this.world.addAgent(new ObservationAgent(observation));
  }

  /**
   * Sets up the simulation. Creates links between agents, creates new agents when
   * needed and adds them to the world.
   */
  public void setup() {
    // DEBUG
    System.out.println("Calicoba.setup");
    Map<String, ObservationAgent> observationAgents = this.world.getAgentsForType(ObservationAgent.class).stream()
        .collect(Collectors.toMap(ObservationAgent::getAttributeName, Function.identity()));

    for (MeasureAgent measureAgent : this.world.getAgentsForType(MeasureAgent.class)) {
      ObservationAgent obsAgent = observationAgents.get(measureAgent.getAttributeName());
      ObjectiveAgent objAgent = new ObjectiveAgent(measureAgent, obsAgent);

      obsAgent.addTargetAgent(objAgent);
      measureAgent.addTargetAgent(objAgent);
      this.world.addAgent(objAgent);

      this.world.getAgentsForType(ParameterAgent.class).forEach(a -> {
        measureAgent.addTargetAgent(a);
        objAgent.addParameterAgent(a);
      });
    }
  }

  /**
   * Runs the simulation for 1 step.
   */
  public void step() {
    this.world.step();
  }

  /**
   * @return The world instance.
   */
  public World getWorld() {
    return this.world;
  }
}
