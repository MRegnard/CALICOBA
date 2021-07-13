package fr.irit.smac.calicoba.gaml.skills;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.util.Pair;

import fr.irit.smac.calicoba.gaml.CalicobaSingleton;
import fr.irit.smac.calicoba.gaml.GamaValueProvider;
import fr.irit.smac.calicoba.gaml.Utils;
import fr.irit.smac.calicoba.gaml.WritableGamaValueProvider;
import fr.irit.smac.calicoba.gaml.types.ObjectiveDefinition;
import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.criticality.BaseCriticalityFunction;
import fr.irit.smac.calicoba.mas.agents.criticality.CriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.action;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.skill;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.IMap;
import msi.gaml.descriptions.IExpressionDescription;

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
  /**
   * Initializes this skill. Gets all attributes of the current GAMA agent whose
   * name start with <code>out_</code>, <code>param_</code> or <code>obj_</code>
   * and adds them as measure, parameter or objective agents respectively into
   * CALICOBA.
   *
   * @param scope The current scope.
   */
  @action( //
      name = ICustomSymbols.TARGET_MODEL_INIT, //
      doc = @doc("Initializes the <code>" + ICustomSymbols.TARGET_MODEL_SKILL + "</code> skill.") //
  )
  public void init(final IScope scope) {
    Calicoba calicoba = CalicobaSingleton.instance();

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
          calicoba.addParameter(new WritableModelAttribute<>(new WritableGamaValueProvider<>(agent, attributeName),
              attributeName, minMax.getFirst(), minMax.getSecond()));

        } else if (attributeName.startsWith("out_")) {
          if (attributeType != Double.class) {
            throw GamaRuntimeException.error("Measures should be floats.", scope);
          }
          Pair<Double, Double> minMax = this.getMinMax(agent, attributeName, scope);
          calicoba.addMeasure(new ReadableModelAttribute<>(new GamaValueProvider<>(agent, attributeName), attributeName,
              minMax.getFirst(), minMax.getSecond()));

        } else if (attributeName.startsWith("obj_")) {
          if (attributeType != ObjectiveDefinition.class) {
            throw GamaRuntimeException.error("Objectives should be obj_def objects.", scope);
          }
          objDefs.put(attributeName, (ObjectiveDefinition) attribute.getValue());
        }
      }

      objDefs.entrySet().forEach(e -> {
        ObjectiveDefinition od = e.getValue();
        CriticalityFunction critFunction = new BaseCriticalityFunction(od.getParameterNames()) {
          @Override
          protected double getImpl(final Map<String, Double> parameterValues) {
            // Objective actions parameter names must be the name of an output followed by a
            // single _
            return Utils.callAction(od.getFunctionName(), agent, parameterValues, p -> p + "_");
          }
        };
        calicoba.addObjective(e.getKey(), critFunction);
      });

      calicoba.setInfluenceFunction((pName, pValue, objName, objCrit) -> {
        Map<String, Object> params = new HashMap<>(4);
        params.put("param_name", pName);
        params.put("param_value", pValue);
        params.put("obj_name", objName);
        params.put("obj_value", objCrit);
        // Because agent’s fields cannot be accessed from within the action for some
        // reason…
        params.put("this", agent);
        return Utils.callAction("get_parameter_influence", agent, params);
      });
    } catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }

    super.setInitialized();
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
}
