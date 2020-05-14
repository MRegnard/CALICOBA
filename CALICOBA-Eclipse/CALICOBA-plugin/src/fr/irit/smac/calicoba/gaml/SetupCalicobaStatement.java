package fr.irit.smac.calicoba.gaml;

import fr.irit.smac.calicoba.mas.Calicoba;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.ISymbolKind;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.descriptions.IDescription;
import msi.gaml.statements.AbstractStatement;

/**
 * The <code>setup_calicoba</code> statement sets up CALICOBA. Thus it should
 * only be called in the global init statement, after the target and reference
 * models have been instanciated.
 * 
 * @author Damien Vergnet
 */
@symbol(name = "calicoba_setup", kind = ISymbolKind.SINGLE_STATEMENT, with_sequence = false)
@inside(kinds = { ISymbolKind.BEHAVIOR, ISymbolKind.SEQUENCE_STATEMENT, ISymbolKind.LAYER })
@doc("Sets up CALICOBA. Must be called <em>after</em> target and reference models have been instanciated.")
public class SetupCalicobaStatement extends AbstractStatement {
  public SetupCalicobaStatement(IDescription desc) {
    super(desc);
  }

  @Override
  protected Object privateExecuteIn(IScope scope) throws GamaRuntimeException {
    try {
      Calicoba.instance().setup();
    }
    catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }
    return null;
  }
}
