package fr.irit.smac.calicoba.mas;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.ReadableAgentAttribute;
import fr.irit.smac.calicoba.WritableAgentAttribute;
import fr.irit.smac.calicoba.mas.agents.CurrentSituationAgent;
import fr.irit.smac.calicoba.mas.agents.MeasureAgent;
import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;
import fr.irit.smac.calicoba.mas.agents.ObservationAgent;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.util.Logger;

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

  /** The world agents will live in. */
  private World world;

  /**
   * Creates a new instance of CALICOBA.
   *
   * @param stepInterval The number of GAMA iterations between each step.
   */
  private Calicoba(int stepInterval) {
    this.world = new World(
        Arrays.asList(MeasureAgent.class, ObservationAgent.class, ObjectiveAgent.class, ParameterAgent.class),
        stepInterval);
  }

  /**
   * Creates an new agent for the given target model parameter.
   *
   * @param parameter A parameter of the target model.
   * @param isFloat   Wether the parameter is a float or an int.
   */
  public void addParameter(WritableAgentAttribute parameter, boolean isFloat) {
    Logger.info(String.format("Creating parameter \"%s\".", parameter.getName()));
    this.world.addAgent(new ParameterAgent(parameter, isFloat));
  }

  /**
   * Creates a new agent for the given target model output.
   *
   * @param measure An output of the target model.
   */
  public void addMeasure(ReadableAgentAttribute measure) {
    Logger.info(String.format("Creating measure \"%s\".", measure.getName()));
    this.world.addAgent(new MeasureAgent(measure));
  }

  /**
   * Creates a new agent for the given reference system output.
   *
   * @param observation An output of the reference system.
   */
  public void addObservation(ReadableAgentAttribute observation) {
    Logger.info(String.format("Creating observation \"%s\".", observation.getName()));
    this.world.addAgent(new ObservationAgent(observation));
  }

  /**
   * Sets up the simulation. Creates links between agents, creates new agents when
   * needed and adds them to the world.
   */
  public void setup() {
    Logger.info("Setting up CALICOBA…");
    Map<String, ObservationAgent> observationAgents = this.world.getAgentsForType(ObservationAgent.class).stream()
        .collect(Collectors.toMap(ObservationAgent::getAttributeName, Function.identity()));

    for (MeasureAgent measureAgent : this.world.getAgentsForType(MeasureAgent.class)) {
      ObservationAgent obsAgent = observationAgents.get(measureAgent.getAttributeName());
      ObjectiveAgent objAgent = new ObjectiveAgent(obsAgent.getAttributeName(), measureAgent, obsAgent);

      Logger.info(String.format("Creating objective for measure \"%s\" and observation \"%s\".",
          measureAgent.getAttributeName(), obsAgent.getAttributeName()));

      this.world.addAgent(objAgent);
    }

    CurrentSituationAgent currentSituation = new CurrentSituationAgent();
    this.getWorld().addAgent(currentSituation);

    Logger.info("CALICOBA set up finished.");
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
