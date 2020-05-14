package fr.irit.smac.calicoba.gaml;

import fr.irit.smac.calicoba.mas.Calicoba;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.skills.Skill;

/**
 * Base skill for target models and reference systems.
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
  public void init() {
    if (!Calicoba.isInitialized()) {
      Calicoba.init();
    }
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
