package fr.irit.smac.calicoba.experiments.factory;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.experiments.Model;
import fr.irit.smac.util.Pair;

/**
 * This type of model made up of a procedurally generated node graph.
 *
 * @author Damien Vergnet
 */
class GeneratedModel extends Model {
  private final Map<String, ParameterNode> parameters;
  private final Map<String, Node> outputs;

  /**
   * Creates a new model for the given parameters and outputs. Parameter and
   * output nodes should be connected through a node graph.
   * 
   * @param parameters List of parameter nodes.
   * @param outputs    List of output nodes.
   */
  public GeneratedModel(final Map<String, ParameterNode> parameters, final Map<String, Node> outputs) {
    super(String.format("generated_model_p%d_o%d", parameters.size(), outputs.size()),
        generateDomains(parameters.keySet()), generateDomains(outputs.keySet()));
    this.parameters = parameters;
    this.outputs = outputs;
  }

  @Override
  protected Map<String, Double> evaluateImpl(Map<String, Double> parameters) {
    // Update parameter nodes
    parameters.entrySet().forEach(e -> this.parameters.get(e.getKey()).setValue(e.getValue()));
    // Compute output nodes
    return this.outputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().compute()));
  }

  /**
   * Generates domains for the given parameter names.
   * 
   * @param names Parameter names to generate domains for.
   * @return A map associating each name to its domain.
   */
  private static Map<String, Pair<Double, Double>> generateDomains(final Collection<String> names) {
    // Cannot allow 0 as there may be divisions in the model
    return names.stream().collect(Collectors.toMap(Function.identity(), k -> new Pair<>(1e-9, 1e9)));
  }

  @Override
  public String toString() {
    return this.outputs.toString();
  }
}
