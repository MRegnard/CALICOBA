package fr.irit.smac.calicoba.gaml.skills;

import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.skills.Skill;

/**
 * Base class for CALICOBA skills.
 * 
 * @author Damien Vergnet
 */
public abstract class ModelSkill extends Skill {
  protected boolean initialized;

  /**
   * Creates the skill.
   */
  public ModelSkill() {
    this.initialized = false;
  }

  /**
   * Initializes this skill and CALICOBA if it is not already.
   */
  public void setInitialized() {
    this.initialized = true;
  }

  /**
   * Checks wether this skill has already been initialized.
   * 
   * @param scope The current scope.
   * @throws GamaRuntimeException If this skill is not yet initialized.
   */
  public void checkInitialized(final IScope scope) {
    if (!this.initialized) {
      String message = "Skill not initialized. " //
          + "You may have forgotten to call init_system or init_model.";
      throw GamaRuntimeException.error(message, scope);
    }
  }
}
