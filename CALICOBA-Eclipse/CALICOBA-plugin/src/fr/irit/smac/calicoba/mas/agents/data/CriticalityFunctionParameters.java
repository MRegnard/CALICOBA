package fr.irit.smac.calicoba.mas.agents.data;

import java.util.Locale;

/**
 * Criticality functions are defined with 6 parameters.
 * 
 * @author Damien Vergnet
 * @note See Appendix A of Luc Pons’ thesis.
 */
public final class CriticalityFunctionParameters {
  /** Value below which criticality is the highest. */
  public final double inf;
  /** Inflection point of the curve between inf and ε1. */
  public final double η1;
  /** Value above which criticality is null. */
  public final double ε1;
  /** Value below which criticality is null. */
  public final double ε2;
  /** Inflection point of the curve between ε2 and sup. */
  public final double η2;
  /** Value above which criticality is the highest. */
  public final double sup;

  /**
   * Creates a new set of parameters.
   * 
   * @param inf Value below which criticality is the highest.
   * @param η1  Inflection point of the curve between inf and ε1.
   * @param ε1  Value above which criticality is null.
   * @param ε2  Value below which criticality is null.
   * @param η2  Inflection point of the curve between ε2 and sup.
   * @param sup Value above which criticality is the highest.
   */
  public CriticalityFunctionParameters(final double inf, final double η1, final double ε1, final double ε2,
      final double η2, final double sup) {
    if (inf >= η1 || η1 >= ε1 || ε1 > ε2 || ε2 >= η2 || η2 >= sup) {
      throw new IllegalArgumentException("expected inf < η1 < ε1 <= ε2 < η2 < sup");
    }
    this.inf = inf;
    this.η1 = η1;
    this.ε1 = ε1;
    this.ε2 = ε2;
    this.η2 = η2;
    this.sup = sup;
  }

  @Override
  public String toString() {
    return String.format(Locale.US, "CFParams{inf=%f,η1=%f,ε1=%f,ε2=%f,η2=%f,sup=%f}", //
        this.inf, this.η1, this.ε1, this.ε2, this.η2, this.sup);
  }
}
