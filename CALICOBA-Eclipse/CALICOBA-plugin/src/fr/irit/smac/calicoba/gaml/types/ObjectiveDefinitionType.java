package fr.irit.smac.calicoba.gaml.types;

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
    return new ObjectiveDefinition(null, 0, 0, 0, 0, 0, 0);
  }

  @Override
  public boolean canCastToConst() {
    return true;
  }

  @Override
  @doc("Casts an array to an obj_def value.")
  public ObjectiveDefinition cast(IScope scope, Object obj, Object param, boolean copy) throws GamaRuntimeException {
    if (obj instanceof ObjectiveDefinition) {
      return (ObjectiveDefinition) obj;
    } else if (obj instanceof GamaList) {
      GamaList<?> l = (GamaList<?>) obj;

      if (l.size() == 7) {
        return new ObjectiveDefinition((String) l.get(0), (Double) l.get(1), (Double) l.get(2), (Double) l.get(3),
            (Double) l.get(4), (Double) l.get(5), (Double) l.get(6));
      } else if (l.size() == 5) {
        return new ObjectiveDefinition((String) l.get(0), (Double) l.get(1), (Double) l.get(2), (Double) l.get(3),
            (Double) l.get(4));
      } else if (l.size() == 4) {
        return new ObjectiveDefinition((String) l.get(0), (Double) l.get(1), (Double) l.get(2), (Double) l.get(3));
      } else {
        throw GamaRuntimeException.error("Invalid number of criticality function parameters.", scope);
      }
    }
    throw GamaRuntimeException.error(String.format("Can only cast \"%s\" objects and lists.", ICustomTypes.OBJ_DEF),
        scope);
  }
}
