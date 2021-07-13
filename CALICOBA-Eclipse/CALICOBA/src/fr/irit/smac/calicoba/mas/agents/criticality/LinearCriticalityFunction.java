package fr.irit.smac.calicoba.mas.agents.criticality;

import java.util.Map;

/**
 * A generic linear function.
 *
 * @author Damien Vergnet
 */
public class LinearCriticalityFunction extends SingleParamCriticalityFunction {
  private final double slope;
  private final double offset;

  /**
   * Generates a criticality function of the form: y = x
   * 
   * @param parameterName Name of the single parameter.
   */
  public LinearCriticalityFunction(final String parameterName) {
    this(parameterName, 1, 0);
  }

  /**
   * Generates a criticality function of the form: y = slope × x + offset
   * 
   * @param parameterName Name of the single parameter.
   * @param slope         The slope of the curve.
   * @param offset        The horizontal offset of the curve.
   */
  public LinearCriticalityFunction(final String parameterName, final double slope, final double offset) {
    super(parameterName);
    this.slope = slope;
    this.offset = offset;
  }

  @Override
  protected double getImpl(final Map<String, Double> parameterValues) {
    return this.slope * parameterValues.get(this.getParameterName()) + this.offset;
  }
}
