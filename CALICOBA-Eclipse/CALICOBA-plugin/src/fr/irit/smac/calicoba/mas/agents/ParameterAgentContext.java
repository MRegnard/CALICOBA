package fr.irit.smac.calicoba.mas.agents;

import java.util.Collections;
import java.util.Map;

/**
 * The context of a Parameter agent is composed of all measure and parameter
 * values.
 * 
 * @author Damien Vergnet
 */
public class ParameterAgentContext {
  private final Map<String, Double> measuresValues;
  private final Map<String, Double> parametersValues;

  /**
   * Creates a new context.
   * 
   * @param measuresValues   All measure values.
   * @param parametersValues All parameter values.
   */
  public ParameterAgentContext(final Map<String, Double> measuresValues, final Map<String, Double> parametersValues) {
    this.measuresValues = Collections.unmodifiableMap(measuresValues);
    this.parametersValues = Collections.unmodifiableMap(parametersValues);
  }

  /**
   * Returns the value for the given measure.
   * 
   * @param measureName The measure name.
   * @return The value.
   */
  public double getMeasureValue(String measureName) {
    return this.measuresValues.get(measureName);
  }

  /**
   * Returns the value for the given parameter.
   * 
   * @param parameterName The parameter name.
   * @return The value.
   */
  public double getParameterValue(String parameterName) {
    return this.parametersValues.get(parameterName);
  }

  /**
   * Computes the similarity with the given context.
   * 
   * @param other The context to compute the similarity with.
   * @return The similarity measure.
   */
  public double similarity(ParameterAgentContext other) {
    double distance = 0;

    distance += other.measuresValues.entrySet().stream() //
        .mapToDouble(e -> Math.pow(e.getValue() - this.measuresValues.get(e.getKey()), 2)) //
        .reduce(Double::sum).getAsDouble();
    distance += other.parametersValues.entrySet().stream()
        .mapToDouble(e -> Math.pow(e.getValue() - this.parametersValues.get(e.getKey()), 2)) //
        .reduce(Double::sum).getAsDouble();

    return distance;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.measuresValues == null) ? 0 : this.measuresValues.hashCode());
    result = prime * result + ((this.parametersValues == null) ? 0 : this.parametersValues.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || this.getClass() != obj.getClass())
      return false;

    ParameterAgentContext other = (ParameterAgentContext) obj;
    return this.measuresValues.equals(other.measuresValues) && this.parametersValues.equals(other.parametersValues);
  }
}
