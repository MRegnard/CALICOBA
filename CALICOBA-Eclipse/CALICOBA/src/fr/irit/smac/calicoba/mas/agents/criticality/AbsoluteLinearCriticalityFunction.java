package fr.irit.smac.calicoba.mas.agents.criticality;

import java.util.Map;

/**
 * A generic absolute linear function.
 *
 * @author Damien Vergnet
 */
public class AbsoluteLinearCriticalityFunction extends SingleParamCriticalityFunction {
  private final double slope;
  private final double offset;

  /**
   * Generates a criticality function of the form: y = |x|
   * 
   * @param parameterName Name of the single parameter.
   */
  public AbsoluteLinearCriticalityFunction(final String parameterName) {
    this(parameterName, 1, 0);
  }

  /**
   * Generates a criticality function of the form: y = |slope × x + offset|
   * 
   * @param slope  The slope of the curve.
   * @param offset The horizontal offset of the curve.
   */
  public AbsoluteLinearCriticalityFunction(final String parameterName, final double slope, final double offset) {
    super(parameterName);
    this.slope = slope;
    this.offset = offset;
  }

  @Override
  protected double getImpl(final Map<String, Double> parameterValues) {
    return Math.abs(this.slope * parameterValues.get(this.getParameterName()) + this.offset);
  }
}
