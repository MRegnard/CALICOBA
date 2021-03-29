package fr.irit.smac.calicoba.gaml.types;

import fr.irit.smac.calicoba.mas.agents.data.CriticalityFunctionParameters;
import msi.gama.common.interfaces.IValue;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.variable;
import msi.gama.precompiler.GamlAnnotations.vars;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.types.IType;

@vars({ //
    @variable(name = ObjectiveDefinition.ATTRIBUTE, type = IType.STRING), //
    @variable(name = ObjectiveDefinition.INF, type = IType.FLOAT), //
    @variable(name = ObjectiveDefinition.INFL1, type = IType.FLOAT), //
    @variable(name = ObjectiveDefinition.NULL_MIN, type = IType.FLOAT), //
    @variable(name = ObjectiveDefinition.NULL_MAX, type = IType.FLOAT), //
    @variable(name = ObjectiveDefinition.INFL2, type = IType.FLOAT), //
    @variable(name = ObjectiveDefinition.SUP, type = IType.FLOAT), //
})
@doc("Defines the relative attribute and criticality function parameters for an objective.")
public final class ObjectiveDefinition implements IValue {
  public static final String ATTRIBUTE = "attribute";
  public static final String INF = "inf";
  public static final String INFL1 = "infl1";
  public static final String NULL_MIN = "nullMin";
  public static final String NULL_MAX = "nullMax";
  public static final String INFL2 = "infl2";
  public static final String SUP = "sup";

  private final String relativeAgentName;
  private final CriticalityFunctionParameters parameters;

  public ObjectiveDefinition(String relativeAgentName, double inf, double nullValue, double sup) {
    this(relativeAgentName, inf, nullValue, nullValue, sup);
  }

  public ObjectiveDefinition(String relativeAgentName, double inf, double nullMin, double nullMax, double sup) {
    this(relativeAgentName, inf, inf + (nullMin - inf) / 3, nullMin, nullMax, sup - (sup - nullMax) / 3, sup);
  }

  public ObjectiveDefinition(String relativeAgentName, double inf, double infl1, double nullMin, double nullMax,
      double infl2, double sup) {
    this.relativeAgentName = relativeAgentName;
    this.parameters = new CriticalityFunctionParameters(inf, infl1, nullMin, nullMax, infl2, sup);
  }

  public String getRelativeAgentName() {
    return this.relativeAgentName;
  }

  public CriticalityFunctionParameters getParameters() {
    return this.parameters;
  }

  @Override
  public String toString() {
    return String.format("ObjDef{relAgent=%s,params=%s}", this.relativeAgentName, this.parameters);
  }

  @Override
  public String stringValue(IScope scope) throws GamaRuntimeException {
    return this.toString();
  }

  @Override
  public IValue copy(IScope scope) throws GamaRuntimeException {
    return this; // Immutable so returns itself
  }
}
