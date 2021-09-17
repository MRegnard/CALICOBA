package fr.irit.smac.calicoba.experimentations;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.irit.smac.util.Pair;

public abstract class Model {
  private final String id;
  private Map<String, Double> parameters;
  private Map<String, Double> outputs;
  private final Map<String, Pair<Double, Double>> parametersDomains;
  private final Map<String, Pair<Double, Double>> outputsDomains;

  public Model(final String id, final Map<String, Pair<Double, Double>> parametersDomains,
      final Map<String, Pair<Double, Double>> outputsDomains) {
    this.id = id;
    this.parameters = parametersDomains.keySet().stream().collect(Collectors.toMap(Function.identity(), k -> 0.0));
    this.outputs = outputsDomains.keySet().stream().collect(Collectors.toMap(Function.identity(), k -> 0.0));
    this.parametersDomains = parametersDomains;
    this.outputsDomains = outputsDomains;
  }

  /**
   * Evaluates then updates this model.
   */
  public final void update() {
    this.outputs = this.evaluateImpl(this.parameters);
  }

  /**
   * Evaluates this model with the given parameter values.
   * 
   * @param parameters Parameter values.
   * @return The output values for this model.
   */
  public final Map<String, Double> evaluate(Map<String, Double> parameters) {
    if (!parameters.keySet().equals(this.parameters.keySet())) {
      throw new IllegalArgumentException("invalid arguments");
    }
    return this.evaluateImpl(parameters);
  }

  /**
   * Evaluates this model with the given parameter values.
   * 
   * @param parameters Parameter values.
   * @return The output values for this model.
   */
  protected abstract Map<String, Double> evaluateImpl(Map<String, Double> parameters);

  public String getId() {
    return this.id;
  }

  public Set<String> getParameterNames() {
    return this.parameters.keySet();
  }

  public double getParameter(String name) {
    return this.parameters.get(name);
  }

  public void setParameter(String name, double value) {
    if (!this.parameters.containsKey(name)) {
      throw new IllegalArgumentException(name);
    }
    this.parameters.put(name, value);
  }

  public Pair<Double, Double> getParameterDomain(String name) {
    return this.parametersDomains.get(name);
  }

  public Set<String> getOutputNames() {
    return this.outputs.keySet();
  }

  public double getOutput(String name) {
    return this.outputs.get(name);
  }

  public Pair<Double, Double> getOutputDomain(String name) {
    return this.outputsDomains.get(name);
  }
}
