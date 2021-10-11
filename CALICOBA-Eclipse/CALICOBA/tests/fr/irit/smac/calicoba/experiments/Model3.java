package fr.irit.smac.calicoba.experiments;

import java.util.HashMap;
import java.util.Map;

import fr.irit.smac.util.Pair;

public class Model3 extends Model {
  private static final Map<String, Pair<Double, Double>> PARAMETERS;
  private static final Map<String, Pair<Double, Double>> OUTPUTS;

  static {
    PARAMETERS = new HashMap<>();
    PARAMETERS.put("p1", new Pair<>(-1500.0, 1500.0));
    PARAMETERS.put("p2", new Pair<>(-1500.0, 1500.0));

    OUTPUTS = new HashMap<>();
    OUTPUTS.put("o1", new Pair<>(-1516.0, 2_251_484.0));
    OUTPUTS.put("o2", new Pair<>(-1512.0, 1488.0));
  }

  public Model3() {
    super("model_3", PARAMETERS, OUTPUTS);
  }

  // o₁ = p₁² + p₂ - 16
  // o₂ = p₂ - 12
  @Override
  protected Map<String, Double> evaluateImpl(Map<String, Double> parameters) {
    final double p1 = parameters.get("p1");
    final double p2 = parameters.get("p2");
    Map<String, Double> output = new HashMap<>();
    output.put("o1", p1 * p1 + p2 - 16);
    output.put("o2", p2 - 12);
    return output;
  }
}
