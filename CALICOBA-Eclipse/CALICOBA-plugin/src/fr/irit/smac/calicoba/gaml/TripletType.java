package fr.irit.smac.calicoba.gaml;

import fr.irit.smac.util.Triplet;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.type;
import msi.gama.precompiler.ISymbolKind;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.types.GamaType;

@type( //
    name = ICustomTypes.TRIPLET, //
    id = ICustomTypes.TRIPLET_ID, //
    wraps = { Triplet.class }, //
    kind = ISymbolKind.Variable.CONTAINER, //
    doc = @doc("This type represents a context, i.e. a snapshot of the inputs/outputs of the target model at a given time.") //
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
  @doc("Do not use.")
  public Triplet<?, ?, ?> cast(IScope scope, Object obj, Object param, boolean copy) throws GamaRuntimeException {
    if (obj instanceof Triplet) {
      return (Triplet<?, ?, ?>) obj;
    }
    throw GamaRuntimeException.error("Can only cast \"" + ICustomTypes.TRIPLET + "\" objects.", scope);
  }
}
