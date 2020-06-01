package fr.irit.smac.calicoba.mas.agents;

import java.util.Collections;
import java.util.Map;

import msi.gama.common.interfaces.IValue;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.getter;
import msi.gama.precompiler.GamlAnnotations.variable;
import msi.gama.precompiler.GamlAnnotations.vars;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.types.IType;

/**
 * A parameter agent’s entry is composed of an action and the variations of all
 * objectives after said action was performed.
 * 
 * @author Damien Vergnet
 */
@vars({
    @variable(name = ParameterAgentMemoryEntry.ACTION, type = IType.FLOAT),
    @variable(name = ParameterAgentMemoryEntry.OBJ_VARIATIONS, type = IType.MAP),
})
@doc("A memory entry associates an action on a parameter to its effect on the objectives.")
public class ParameterAgentMemoryEntry implements IValue {
  public static final String ACTION = "action";
  public static final String OBJ_VARIATIONS = "objectives_variations";

  /** The performed action. */
  private final double action;
  /** The variations of all objectives after the action was performed. */
  private final Map<String, Double> objectivesVariations;

  /**
   * Creates a new memory entry.
   * 
   * @param action               The performed action.
   * @param objectivesVariations The variations of the objectives after the action
   *                             was performed.
   */
  public ParameterAgentMemoryEntry(double action, Map<String, Double> objectivesVariations) {
    this.action = action;
    this.objectivesVariations = Collections.unmodifiableMap(objectivesVariations);
  }

  /**
   * @return The action.
   */
  @getter(ACTION)
  public double getAction() {
    return this.action;
  }

  /**
   * @return The variations of all objectives after the action was performed.
   */
  @getter(OBJ_VARIATIONS)
  public Map<String, Double> getObjectivesVariations() {
    return this.objectivesVariations;
  }

  @Override
  public String toString() {
    return String.format("MemoryEntry{action=%f,obj_variations=%s}", this.action, this.objectivesVariations);
  }

  @Override
  public String stringValue(final IScope scope) throws GamaRuntimeException {
    return this.toString();
  }

  @Override
  public IValue copy(final IScope scope) throws GamaRuntimeException {
    // TODO Auto-generated method stub
    return null;
  }
}
