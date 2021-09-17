package fr.irit.smac.calicoba.scenarios;

import java.util.Arrays;
import java.util.Map;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.criticality.BaseCriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;

public class AlexandrovTest {
  public static void main(String[] args) {
    DummyValueProvider pr1 = new DummyValueProvider(0);
    DummyValueProvider pr2 = new DummyValueProvider(0);
    DummyValueProvider pr3 = new DummyValueProvider(0);

    Calicoba calicoba = new Calicoba(true, "rosenbrock", false, 0, false);
    calicoba.addParameter(new WritableModelAttribute<>(pr1, "s", -4, 4));
    calicoba.addOutput(new ReadableModelAttribute<>(pr1, "os", -4, 4));
    calicoba.addParameter(new WritableModelAttribute<>(pr2, "l1", -4, 4));
    calicoba.addOutput(new ReadableModelAttribute<>(pr2, "ol1", -4, 4));
    calicoba.addParameter(new WritableModelAttribute<>(pr3, "l2", -4, 4));
    calicoba.addOutput(new ReadableModelAttribute<>(pr3, "ol2", -4, 4));
    calicoba.addObjective("o", new BaseCriticalityFunction(Arrays.asList("os", "ol1", "ol2"), true) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double s = parameterValues.get("os");
        double l1 = parameterValues.get("ol1");
        double l2 = parameterValues.get("ol2");
        double a1 = (2 * l1 - l2) / 3; // True form: a1 = (l1 - a2) / 2
        double a2 = (2 * l2 - l1) / 3; // True form: a2 = (l2 - a1) / 2
        return (a1 * a1 + 10 * a2 * a2 + 5 * Math.pow(s - 3, 2)) / 2;
      }
    });
    calicoba.addObjective("c1", new BaseCriticalityFunction(Arrays.asList("os", "ol1")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double c = parameterValues.get("os") + parameterValues.get("ol1");
        return c <= 1 ? 0 : c;
      }
    });
    calicoba.addObjective("c2", new BaseCriticalityFunction(Arrays.asList("os", "ol2")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double c = parameterValues.get("os") - parameterValues.get("ol2");
        return c <= -2 ? 0 : c;
      }
    });

    calicoba.setup();
    while (true) {
      calicoba.step();
    }
  }
}
