package fr.irit.smac.util;

import msi.gama.runtime.IScope;
import msi.gaml.statements.IStatement.WithArgs;

/**
 * Simple wrapper for GAMA actions.
 * 
 * @author Damien Vergnet
 *
 * @param <R> Return type of action.
 */
public class GamaAction<R> {
  private final WithArgs action;
  private final IScope scope;

  public GamaAction(final WithArgs action, final IScope scope) {
    this.action = action;
    this.scope = scope;
  }

  /**
   * Executes the action on the current runtime scope.
   * 
   * @return The value returned by the action.
   */
  @SuppressWarnings("unchecked")
  public R apply() {
    return (R) this.action.executeOn(this.scope);
  }
}
