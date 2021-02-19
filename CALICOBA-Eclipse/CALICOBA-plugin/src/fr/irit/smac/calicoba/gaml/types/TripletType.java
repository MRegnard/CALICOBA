package fr.irit.smac.calicoba.gaml.types;

import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.type;
import msi.gama.precompiler.ISymbolKind;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaList;
import msi.gaml.types.GamaType;

@type( //
    name = ICustomTypes.TRIPLET, //
    id = ICustomTypes.TRIPLET_ID, //
    wraps = { Triplet.class }, //
    kind = ISymbolKind.Variable.CONTAINER, //
    doc = @doc("This type represents a triplet of values.") //
)
public class TripletType extends GamaType<Triplet<?, ?, ?>> {
  @Override
  public Triplet<?, ?, ?> getDefault() {
    return new Triplet<>(null, null, null);
  }

  @Override
  public boolean canCastToConst() {
    return true;
  }

  @Override
  @doc("Casts a list into a triplet value.")
  public Triplet<?, ?, ?> cast(IScope scope, Object obj, Object param, boolean copy) throws GamaRuntimeException {
    if (obj instanceof Triplet) {
      return (Triplet<?, ?, ?>) obj;
    } else if (obj instanceof GamaList) {
      GamaList<?> l = (GamaList<?>) obj;
      return new Triplet<>(l.get(0), l.get(1), l.get(2));
    }
    throw GamaRuntimeException.error("Can only cast \"" + ICustomTypes.TRIPLET + "\" objects.", scope);
  }
}
