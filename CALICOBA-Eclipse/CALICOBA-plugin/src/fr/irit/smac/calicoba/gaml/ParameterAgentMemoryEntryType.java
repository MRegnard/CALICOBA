package fr.irit.smac.calicoba.gaml;

import java.util.Collections;

import fr.irit.smac.calicoba.mas.agents.ParameterAgentMemoryEntry;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.type;
import msi.gama.precompiler.ISymbolKind;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.types.GamaType;

@type( //
    name = ICustomTypes.PARAMETER_MEMORY_ENTRY, //
    id = ICustomTypes.PARAMETER_MEMORY_ENTRY_ID, //
    wraps = { ParameterAgentMemoryEntry.class }, //
    kind = ISymbolKind.Variable.CONTAINER, //
    doc = @doc("A memory entry associates an action on a parameter to its effect on the objectives.") //
)
public class ParameterAgentMemoryEntryType extends GamaType<ParameterAgentMemoryEntry> {
  @Override
  public ParameterAgentMemoryEntry getDefault() {
    return new ParameterAgentMemoryEntry(0, Collections.emptyMap());
  }

  @Override
  public boolean canCastToConst() {
    return true;
  }

  @Override
  @doc("Do not use.")
  public ParameterAgentMemoryEntry cast(IScope scope, Object obj, Object param, boolean copy)
      throws GamaRuntimeException {
    throw GamaRuntimeException.error(
        "Cannot instanciate \"" + ICustomTypes.PARAMETER_MEMORY_ENTRY + "\" objects directly.",
        scope);
  }
}
