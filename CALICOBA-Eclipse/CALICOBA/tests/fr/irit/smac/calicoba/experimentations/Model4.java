package fr.irit.smac.calicoba.experimentations;

import java.util.HashMap;
import java.util.Map;

import fr.irit.smac.util.Pair;

public class Model4 extends Model {
  private static final Map<String, Pair<Double, Double>> PARAMETERS;
  private static final Map<String, Pair<Double, Double>> OUTPUTS;

  static {
    PARAMETERS = new HashMap<>();
    PARAMETERS.put("p1", new Pair<>(-1500.0, 1500.0));
    PARAMETERS.put("p2", new Pair<>(-1500.0, 1500.0));

    OUTPUTS = new HashMap<>();
    OUTPUTS.put("o1", new Pair<>(-2999.0, 3001.0));
    OUTPUTS.put("o2", new Pair<>(-3001.0, 2999.0));
    OUTPUTS.put("o3", new Pair<>(-1520.0, 1580.0));
  }

  public Model4() {
    super("model_4", PARAMETERS, OUTPUTS);
  }

  // o₁ = p₁ + p₂ + 1
  // o₂ = p₁ + p₂ - 1
  // o₃ = p₂ - 50
  @Override
  protected Map<String, Double> evaluateImpl(Map<String, Double> parameters) {
    final double p1 = parameters.get("p1");
    final double p2 = parameters.get("p2");
    Map<String, Double> output = new HashMap<>();
    output.put("o1", p1 + p2 + 1);
    output.put("o2", p1 + p2 - 1);
    output.put("o3", p2 - 20);
    return output;
  }
}
