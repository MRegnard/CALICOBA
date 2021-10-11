package fr.irit.smac.calicoba.experiments;

import java.util.Optional;

public final class ExperimentsConfig {
  /** Optional name of only parameter that is allowed to act. */
  public static final Optional<String> FREE_PARAM = Optional.ofNullable(null);

  /**
   * Number of cycles the model’s outputs have to be stable to consider it
   * calibrated.
   */
  public static final int CALIBRATION_THRESHOLD = 60;
  /** Maximum number of cycles for each run. */
  public static final int MAX_STEPS_NB = 1000;

  /** Whether CALICOBA should learn influences. */
  public static final boolean LEARN = false;
  /** When {@link #LEARN} is true, the α hyper-parameter. */
  public static final double ALPHA = 0;

  /** The optional seed for the RNG. */
  public static final Optional<Long> SEED = Optional.ofNullable(1L);

  private ExperimentsConfig() {
  }
}
