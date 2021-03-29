package fr.irit.smac.calicoba.gaml;

import fr.irit.smac.calicoba.mas.Calicoba;

/**
 * This class wraps in single instance of CALICOBA for the plugin.
 *
 * @author Damien Vergnet
 */
public final class CalicobaSingleton {
  private static Calicoba instance;

  /**
   * Initializes the singleton instance.
   */
  public static void init() {
    instance = new Calicoba();
  }

  /**
   * @return The singleton instance.
   */
  public static Calicoba instance() {
    if (instance == null) {
      throw new IllegalStateException("CALICOBA singleton instance not initialized.");
    }
    return instance;
  }

  private CalicobaSingleton() {
  }
}
