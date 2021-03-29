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
  /** Inflection point of the curve between inf and nullMin. */
  public final double infl1;
  /** Value above which criticality is null. */
  public final double nullMin;
  /** Value below which criticality is null. */
  public final double nullMax;
  /** Inflection point of the curve between nullMax and sup. */
  public final double infl2;
  /** Value above which criticality is the highest. */
  public final double sup;

  /**
   * Creates a new set of parameters.
   * 
   * @param inf     Value below which criticality is the highest.
   * @param infl1   Inflection point of the curve between inf and ε1.
   * @param nullMin Value above which criticality is null.
   * @param nullMax Value below which criticality is null.
   * @param infl2   Inflection point of the curve between ε2 and sup.
   * @param sup     Value above which criticality is the highest.
   */
  public CriticalityFunctionParameters(final double inf, final double infl1, final double nullMin, final double nullMax,
      final double infl2, final double sup) {
    if (inf >= infl1 || infl1 >= nullMin || nullMin > nullMax || nullMax >= infl2 || infl2 >= sup) {
      throw new IllegalArgumentException("expected inf < infl1 < nullMin <= nullMax < infl2 < sup");
    }
    this.inf = inf;
    this.infl1 = infl1;
    this.nullMin = nullMin;
    this.nullMax = nullMax;
    this.infl2 = infl2;
    this.sup = sup;
  }

  @Override
  public String toString() {
    return String.format(Locale.US, "CFParams{inf=%f,infl1=%f,nullMin=%f,nullMax=%f,infl2=%f,sup=%f}", //
        this.inf, this.infl1, this.nullMin, this.nullMax, this.infl2, this.sup);
  }
}
