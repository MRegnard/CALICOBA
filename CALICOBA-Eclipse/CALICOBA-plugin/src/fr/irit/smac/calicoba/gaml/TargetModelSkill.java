package fr.irit.smac.calicoba.gaml;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;

import fr.irit.smac.calicoba.ReadableAgentAttribute;
import fr.irit.smac.calicoba.WritableAgentAttribute;
import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.calicoba.mas.agents.ParameterAgentContext;
import fr.irit.smac.calicoba.mas.agents.ParameterAgentMemoryEntry;
import fr.irit.smac.util.Triplet;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.action;
import msi.gama.precompiler.GamlAnnotations.arg;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.skill;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaListFactory;
import msi.gama.util.GamaMapFactory;
import msi.gama.util.IList;
import msi.gama.util.IMap;
import msi.gaml.types.IType;
import msi.gaml.types.Types;

/**
 * Skill that defines an agent as a target model. There should be only one
 * species at a time with this skill in a simulation. Only one agent from a
 * species with this skill must be alive at a time.
 * 
 * @author Damien Vergnet
 */
@skill( //
    name = ICustomSymbols.TARGET_MODEL_SKILL, //
    doc = @doc("Skill for the CALICOBA target model.") //
)
public class TargetModelSkill extends ModelSkill {
  private static final String PARAMETER_NAME = "parameter_name";
  private static final String SELECTION_STEP = "selection_step";

  /**
   * Initializes this skill. Gets all attributes of the current GAMA agent whose
   * name start with <code>out_</code> or <code>param_</code> and adds them as
   * measure or parameter attributes respectively into CALICOBA.
   * 
   * @param scope The current scope.
   */
  @action( //
      name = ICustomSymbols.TARGET_MODEL_INIT, //
      doc = @doc("Initializes the <code>" + ICustomSymbols.TARGET_MODEL_SKILL + "</code> skill.") //
  )
  public void init(final IScope scope) {
    super.init();

    final IAgent agent = this.getCurrentAgent(scope);
    final IMap<String, Object> attributes = agent.getOrCreateAttributes();

    try {
      for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
        final String attributeName = attribute.getKey();
        final Class<?> attributeType = attribute.getValue().getClass();

        if (attributeName.startsWith("param_")) {
          if (!Number.class.isAssignableFrom(attributeType)) {
            throw GamaRuntimeException.error("Parameters should be numbers.", scope);
          }
          Calicoba.instance().addParameter(new WritableAgentAttribute<>(agent, attributeName, Double.class),
              attributeType == Double.class);
        }
        if (attributeName.startsWith("out_")) {
          if (!Number.class.isAssignableFrom(attributeType)) {
            throw GamaRuntimeException.error("Parameters should be numbers.", scope);
          }
          Calicoba.instance().addMeasure(new ReadableAgentAttribute<>(agent, attributeName, Double.class));
        }
      }
    }
    catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }
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
      name = ICustomSymbols.TARGET_MODEL_GET_PARAM_ACTION, //
      doc = @doc("Returns the action performed by the given parameter of the <code>" + ICustomSymbols.TARGET_MODEL_SKILL
          + "</code>."), //
      args = { //
          @arg( //
              name = PARAMETER_NAME, //
              type = IType.STRING, //
              optional = false, //
              doc = @doc("Name of the parameter.") //
          ), //
      } //
  )
  public Triplet<Double, String, Double> getParameterAction(final IScope scope) {
    final String paramName = scope.getStringArg(PARAMETER_NAME);
    final ParameterAgent agent = this.getParameterAgent(paramName, scope);
    Triplet<Double, Optional<String>, Optional<Double>> t = agent.getLastAction();
    Triplet<Double, String, Double> res = new Triplet<>(t.getFirst(), t.getSecond().orElse(null),
        t.getThird().orElse(Double.NaN));

    return res;
  }

  /**
   * Returns the memory of the Parameter Agent for the given parameter.
   * 
   * @param scope The current scope.
   * @return The memory of the agent.
   */
  @action( //
      name = ICustomSymbols.GET_PARAMETER_MEMORY, //
      args = { //
          @arg( //
              name = PARAMETER_NAME, //
              type = IType.STRING, //
              optional = false, //
              doc = @doc("Name of the parameter.") //
          )
      }, //
      doc = @doc("Returns the memory of the Parameter Agent for the given parameter.") //
  )
  public IMap<ParameterAgentContext, ParameterAgentMemoryEntry> getParameterMemory(final IScope scope) {
    final String paramName = scope.getStringArg(PARAMETER_NAME);
    final ParameterAgent agent = this.getParameterAgent(paramName, scope);

    return GamaMapFactory.wrap(Types.get(ICustomTypes.PARAMETER_CONTEXT),
        Types.get(ICustomTypes.PARAMETER_MEMORY_ENTRY), agent.getMemory());
  }

  /**
   * TODO
   * 
   * @param scope The current scope.
   * @return TODO
   */
  @action( //
      name = ICustomSymbols.GET_PARAMETER_SELECTION, //
      args = { //
          @arg( //
              name = PARAMETER_NAME, //
              type = IType.STRING, //
              optional = false, //
              doc = @doc("Name of the parameter.") //
          ), //
          @arg( //
              name = SELECTION_STEP, //
              type = IType.INT, //
              optional = false, //
              doc = @doc("Step of the custom KNN selection process.") //
          )
      }, //
      doc = @doc("Returns the selected memory entries of the given parameter.") //
  )
  public IList<Double> getParameterSelection(final IScope scope) {
    final int selectionStep = scope.getIntArg(SELECTION_STEP);
    final String paramName = scope.getStringArg(PARAMETER_NAME);
    final ParameterAgent agent = this.getParameterAgent(paramName, scope);

    Map<ParameterAgentContext, Pair<Double, ParameterAgentMemoryEntry>> m;

    switch (selectionStep) {
      case 1:
        m = agent.getFirstlySelectedMemoryEntries();
        break;
      case 2:
        m = agent.getSecondlySelectedMemoryEntries();
        break;
      default:
        throw GamaRuntimeException
            .error(String.format("Invalid value %d for parameter %s.", selectionStep, SELECTION_STEP), scope);
    }

    return GamaListFactory.wrap(Types.get(IType.FLOAT),
        m.values().stream().map(v -> v.getFirst()).collect(Collectors.toList()));
  }

  /**
   * TODO
   * 
   * @param scope The current scope.
   * @return TODO
   */
  @action( //
      name = ICustomSymbols.GET_PARAMETER_ACTION_DISTANCE, //
      args = { //
          @arg( //
              name = PARAMETER_NAME, //
              type = IType.STRING, //
              optional = false, //
              doc = @doc("Name of the parameter.") //
          )
      }, //
      doc = @doc("Returns the selected action’s distance of the given parameter.") //
  )
  public double getParameterActionDistance(final IScope scope) {
    final String paramName = scope.getStringArg(PARAMETER_NAME);
    final ParameterAgent agent = this.getParameterAgent(paramName, scope);

    return agent.getSelectedActionDistance();
  }

  private ParameterAgent getParameterAgent(final String paramName, final IScope scope) {
    return Calicoba.instance().getWorld().getAgentsForType(ParameterAgent.class)
        .stream().filter(a -> a.getAttributeName().equals(paramName)).findFirst()
        .orElseThrow(
            () -> GamaRuntimeException.error(String.format("No agent for parameter name \"%s\"", paramName), scope));
  }
}
