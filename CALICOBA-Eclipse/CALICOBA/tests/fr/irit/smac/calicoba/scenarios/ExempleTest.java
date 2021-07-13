package fr.irit.smac.calicoba.scenarios;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.calicoba.mas.agents.criticality.BaseCriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;
import fr.irit.smac.util.Logger;

public class ExempleTest {
  private static final int P1 = -50;
  private static final int P2 = -50;
  private static final boolean LEARN = false;
  private static final double ALPHA = 0.5;
  private static final int MAX_CYCLES = 350;

  public static void main(String[] args) {
    Locale.setDefault(Locale.ENGLISH);

    Logger.info(String.format("\tp1 = %d; p2 = %d", P1, P2));
    test(P1, P2, LEARN, ALPHA, MAX_CYCLES);
  }

  public static void test(int p1, int p2, final boolean learn, final double alpha, int maxCycles) {
    DummyValueProvider pr1 = new DummyValueProvider(p1);
    DummyValueProvider pr2 = new DummyValueProvider(p2);

    String dirname = String.format("%d_%d", p1, p2);
    if (learn) {
      dirname = String.format("learning_%.3f_%s", alpha, dirname);
    }
    Calicoba calicoba = new Calicoba(true, dirname, learn, alpha);
    calicoba.setRNGSeed(2);
    calicoba.addParameter(new WritableModelAttribute<>(pr1, "p1", -1500, 1500));
    calicoba.addMeasure(new ReadableModelAttribute<>(pr1, "m1", -1500, 1500));
    calicoba.addParameter(new WritableModelAttribute<>(pr2, "p2", -1500, 1500));
    calicoba.addMeasure(new ReadableModelAttribute<>(pr2, "m2", -1500, 1500));
    calicoba.addObjective("obj1", new BaseCriticalityFunction(Arrays.asList("m1", "m2")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        return Math.abs(parameterValues.get("m1") - parameterValues.get("m2")) - 23;
      }
    });
    calicoba.addObjective("obj2", new BaseCriticalityFunction(Arrays.asList("m2")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        return parameterValues.get("m2") - 12;
      }
    });
    calicoba.setInfluenceFunction((pName, pValue, objName, objCrit) -> {
      if (objName.equals("obj1")) {
        if (pName.equals("p1")) {
          double p2Value = ((ParameterAgent) calicoba.getAgentById("ParameterAgent_2")).getAttributeValue();
          return Math.abs(Math.abs(pValue + 1 - p2Value) - 23) >= Math.abs(Math.abs(pValue - p2Value) - 23) ? 1.0
              : -1.0;
        } else {
          double p1Value = ((ParameterAgent) calicoba.getAgentById("ParameterAgent_1")).getAttributeValue();
          return Math.abs(Math.abs(p1Value - (pValue + 1)) - 23) >= Math.abs(Math.abs(p1Value - pValue) - 23) ? 1.0
              : -1.0;
        }
      } else {
        if (pName.equals("p1")) {
          return 0.0;
        } else {
          return pValue >= 12 ? 1.0 : -1.0;
        }
      }
    });

    calicoba.setup();
    for (int i = 0; i < maxCycles; i++) {
      calicoba.step();
    }
  }
}
