package fr.irit.smac.calicoba.experimentations;

import java.util.HashMap;
import java.util.Map;

import fr.irit.smac.util.Pair;

public class Model2 extends Model {
  private static final Map<String, Pair<Double, Double>> PARAMETERS;
  private static final Map<String, Pair<Double, Double>> OUTPUTS;

  static {
    PARAMETERS = new HashMap<>();
    PARAMETERS.put("p1", new Pair<>(-1500.0, 1500.0));
    PARAMETERS.put("p2", new Pair<>(-1500.0, 1500.0));

    OUTPUTS = new HashMap<>();
    OUTPUTS.put("o1", new Pair<>(-23.0, 2977.0));
    OUTPUTS.put("o2", new Pair<>(-1512.0, 1488.0));
  }

  public Model2() {
    super("model_2", PARAMETERS, OUTPUTS);
  }

  // o₁ = |p₁ - p₂| - 23
  // o₂ = p₂ - 12
  @Override
  protected Map<String, Double> evaluateImpl(Map<String, Double> parameters) {
    Map<String, Double> output = new HashMap<>();
    output.put("o1", Math.abs(parameters.get("p1") - parameters.get("p2")) - 23);
    output.put("o2", parameters.get("p2") - 12);
    return output;
  }
}
