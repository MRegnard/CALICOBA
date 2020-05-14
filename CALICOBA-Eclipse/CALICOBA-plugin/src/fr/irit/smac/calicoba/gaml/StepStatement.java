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
 * The <code>calicoba_step</code> statement runs CALICOBA for 1 step. Thus it
 * should not be called more than once per cycle.
 * 
 * @author Damien Vergnet
 */
@symbol(name = "calicoba_step", kind = ISymbolKind.SINGLE_STATEMENT, with_sequence = false)
@inside(kinds = { ISymbolKind.BEHAVIOR, ISymbolKind.SEQUENCE_STATEMENT, ISymbolKind.LAYER })
@doc("Runs CALICOBA for 1 step.")
public class StepStatement extends AbstractStatement {
  public StepStatement(IDescription desc) {
    super(desc);
  }

  @Override
  protected Object privateExecuteIn(IScope scope) throws GamaRuntimeException {
    try {
      Calicoba.instance().step();
    }
    catch (RuntimeException e) {
      throw GamaRuntimeException.create(e, scope);
    }
    return null;
  }
}
