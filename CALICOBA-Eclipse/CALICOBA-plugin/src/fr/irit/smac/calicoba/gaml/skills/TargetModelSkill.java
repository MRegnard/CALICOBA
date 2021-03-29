package fr.irit.smac.calicoba.gaml.skills;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.util.Pair;

import fr.irit.smac.calicoba.gaml.CalicobaSingleton;
import fr.irit.smac.calicoba.gaml.GamaValueProvider;
import fr.irit.smac.calicoba.gaml.WritableGamaValueProvider;
import fr.irit.smac.calicoba.gaml.types.ObjectiveDefinition;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.action;
import msi.gama.precompiler.GamlAnnotations.arg;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.skill;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaMap;
import msi.gama.util.IMap;
import msi.gaml.descriptions.IExpressionDescription;
import msi.gaml.types.IType;

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

  /**
   * Initializes this skill. Gets all attributes of the current GAMA agent whose
   * name start with <code>out_</code> or <code>param_</code> and adds them as
   * measure or parameter attributes respectively into CALICOBA.
   *
   * @param scope The current scope.
   */
  @SuppressWarnings("unchecked")
  @action( //
      name = ICustomSymbols.TARGET_MODEL_INIT, //
      doc = @doc("Initializes the <code>" + ICustomSymbols.TARGET_MODEL_SKILL + "</code> skill.") //
  )
  public void init(final IScope scope) {
    super.init();

    final IAgent agent = this.getCurrentAgent(scope);
    final IMap<String, Object> attributes = agent.getOrCreateAttributes();
    Map<String, ObjectiveDefinition> objDefs = new HashMap<>();

    try {
      for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
        final String attributeName = attribute.getKey();
        final Class<?> attributeType = attribute.getValue().getClass();

        if (attributeName.startsWith("param_")) {
          if (attributeType != Double.class) {
            throw GamaRuntimeException.error("Parameters should be floats.", scope);
          }
          Pair<Double, Double> minMax = this.getMinMax(agent, attributeName, scope);
          CalicobaSingleton.instance()
              .addParameter(new WritableModelAttribute<>(new WritableGamaValueProvider<>(agent, attributeName),
                  attributeName, minMax.getFirst(), minMax.getSecond()));

        } else if (attributeName.startsWith("out_")) {
          if (attributeType != Double.class) {
            throw GamaRuntimeException.error("Measures should be floats.", scope);
          }
          Pair<Double, Double> minMax = this.getMinMax(agent, attributeName, scope);
          CalicobaSingleton.instance().addMeasure(new ReadableModelAttribute<>(
              new GamaValueProvider<>(agent, attributeName), attributeName, minMax.getFirst(), minMax.getSecond()));

        } else if (attributeName.startsWith("obj_")) {
          if (attributeType != ObjectiveDefinition.class) {
            throw GamaRuntimeException.error("Objectives should be obj_def objects.", scope);
          }
          objDefs.put(attributeName, (ObjectiveDefinition) attribute.getValue());
        }
      }

      objDefs.entrySet().forEach(e -> CalicobaSingleton.instance().addObjective(e.getKey(),
          e.getValue().getParameters(), e.getValue().getRelativeAgentName()));

      Object correlationMatrix = attributes.get("corr_matrix");
      if (correlationMatrix != null) {
        if (!(correlationMatrix instanceof GamaMap)) {
          throw GamaRuntimeException.error("Correlation matrix must be a map.", scope);
        }
        CalicobaSingleton.instance().setCorrelationMatrix((Map<String, Map<String, Number>>) correlationMatrix);
      }
    } catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }
  }

  private Pair<Double, Double> getMinMax(IAgent agent, String attributeName, IScope scope) {
    return new Pair<>(this.getAttributeFacetValue(agent, attributeName, "min", -Double.MAX_VALUE, scope),
        this.getAttributeFacetValue(agent, attributeName, "max", Double.MAX_VALUE, scope));
  }

  @SuppressWarnings("unchecked")
  private <T> T getAttributeFacetValue(IAgent agent, String attributeName, String facetName, T defaultValue,
      IScope scope) {
    // cf https://groups.google.com/g/gama-platform/c/x2v8BmWW2e4/m/-v8rlHyxBAAJ
    IExpressionDescription exprDesc = agent.getSpecies().getDescription().getAttribute(attributeName)
        .getFacet(facetName);

    if (exprDesc != null) {
      return (T) exprDesc.getExpression().value(scope);
    }

    return defaultValue;
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
  public int getParameterAction(final IScope scope) {
    final String paramName = scope.getStringArg(PARAMETER_NAME);
    final ParameterAgent agent = this.getParameterAgent(paramName, scope);
    return agent.getLastAction();
  }

  private ParameterAgent getParameterAgent(final String paramName, final IScope scope) {
    return CalicobaSingleton.instance().getAgentsForType(ParameterAgent.class).stream()
        .filter(a -> a.getAttributeName().equals(paramName)).findFirst().orElseThrow(
            () -> GamaRuntimeException.error(String.format("No agent for parameter name \"%s\"", paramName), scope));
  }
}
