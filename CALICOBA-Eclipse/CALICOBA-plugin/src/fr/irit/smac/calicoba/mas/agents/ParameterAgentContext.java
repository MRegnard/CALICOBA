package fr.irit.smac.calicoba.mas.agents;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import msi.gama.common.interfaces.IValue;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.getter;
import msi.gama.precompiler.GamlAnnotations.variable;
import msi.gama.precompiler.GamlAnnotations.vars;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.types.IType;

/**
 * The context of a Parameter agent is composed of all measure and parameter
 * values.
 * 
 * @author Damien Vergnet
 */
@vars({
    @variable(name = ParameterAgentContext.MEASURES, type = IType.MAP),
    @variable(name = ParameterAgentContext.PARAMETERS, type = IType.MAP),
})
@doc("A context is a snapshot of the inputs/outputs of the target model at a given time.")
public class ParameterAgentContext implements IValue {
  public static final String MEASURES = "measures";
  public static final String PARAMETERS = "parameters";

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

  @getter(MEASURES)
  public Map<String, Double> getMeasuresValues() {
    return new HashMap<>(this.measuresValues);
  }

  @getter(PARAMETERS)
  public Map<String, Double> getParametersValues() {
    return new HashMap<>(this.parametersValues);
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
   * Computes the distance to the given context based on the given maps. These
   * maps should contain the same keys.
   * 
   * @param map1 The first map.
   * @param map2 The second map.
   * @param mins A map containing the all-time minimum value for each measure.
   * @param maxs A map containing the all-time maximum value for each measure.
   * @return The distance.
   */
  private double distance(Map<String, Double> map1, Map<String, Double> map2, Map<String, Double> mins,
      Map<String, Double> maxs) {
    double distance = 0;

    distance += map2.entrySet().stream().mapToDouble(e -> {
      String valueName = e.getKey();
      double min = mins.get(valueName);
      double max = maxs.get(valueName);
      return this.normalize(Math.pow(e.getValue() - map1.get(valueName), 2), min, max);
    }).reduce(Double::sum).getAsDouble();

    return distance;
  }

  /**
   * Computes the distance to the given context based on measures only.
   * 
   * @param other The context to compute the distance with.
   * @param mins  A map containing the all-time minimum value for each measure.
   * @param maxs  A map containing the all-time maximum value for each measure.
   * @return The distance.
   */
  public double distanceMeasures(ParameterAgentContext other, Map<String, Double> mins, Map<String, Double> maxs) {
    return this.distance(this.measuresValues, other.measuresValues, mins, maxs);
  }

  /**
   * Computes the distance to the given context based on parameters only.
   * 
   * @param other The context to compute the distance with.
   * @param mins  A map containing the all-time minimum value for each parameter.
   * @param maxs  A map containing the all-time maximum value for each parameter.
   * @return The distance.
   */
  public double distanceParameters(ParameterAgentContext other, Map<String, Double> mins, Map<String, Double> maxs) {
    return this.distance(this.parametersValues, other.parametersValues, mins, maxs);
  }

  /**
   * Normalizes a value between two bounds.
   * 
   * @param v   The value to normalize.
   * @param min The lower bound.
   * @param max The upper bound.
   * @return The normalized value.
   */
  private double normalize(double v, double min, double max) {
    return (v - min) / (max - min);
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

  @Override
  public String toString() {
    return String.format("Context{measures=%s,parameters=%s}", this.measuresValues, this.parametersValues);
  }

  @Override
  public String stringValue(final IScope scope) throws GamaRuntimeException {
    return this.toString();
  }

  @Override
  public IValue copy(final IScope scope) throws GamaRuntimeException {
    // TODO Auto-generated method stub
    return null;
  }
}
