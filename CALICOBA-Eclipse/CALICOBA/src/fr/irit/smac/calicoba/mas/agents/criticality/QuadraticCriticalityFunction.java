package fr.irit.smac.calicoba.mas.agents.criticality;

import java.util.Map;

/**
 * A generic quadratic function.
 *
 * @author Damien Vergnet
 */
public class QuadraticCriticalityFunction extends SingleParamCriticalityFunction {
  private final double slope;
  private final double hOffset;
  private final double vOffset;

  /**
   * Generates a criticality function of the form: y = x<sup>2</sup>
   * 
   * @param parameterName Name of the single parameter.
   */
  public QuadraticCriticalityFunction(final String parameterName) {
    this(parameterName, 1, 0, 0);
  }

  /**
   * Generates a criticality function of the form: y = slope × (x<sup>2</sup> −
   * horizontalOffset) + verticalOffset
   * 
   * @param parameterName    Name of the single parameter.
   * @param slope            The slope of the curve.
   * @param horizontalOffset The horizontal offset of the curve.
   * @param verticalOffset   The vertical offset of the curve.
   */
  public QuadraticCriticalityFunction(final String parameterName, final double slope, final double horizontalOffset,
      final double verticalOffset) {
    super(parameterName);
    this.slope = slope;
    this.hOffset = horizontalOffset;
    this.vOffset = verticalOffset;
  }

  @Override
  protected double getImpl(final Map<String, Double> parameterValues) {
    return this.slope * Math.pow(parameterValues.get(this.getParameterName()) - this.hOffset, 2) + this.vOffset;
  }
}
