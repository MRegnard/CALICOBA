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

    Calicoba calicoba = new Calicoba(true, "viennet1", false, 0, false);
    calicoba.addParameter(new WritableModelAttribute<>(pr1, "x", -4, 4));
    calicoba.addOutput(new ReadableModelAttribute<>(pr1, "ox", -4, 4));
    calicoba.addParameter(new WritableModelAttribute<>(pr2, "y", -4, 4));
    calicoba.addOutput(new ReadableModelAttribute<>(pr2, "oy", -4, 4));
    calicoba.addObjective("o1", new BaseCriticalityFunction(Arrays.asList("ox", "oy"), true) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double x = parameterValues.get("ox");
        double y = parameterValues.get("oy");
        return x * x + Math.pow(y - 1, 2);
      }
    });
    calicoba.addObjective("o2", new BaseCriticalityFunction(Arrays.asList("ox", "oy"), true) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double x = parameterValues.get("ox");
        double y = parameterValues.get("oy");
        return x * x + Math.pow(y + 1, 2);
      }
    });
    calicoba.addObjective("o3", new BaseCriticalityFunction(Arrays.asList("ox", "oy"), true) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double x = parameterValues.get("ox");
        double y = parameterValues.get("oy");
        return Math.pow(x - 1, 2) + y * y + 2;
      }
    });

    calicoba.setup();
    while (true) {
      calicoba.step();
    }
  }
}
