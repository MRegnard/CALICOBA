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

    Calicoba calicoba = new Calicoba(true, "rosenbrock", false, 0);
    calicoba.addParameter(new WritableModelAttribute<>(pr1, "s", -4, 4));
    calicoba.addMeasure(new ReadableModelAttribute<>(pr1, "ms", -4, 4));
    calicoba.addParameter(new WritableModelAttribute<>(pr2, "l1", -4, 4));
    calicoba.addMeasure(new ReadableModelAttribute<>(pr2, "ml1", -4, 4));
    calicoba.addParameter(new WritableModelAttribute<>(pr3, "l2", -4, 4));
    calicoba.addMeasure(new ReadableModelAttribute<>(pr3, "ml2", -4, 4));
    calicoba.addObjective("o", new BaseCriticalityFunction(Arrays.asList("ms", "ml1", "ml2"), true) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double s = parameterValues.get("ms");
        double l1 = parameterValues.get("ml1");
        double l2 = parameterValues.get("ml2");
        double a1 = (2 * l1 - l2) / 3; // True form: a1 = (l1 - a2) / 2
        double a2 = (2 * l2 - l1) / 3; // True form: a2 = (l2 - a1) / 2
        return (a1 * a1 + 10 * a2 * a2 + 5 * Math.pow(s - 3, 2)) / 2;
      }
    });
    calicoba.addObjective("c1", new BaseCriticalityFunction(Arrays.asList("ms", "ml1")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double c = parameterValues.get("ms") + parameterValues.get("ml1");
        return c <= 1 ? 0 : c;
      }
    });
    calicoba.addObjective("c2", new BaseCriticalityFunction(Arrays.asList("ms", "ml2")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double c = parameterValues.get("ms") - parameterValues.get("ml2");
        return c <= -2 ? 0 : c;
      }
    });

    calicoba.setup();
    while (true) {
      calicoba.step();
    }
  }
}
