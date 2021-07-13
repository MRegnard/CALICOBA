package fr.irit.smac.calicoba.mas.agents.criticality;

import java.util.Collections;

/**
 * Simple class to store the name of the parameter for single-parameter default
 * functions.
 *
 * @author Damien Vergnet
 */
public abstract class SingleParamCriticalityFunction extends BaseCriticalityFunction {
  /**
   * Creates a criticality function with a single parameter.
   * 
   * @param parameterName Name of the single parameter.
   * @param argsNumber    The number of parameters for this function.
   */
  public SingleParamCriticalityFunction(final String parameterName) {
    super(Collections.singletonList(parameterName));
  }

  /**
   * @return The name of the single parameter.
   */
  public String getParameterName() {
    return this.parameterNames.get(0);
  }
}
