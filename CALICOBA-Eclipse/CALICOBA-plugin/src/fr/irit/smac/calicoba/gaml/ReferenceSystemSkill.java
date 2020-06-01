package fr.irit.smac.calicoba.gaml;

import java.util.Map;

import fr.irit.smac.calicoba.ReadableAgentAttribute;
import fr.irit.smac.calicoba.mas.Calicoba;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.action;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.skill;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.IMap;

/**
 * Skill that defines an agent as a reference system. There should be only one
 * species at a time with this skill in a simulation. Only one agent from a
 * species with this skill must be alive at a time.
 * 
 * @author Damien Vergnet
 */
@skill( //
    name = ICustomSymbols.REFERENCE_SYSTEM_SKILL, //
    doc = @doc("Skill for the CALICOBA reference system.") //
)
public class ReferenceSystemSkill extends ModelSkill {
  /**
   * Initializes this skill. Gets all attributes of the current GAMA agent whose
   * name start with <code>out_</code> and adds them as observation attributes
   * into CALICOBA.
   * 
   * @param scope The current scope.
   */
  @action( //
      name = ICustomSymbols.REFERENCE_SYSTEM_INIT, //
      doc = @doc("Initializes the <code>" + ICustomSymbols.REFERENCE_SYSTEM_SKILL + "</code> skill.") //
  )
  public void init(final IScope scope) {
    super.init();

    IAgent agent = this.getCurrentAgent(scope);
    IMap<String, Object> attributes = agent.getOrCreateAttributes();

    for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
      String attributeName = attribute.getKey();
      Class<?> attributeType = attribute.getValue().getClass();

      if (attributeName.startsWith("out_")) {
        if (!Number.class.isAssignableFrom(attributeType)) {
          throw GamaRuntimeException.error("Observation variables should be numbers.", scope);
        }
        try {
          ReadableAgentAttribute<Double> attr = //
              new ReadableAgentAttribute<Double>(agent, attributeName, Double.class);
          Calicoba.instance().addObservation(attr);
        }
        catch (RuntimeException e) {
          throw GamaRuntimeException.create(e, scope);
        }
      }
    }
  }
}
