package fr.irit.smac.calicoba.gaml;

import java.util.Collections;

import fr.irit.smac.calicoba.mas.agents.ModelState;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.type;
import msi.gama.precompiler.ISymbolKind;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.types.GamaType;

@type( //
    name = ICustomTypes.PARAMETER_CONTEXT, //
    id = ICustomTypes.PARAMETER_CONTEXT_ID, //
    wraps = { ModelState.class }, //
    kind = ISymbolKind.Variable.CONTAINER, //
    doc = @doc("A triplet is a container that holds 3 values.") //
)
public class ParameterAgentContextType extends GamaType<ModelState> {
  @Override
  public ModelState getDefault() {
    return new ModelState(Collections.emptyMap(), Collections.emptyMap());
  }

  @Override
  public boolean canCastToConst() {
    return false;
  }

  @Override
  @doc("Do not use.")
  public ModelState cast(IScope scope, Object obj, Object param, boolean copy) throws GamaRuntimeException {
    throw GamaRuntimeException.error("Cannot instanciate \"" + ICustomTypes.PARAMETER_CONTEXT + "\" objects directly.",
        scope);
  }
}
