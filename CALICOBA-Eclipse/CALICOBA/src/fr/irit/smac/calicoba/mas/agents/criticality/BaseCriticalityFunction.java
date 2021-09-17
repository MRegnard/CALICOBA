package fr.irit.smac.calicoba.mas.agents.criticality;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base implementation of a criticality function. It checks whether the actual
 * number of values corresponds to the expected number of parameters. It may
 * also cap the result between −100 and 100 inclusive.
 * 
 * @author Damien Vergnet
 */
public abstract class BaseCriticalityFunction implements CriticalityFunction {
  protected final List<String> parameterNames;
  private final Set<String> parameterSet; // Cache for sanity checks
  private final boolean optimize;

  /**
   * Creates a criticality function as a constraint.
   * 
   * @param parameterNames The number of parameters for this function.
   */
  public BaseCriticalityFunction(final List<String> parameterNames) {
    this(parameterNames, false);
  }

  /**
   * Creates a criticality function with the given method.
   * 
   * @param parameterNames The names of parameters for this function.
   * @param optimize       Whether this function represents an objective to
   *                       optimize (true) or a constraint (false).
   */
  public BaseCriticalityFunction(final List<String> parameterNames, final boolean optimize) {
    Objects.requireNonNull(parameterNames);
    if (parameterNames.isEmpty()) {
      throw new IllegalArgumentException("parameterNames size must be >= 1");
    }
    this.parameterNames = parameterNames;
    this.parameterSet = this.parameterNames.stream().collect(Collectors.toSet());
    this.optimize = Objects.requireNonNull(optimize);
  }

  @Override
  public List<String> getParameterNames() {
    return this.parameterNames;
  }

  // Objective functions’ criticality is constant
  private static final double OPT_CRIT = 0.1;

  @Override
  public final double get(final Map<String, Double> parameterValues) {
    if (!parameterValues.keySet().equals(this.parameterSet)) {
      throw new IllegalArgumentException(
          String.format("Invalid argument(s) for criticality function, expected %s, got %s.", this.parameterSet,
              parameterValues.keySet()));
    }
    return this.optimize ? OPT_CRIT : this.getImpl(parameterValues);
  }

  /**
   * Actual implementation of the criticality function.
   * 
   * @param parameterValues The functions’s parameters values.
   * @return The criticality.
   */
  protected abstract double getImpl(final Map<String, Double> parameterValues);
}
