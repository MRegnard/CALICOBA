package fr.irit.smac.calicoba.gaml.skills;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.gaml.CalicobaSingleton;
import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.util.Logger;
import msi.gama.precompiler.GamlAnnotations.action;
import msi.gama.precompiler.GamlAnnotations.arg;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.skill;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaMapFactory;
import msi.gama.util.IMap;
import msi.gaml.types.IType;
import msi.gaml.types.Types;

/**
 * This skill should only be used by the global agent species.
 *
 * @author Damien Vergnet
 */
@skill( //
    name = ICustomSymbols.CALICOBA_SKILL, //
    doc = @doc("Skill for the global agent.") //
)
public class GlobalSkill extends ModelSkill {
  private static final String INIT_ARG_LEARN = "learn";
  private static final String INIT_ARG_ALPHA = "alpha";
  private static final String GET_OBJECTIVE_ARG_OBJ_NAME = "objective_name";
  private static final String GET_PARAM_ACTION_ARG_PARAM_NAME = "parameter_name";
  private static final String GET_INFLUENCE_ARG_PARAM_NAME = "parameter_name";
  private static final String GET_INFLUENCE_ARG_OBJ_NAME = "objective_name";

  /**
   * Initializes CALICOBA. Should only be called in the init statement, before any
   * other statement.
   *
   * @param scope The current scope.
   */
  @action( //
      name = ICustomSymbols.CALICOBA_INIT, //
      doc = @doc("Initializes CALICOBA. Must be called <em>before</em> any other statement."), //
      args = { //
          @arg( //
              name = INIT_ARG_LEARN, //
              type = IType.BOOL, //
              optional = false, //
              doc = @doc("If true, parameter agents will have to learn their influences on objectives.") //
          ), //
          @arg( //
              name = INIT_ARG_ALPHA, //
              type = IType.FLOAT, //
              optional = false, //
              doc = @doc("The value of α for influences estimation.") //
          ) //
      } //
  )
  public void init(final IScope scope) {
    Path path = Paths.get(Calicoba.ROOT_OUTPUT_DIR);
    if (!Files.isDirectory(path)) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        throw GamaRuntimeException.create(e, scope);
      }
    }
    try {
      String fname = Calicoba.ROOT_OUTPUT_DIR + "calicoba.log";
      Logger.info("Log output file: " + fname);
      Logger.setWriter(new FileWriter(fname));
    } catch (IOException e) {
      throw GamaRuntimeException.create(e, scope);
    }
    Logger.setWriterLevel(Logger.Level.INFO);
    Logger.setStdoutLevel(Logger.Level.DEBUG);

    try {
      CalicobaSingleton.init(scope.getBoolArg(INIT_ARG_LEARN), scope.getFloatArg(INIT_ARG_ALPHA));
    } catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }

    super.setInitialized();
  }

  /**
   * Sets up CALICOBA. It should only be called in the init statement, after the
   * target and reference models have been instanciated.
   *
   * @param scope The current scope.
   */
  @action( //
      name = ICustomSymbols.CALICOBA_SETUP, //
      doc = @doc("Sets up CALICOBA. Must be called <em>after</em> target and reference models have been instanciated.") //
  )
  public void setup(final IScope scope) {
    this.checkInitialized(scope);
    try {
      CalicobaSingleton.instance().setup();
    } catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }
  }

  /**
   * Runs CALICOBA for 1 step. Should not be called more than once per cycle.
   *
   * @param scope The current scope.
   */
  @action( //
      name = ICustomSymbols.CALICOBA_STEP, //
      doc = @doc("Runs CALICOBA for 1 step.") //
  )
  public void step(final IScope scope) {
    this.checkInitialized(scope);
    try {
      CalicobaSingleton.instance().step();
    } catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }
  }

  /**
   * Returns the values of all objectives.
   *
   * @param scope The current scope.
   * @return The objectives’ values.
   */
  @action( //
      name = ICustomSymbols.GLOBAL_GET_OBJECTIVES, //
      doc = @doc("Returns the objectives’ values.") //
  )
  public IMap<String, Double> getObjectives(final IScope scope) {
    this.checkInitialized(scope);

    Map<String, Double> objectives;

    try {
      objectives = CalicobaSingleton.instance().getAgentsForType(ObjectiveAgent.class).stream()
          .collect(Collectors.toMap(ObjectiveAgent::getName, ObjectiveAgent::getCriticality));
    } catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }

    return GamaMapFactory.wrap(Types.STRING, Types.FLOAT, objectives);
  }

  /**
   * Returns the value of the given objective.
   *
   * @param scope The current scope.
   * @return The objective value.
   * @throws GamaRuntimeException If the given objective name doesn’t exist.
   */
  @action( //
      name = ICustomSymbols.GLOBAL_GET_OBJECTIVE, //
      doc = @doc("Returns the objective value with the given name."), //
      args = { //
          @arg( //
              name = GET_OBJECTIVE_ARG_OBJ_NAME, //
              type = IType.STRING, //
              optional = false, //
              doc = @doc("Name of the objective.") //
          ), //
      } //
  )
  public double getObjective(final IScope scope) {
    this.checkInitialized(scope);

    String objectiveName = scope.getStringArg(GET_OBJECTIVE_ARG_OBJ_NAME);
    ObjectiveAgent agent;

    try {
      agent = CalicobaSingleton.instance().getAgentsForType(ObjectiveAgent.class).stream() //
          .filter(a -> a.getName().equals(objectiveName)) //
          .findFirst() //
          .orElseThrow(
              () -> GamaRuntimeException.error(String.format("'%s' is not an objective", objectiveName), scope));
    } catch (RuntimeException e) {
      return Double.NaN;
    }

    return agent.getCriticality();
  }

  /**
   * Returns the latest performed action of the agent associated to the given
   * parameter.
   *
   * @param scope The current scope.
   * @return The action.
   * @throws GamaRuntimeException If the given attribute name is not a parameter
   *                              of the target model.
   */
  @action( //
      name = ICustomSymbols.GLOBAL_GET_PARAM_ACTION, //
      doc = @doc("Returns the action performed by the given parameter of the <code>" + ICustomSymbols.TARGET_MODEL_SKILL
          + "</code>."), //
      args = { //
          @arg( //
              name = GET_PARAM_ACTION_ARG_PARAM_NAME, //
              type = IType.STRING, //
              optional = false, //
              doc = @doc("Name of the parameter.") //
          ), //
      } //
  )
  public int getParameterAction(final IScope scope) {
    this.checkInitialized(scope);

    final String paramName = scope.getStringArg(GET_PARAM_ACTION_ARG_PARAM_NAME);
    final ParameterAgent agent = getParameterAgent(paramName, scope);
    return agent.getLastAction();
  }

  /**
   * Returns the influence coefficient of the given parameter/objective pair.
   * 
   * @param scope The current scope.
   * @return The coefficient.
   * @throws GamaRuntimeException If either the parameter or objective name is
   *                              undefined.
   */
  @action( //
      name = ICustomSymbols.GLOBAL_GET_INFLUENCE, //
      doc = @doc("Returns the influence coefficient of the given parameter/objective pair."), //
      args = { //
          @arg( //
              name = GET_INFLUENCE_ARG_PARAM_NAME, //
              type = IType.STRING, //
              optional = false, //
              doc = @doc("Name of the parameter.") //
          ), //
          @arg( //
              name = GET_INFLUENCE_ARG_OBJ_NAME, //
              type = IType.STRING, //
              optional = false, //
              doc = @doc("Name of the objective.") //
          ), //
      } //
  )
  public double getInfluence(final IScope scope) {
    this.checkInitialized(scope);

    String paramName = scope.getStringArg(GET_INFLUENCE_ARG_PARAM_NAME);
    String objName = scope.getStringArg(GET_INFLUENCE_ARG_OBJ_NAME);
    ParameterAgent p = getParameterAgent(paramName, scope);
    try {
      return p.getInfluence(objName);
    } catch (IllegalArgumentException e) {
      if (CalicobaSingleton.instance().getCycle() == 0) {
        return 0; // Parameter agents still haven’t perceived their environment.
      }
      throw GamaRuntimeException.create(e, scope);
    }
  }

  private static ParameterAgent getParameterAgent(final String paramName, final IScope scope)
      throws GamaRuntimeException {
    return CalicobaSingleton.instance() //
        .getAgentsForType(ParameterAgent.class) //
        .stream() //
        .filter(a -> a.getAttributeName().equals(paramName)) //
        .findFirst() //
        .orElseThrow(
            () -> GamaRuntimeException.error(String.format("No agent for parameter name \"%s\"", paramName), scope));
  }
}
