package fr.irit.smac.calicoba.gaml;

import java.util.Map;
import java.util.function.Supplier;

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
@skill(name = "calicoba_target_model", doc = @doc("Skill for the CALICOBA target model."))
public class TargetModelSkill extends ModelSkill {
  private static final String PARAMETER_NAME_ARG = "parameter_name";

  /**
   * Initializes this skill. Gets all attributes of the current GAMA agent whose
   * name start with <code>out_</code> or <code>param_</code> and adds them as
   * measure or parameter attributes respectively into CALICOBA.
   * 
   * @param scope The current scope.
   */
  @action(name = "model_init", doc = @doc("Initializes the <code>target_model</code> skill."))
  public void init(final IScope scope) {
    super.init();

    IAgent agent = this.getCurrentAgent(scope);
    IMap<String, Object> attributes = agent.getOrCreateAttributes();

    try {
      for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
        String attributeName = attribute.getKey();
        Class<?> attributeType = attribute.getValue().getClass();

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
      name = "get_parameter_action", //
      doc = @doc("Returns the action performed by the given parameter of the <code>target_model</code>."), //
      args = { //
          @arg( //
              name = PARAMETER_NAME_ARG, //
              type = IType.STRING, //
              optional = false, //
              doc = @doc("Name of the parameter.") //
          ), //
      } //
  )
  public double getParameterAction(final IScope scope) {
    String parameterName = scope.getStringArg(PARAMETER_NAME_ARG);
    ParameterAgent agent;

    try {
      Supplier<GamaRuntimeException> s = //
          () -> GamaRuntimeException.error(String.format("'%s' is not a parameter", parameterName), scope);
      agent = Calicoba.instance().getWorld().getAgentsForType(ParameterAgent.class).stream()
          .filter(a -> a.getAttributeName().equals(parameterName)).findFirst().orElseThrow(s);
    }
    catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }

    return agent.getLastAction();
  }
}
