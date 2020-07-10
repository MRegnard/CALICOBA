package fr.irit.smac.calicoba.gaml;

import java.util.Map;

import fr.irit.smac.calicoba.ReadableAgentAttribute;
import fr.irit.smac.calicoba.WritableAgentAttribute;
import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.action;
import msi.gama.precompiler.GamlAnnotations.arg;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.skill;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.IMap;
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
          if (attributeType != Double.class) {
            throw GamaRuntimeException.error("Parameters should be floats.", scope);
          }
          Calicoba.instance().addParameter(new WritableAgentAttribute(agent, attributeName),
              attributeType == Double.class);
        }
        if (attributeName.startsWith("out_")) {
          if (attributeType != Double.class) {
            throw GamaRuntimeException.error("Parameters should be floats.", scope);
          }
          Calicoba.instance().addMeasure(new ReadableAgentAttribute(agent, attributeName));
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
  public Double getParameterAction(final IScope scope) {
    final String paramName = scope.getStringArg(PARAMETER_NAME);
    final ParameterAgent agent = this.getParameterAgent(paramName, scope);
    return agent.getLastAction().getFirst();
  }

  private ParameterAgent getParameterAgent(final String paramName, final IScope scope) {
    return Calicoba.instance().getWorld().getAgentsForType(ParameterAgent.class)
        .stream().filter(a -> a.getAttributeName().equals(paramName)).findFirst()
        .orElseThrow(
            () -> GamaRuntimeException.error(String.format("No agent for parameter name \"%s\"", paramName), scope));
  }
}
