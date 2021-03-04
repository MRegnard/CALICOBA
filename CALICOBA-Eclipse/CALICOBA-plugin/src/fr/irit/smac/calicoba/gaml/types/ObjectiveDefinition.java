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
    @variable(name = ObjectiveDefinition.Η1, type = IType.FLOAT), //
    @variable(name = ObjectiveDefinition.Ε1, type = IType.FLOAT), //
    @variable(name = ObjectiveDefinition.Ε2, type = IType.FLOAT), //
    @variable(name = ObjectiveDefinition.Η2, type = IType.FLOAT), //
    @variable(name = ObjectiveDefinition.SUP, type = IType.FLOAT), //
})
@doc("Defines the relative attribute and criticality function parameters for an objective.")
public final class ObjectiveDefinition implements IValue {
  public static final String ATTRIBUTE = "attribute";
  public static final String INF = "inf";
  public static final String Η1 = "η1";
  public static final String Ε1 = "ε1";
  public static final String Ε2 = "ε2";
  public static final String Η2 = "η2";
  public static final String SUP = "sup";

  private final String relativeAgentName;
  private final CriticalityFunctionParameters parameters;

  public ObjectiveDefinition(String relativeAgentName, double inf, double ε, double sup) {
    this(relativeAgentName, inf, ε, ε, sup);
  }

  public ObjectiveDefinition(String relativeAgentName, double inf, double ε1, double ε2, double sup) {
    this(relativeAgentName, inf, inf + (ε1 - inf) / 3, ε1, ε2, sup - (sup - ε2) / 3, sup);
  }

  public ObjectiveDefinition(String relativeAgentName, double inf, double η1, double ε1, double ε2, double η2,
      double sup) {
    this.relativeAgentName = relativeAgentName;
    this.parameters = new CriticalityFunctionParameters(inf, η1, ε1, ε2, η2, sup);
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
