package fr.irit.smac.calicoba.scenarios;

import java.util.Arrays;
import java.util.Map;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.criticality.BaseCriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;

public class RosenbrockValleyTest {
  public static void main(String[] args) {
    DummyValueProvider pr1 = new DummyValueProvider(0);
    DummyValueProvider pr2 = new DummyValueProvider(0);

    Calicoba calicoba = new Calicoba(true, "rosenbrock", false, 0, false);
    calicoba.addParameter(new WritableModelAttribute<>(pr1, "x", -4, 4));
    calicoba.addOutput(new ReadableModelAttribute<>(pr1, "ox", -4, 4));
    calicoba.addParameter(new WritableModelAttribute<>(pr2, "y", -4, 4));
    calicoba.addOutput(new ReadableModelAttribute<>(pr2, "mo", -4, 4));
    calicoba.addObjective("obj", new BaseCriticalityFunction(Arrays.asList("ox", "oy"), true) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double x = parameterValues.get("ox");
        double y = parameterValues.get("oy");
        return Math.pow(1 - x, 2) + 100 * Math.pow(y - x * x, 2);
      }
    });

    calicoba.setup();
    while (true) {
      calicoba.step();
    }
  }
}
