package fr.irit.smac.calicoba.gaml.types;

import java.util.Collections;
import java.util.List;

import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.type;
import msi.gama.precompiler.ISymbolKind;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaList;
import msi.gaml.types.GamaType;

@type( //
    name = ICustomTypes.OBJ_DEF, //
    id = ICustomTypes.OBJ_DEF_ID, //
    wraps = { ObjectiveDefinition.class }, //
    kind = ISymbolKind.Variable.CONTAINER, //
    doc = @doc("This type is used to define an objective.") //
)
public class ObjectiveDefinitionType extends GamaType<ObjectiveDefinition> {
  @Override
  public ObjectiveDefinition getDefault() {
    return new ObjectiveDefinition(Collections.emptyList(), "");
  }

  @Override
  public boolean canCastToConst() {
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  @doc("Casts an array to an obj_def value.")
  public ObjectiveDefinition cast(IScope scope, Object obj, Object param, boolean copy) throws GamaRuntimeException {
    if (obj instanceof ObjectiveDefinition) {
      return (ObjectiveDefinition) obj;
    } else if (obj instanceof GamaList) {
      GamaList<?> l = (GamaList<?>) obj;

      if (l.size() == 2) {
        try {
          return new ObjectiveDefinition((List<String>) l.get(0), (String) l.get(1));
        } catch (ClassCastException e) {
          throw GamaRuntimeException.create(e, scope);
        }
      } else {
        throw GamaRuntimeException.error("List must have length of 2.", scope);
      }
    }
    throw GamaRuntimeException.error(String.format("Can only cast lists and \"%s\" objects.", ICustomTypes.OBJ_DEF),
        scope);
  }
}
