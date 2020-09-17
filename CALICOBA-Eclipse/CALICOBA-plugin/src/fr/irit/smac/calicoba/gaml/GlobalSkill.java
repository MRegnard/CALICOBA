package fr.irit.smac.calicoba.gaml;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;
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
  private static final String INIT_STEP_INTERVAL_ARG_NAME = "step_interval";
  private static final String GET_OBJECTIVE_OBJ_NAME_ARG_NAME = "objective_name";

  /**
   * Initializes CALICOBA. Should only be called in the init statement, before any
   * other statement.
   *
   * @param scope The current scope.
   */
  @action( //
      name = ICustomSymbols.CALICOBA_INIT, //
      args = { //
          @arg( //
              name = INIT_STEP_INTERVAL_ARG_NAME, //
              type = IType.INT, //
              optional = false, //
              doc = @doc("The number of GAMA iterations between each step of CALICOBA.") //
          ) }, //
      doc = @doc("Initializes CALICOBA. Must be called <em>before</em> any other statement.") //
  )
  public void init(final IScope scope) {
    Logger.setLevel(Logger.Level.DEBUG);

    super.init();

    try {
      Calicoba.init(scope.getIntArg(INIT_STEP_INTERVAL_ARG_NAME));
    } catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }
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
    try {
      Calicoba.instance().setup();
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
    try {
      Calicoba.instance().step();
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
      name = ICustomSymbols.GET_OBJECTIVES, //
      doc = @doc("Returns the objectives’ values.") //
  )
  public IMap<String, Double> getObjectives(final IScope scope) {
    Map<String, Double> objectives;

    try {
      objectives = Calicoba.instance().getAgentsForType(ObjectiveAgent.class).stream()
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
      name = ICustomSymbols.GET_OBJECTIVE, //
      doc = @doc("Returns the objective value with the given name."), //
      args = { //
          @arg( //
              name = GET_OBJECTIVE_OBJ_NAME_ARG_NAME, //
              type = IType.STRING, //
              optional = false, //
              doc = @doc("Name of the objective.") //
          ), //
      } //
  )
  public double getObjective(final IScope scope) {
    String objectiveName = scope.getStringArg(GET_OBJECTIVE_OBJ_NAME_ARG_NAME);
    ObjectiveAgent agent;

    try {
      Supplier<GamaRuntimeException> s = //
          () -> GamaRuntimeException.error(String.format("'%s' is not an objective", objectiveName), scope);
      agent = Calicoba.instance().getAgentsForType(ObjectiveAgent.class).stream()
          .filter(a -> a.getName().equals(objectiveName)).findFirst().orElseThrow(s);
    } catch (RuntimeException e) {
      return Double.NaN;
    }

    return agent.getCriticality();
  }
}
