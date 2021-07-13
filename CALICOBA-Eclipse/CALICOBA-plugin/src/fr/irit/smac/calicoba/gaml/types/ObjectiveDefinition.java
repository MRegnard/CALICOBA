package fr.irit.smac.calicoba.gaml.types;

import java.util.List;
import java.util.Objects;

import msi.gama.common.interfaces.IValue;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.variable;
import msi.gama.precompiler.GamlAnnotations.vars;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.types.IType;

@vars({ //
    @variable(name = ObjectiveDefinition.PARAM_NAMES, type = IType.LIST), //
    @variable(name = ObjectiveDefinition.FUNCTION_NAME, type = IType.STRING), //
})
@doc("Defines the function for an objective and its parameters.")
public final class ObjectiveDefinition implements IValue {
  public static final String PARAM_NAMES = "parameters";
  public static final String FUNCTION_NAME = "function";

  private final List<String> parameterNames;
  private final String functionName;

  public ObjectiveDefinition(List<String> parameterNames, String functionName) {
    this.parameterNames = Objects.requireNonNull(parameterNames);
    this.functionName = Objects.requireNonNull(functionName);
  }

  public List<String> getParameterNames() {
    return this.parameterNames;
  }

  public String getFunctionName() {
    return this.functionName;
  }

  @Override
  public String toString() {
    return String.format("ObjDef{params=%s}", this.parameterNames);
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
