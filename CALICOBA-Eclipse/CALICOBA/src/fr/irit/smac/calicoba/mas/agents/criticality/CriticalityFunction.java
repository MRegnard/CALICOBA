package fr.irit.smac.calicoba.mas.agents.criticality;

import java.util.List;
import java.util.Map;

public interface CriticalityFunction {
  /**
   * Computes the criticality for the given values.
   * 
   * @param parameterValues The functions’s parameter values.
   * @return The criticality.
   */
  double get(final Map<String, Double> parameterValues);

  /**
   * @return The name of all parameters.
   */
  List<String> getParameterNames();
}
