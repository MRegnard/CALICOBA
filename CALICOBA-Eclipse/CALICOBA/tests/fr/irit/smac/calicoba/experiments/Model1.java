package fr.irit.smac.calicoba.experiments;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fr.irit.smac.util.Pair;

public class Model1 extends Model {
  private static final Map<String, Pair<Double, Double>> PARAMETERS;
  private static final Map<String, Pair<Double, Double>> OUTPUTS;

  static {
    PARAMETERS = new HashMap<>();
    PARAMETERS.put("p1", new Pair<>(-1500.0, 1500.0));

    OUTPUTS = new HashMap<>();
    OUTPUTS.put("o1", new Pair<>(0.0, 1512.0));
  }

  public Model1() {
    super("model_1", PARAMETERS, OUTPUTS);
  }

  // o = |p - 12|
  @Override
  protected Map<String, Double> evaluateImpl(Map<String, Double> parameters) {
    return Collections.singletonMap("o1", Math.abs(parameters.get("p1") - 12));
  }
}
