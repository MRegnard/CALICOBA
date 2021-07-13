package fr.irit.smac.calicoba.scenarios;

import java.util.Arrays;
import java.util.Map;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.criticality.BaseCriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;

public class Viennet1Test {
  public static void main(String[] args) {
    DummyValueProvider pr1 = new DummyValueProvider(0);
    DummyValueProvider pr2 = new DummyValueProvider(0);

    Calicoba calicoba = new Calicoba(true, "viennet1", false, 0);
    calicoba.addParameter(new WritableModelAttribute<>(pr1, "x", -4, 4));
    calicoba.addMeasure(new ReadableModelAttribute<>(pr1, "mx", -4, 4));
    calicoba.addParameter(new WritableModelAttribute<>(pr2, "y", -4, 4));
    calicoba.addMeasure(new ReadableModelAttribute<>(pr2, "my", -4, 4));
    calicoba.addObjective("o1", new BaseCriticalityFunction(Arrays.asList("mx", "my"), true) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double x = parameterValues.get("mx");
        double y = parameterValues.get("my");
        return x * x + Math.pow(y - 1, 2);
      }
    });
    calicoba.addObjective("o2", new BaseCriticalityFunction(Arrays.asList("mx", "my"), true) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double x = parameterValues.get("mx");
        double y = parameterValues.get("my");
        return x * x + Math.pow(y + 1, 2);
      }
    });
    calicoba.addObjective("o3", new BaseCriticalityFunction(Arrays.asList("mx", "my"), true) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double x = parameterValues.get("mx");
        double y = parameterValues.get("my");
        return Math.pow(x - 1, 2) + y * y + 2;
      }
    });

    calicoba.setup();
    while (true) {
      calicoba.step();
    }
  }
}
