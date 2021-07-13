package fr.irit.smac.calicoba.mas.agents.criticality;

import java.util.Map;

public class DefaultCriticalityFunction extends SingleParamCriticalityFunction {
  private final CriticalityFunctionParameters p;

  /**
   * Generates a function as defined in Luc Pons’ thesis.
   * 
   * @param parameterName Name of the single parameter.
   * @param params        The 6 parameters.
   * @note See Appendix A of Luc Pons’ thesis.
   */
  public DefaultCriticalityFunction(final String parameterName, final CriticalityFunctionParameters params) {
    super(parameterName);
    this.p = params;
  }

  @Override
  protected double getImpl(final Map<String, Double> parameterValues) {
    double x = parameterValues.get(this.getParameterName());

    if (x < this.p.inf || x >= this.p.sup) {
      return 100 * (x < this.p.inf ? -1 : 1);
    } else {
      double g1 = -2 * 100 / (this.p.nullMin - this.p.inf);
      double g2 = -2 * 100 / (this.p.sup - this.p.nullMax);
      double d1 = -g1 * (this.p.nullMin - this.p.infl1) / 2;
      double d2 = -g2 * (this.p.infl2 - this.p.nullMax) / 2;

      if (x < this.p.infl1) {
        return -(g1 * (Math.pow(x - this.p.infl1, 2) / (2 * (this.p.infl1 - this.p.inf))) + g1 * (x - this.p.infl1)
            + d1);
      } else if (x < this.p.nullMin) {
        return -(-g1 * (Math.pow(x - this.p.infl1, 2) / (2 * (this.p.nullMin - this.p.infl1))) + g1 * (x - this.p.infl1)
            + d1);
      } else if (x <= this.p.nullMax) {
        return 0;
      } else if (x < this.p.infl2) {
        return -g2 * (Math.pow(this.p.infl2 - x, 2) / (2 * (this.p.infl2 - this.p.nullMax))) + g2 * (this.p.infl2 - x)
            + d2;
      } else {
        return g2 * (Math.pow(this.p.infl2 - x, 2) / (2 * (this.p.sup - this.p.infl2))) + g2 * (this.p.infl2 - x) + d2;
      }
    }
  }
}
