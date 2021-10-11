package fr.irit.smac.calicoba.experiments;

import java.util.HashMap;
import java.util.Map;

import fr.irit.smac.util.Pair;

public class Model5 extends Model {
  private static final Map<String, Pair<Double, Double>> PARAMETERS;
  private static final Map<String, Pair<Double, Double>> OUTPUTS;

  static {
    PARAMETERS = new HashMap<>();
    PARAMETERS.put("p1", new Pair<>(-3.0, 2.0));

    OUTPUTS = new HashMap<>();
    OUTPUTS.put("o1", new Pair<>(-12.0, 63.0));
  }

  public Model5() {
    super("model_5", PARAMETERS, OUTPUTS);
  }

  // o₁ = -p₁⁵ + 10p₁³ - 30p₁
  @Override
  protected Map<String, Double> evaluateImpl(Map<String, Double> parameters) {
    final double p1 = parameters.get("p1");
    Map<String, Double> output = new HashMap<>();
    output.put("o1", -Math.pow(p1, 5) + 10 * Math.pow(p1, 3) - 30 * p1);
    return output;
  }
}
