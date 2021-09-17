package fr.irit.smac.calicoba.scenarios;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.calicoba.mas.agents.criticality.BaseCriticalityFunction;
import fr.irit.smac.calicoba.mas.agents.criticality.CriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;
import fr.irit.smac.calicoba.test_util.SobolSequenceGenerator;
import fr.irit.smac.util.Logger;
import fr.irit.smac.util.Pair;

public class ExempleTest {
  private static final boolean SOBOL = true;

  private static final int RUNS = 20;

  private static final int P1_INIT = 37;
  private static final int P2_INIT = 37;

  private static final String EXAMPLE_ID = "quad";

  private static final boolean LEARN = false;
  private static final double ALPHA = 0.5;

  private static final boolean MANUAL = false;

  private static final int MAX_CYCLES = 700;

  private static final Map<String, Pair<CriticalityFunction, CriticalityFunction>> MODELS;

  static {
    MODELS = new HashMap<>();

    MODELS.put("linear_abs", new Pair<>(new BaseCriticalityFunction(Arrays.asList("o1", "o2")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        return Math.abs(parameterValues.get("o1") - parameterValues.get("o2")) - 23;
      }
    }, new BaseCriticalityFunction(Arrays.asList("o2")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        return parameterValues.get("o2") - 12;
      }
    }));

    MODELS.put("linear", new Pair<>(new BaseCriticalityFunction(Arrays.asList("o1", "o2")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        return parameterValues.get("o1") - 2 * parameterValues.get("o2") - 23;
      }
    }, new BaseCriticalityFunction(Arrays.asList("o2")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        return parameterValues.get("o2") - 12;
      }
    }));

    MODELS.put("quad", new Pair<>(new BaseCriticalityFunction(Arrays.asList("o1", "o2")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        return parameterValues.get("o1") + 1e-3 * Math.pow(parameterValues.get("o2"), 2);
      }
    }, new BaseCriticalityFunction(Arrays.asList("o1", "o2")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        return 2 * parameterValues.get("o1") + parameterValues.get("o2");
      }
    }));
  }

  public static void main(String[] args) {
    Locale.setDefault(Locale.ENGLISH);

    for (Map.Entry<String, Pair<CriticalityFunction, CriticalityFunction>> e : MODELS.entrySet()) {
      if (SOBOL) {
        int i = 1;
        for (double[] v : new SobolSequenceGenerator(2, RUNS)) {
          Logger.info(String.format("Performing test run %d/%d…", i, RUNS));
          int p1 = sobolToParam(v[0]), p2 = sobolToParam(v[1]);
          Logger.info(String.format("\tp1 = %d; p2 = %d", p1, p2));
          test(p1, p2, e.getKey(), e.getValue().getFirst(), e.getValue().getSecond(), LEARN, ALPHA, MAX_CYCLES, MANUAL);
          i++;
        }
      } else {
        String exampleID = e.getKey();
        if (EXAMPLE_ID == null || exampleID.equals(EXAMPLE_ID)) {
          Logger.info(String.format("\tp1 = %d; p2 = %d", P1_INIT, P2_INIT));
          test(P1_INIT, P2_INIT, exampleID, e.getValue().getFirst(), e.getValue().getSecond(), LEARN, ALPHA, MAX_CYCLES,
              MANUAL);
        }
      }
    }
  }

  private static int sobolToParam(double s) {
    return (int) (s * 100 - 50);
  }

  public static void test(int p1, int p2, final String id, final CriticalityFunction cf1, final CriticalityFunction cf2,
      final boolean learn, final double alpha, final int maxCycles, final boolean manualActions) {
    DummyValueProvider pr1 = new DummyValueProvider(p1);
    DummyValueProvider pr2 = new DummyValueProvider(p2);

    String dirname = String.format("%s/%d_%d", id, p1, p2);
    if (learn) {
      dirname += String.format("_learning_%.3f", alpha);
    }
    Calicoba calicoba = new Calicoba(true, dirname, learn, alpha, manualActions);
    calicoba.setRNGSeed(1);
    calicoba.addParameter(new WritableModelAttribute<>(pr1, "p1", -1500, 1500));
    calicoba.addOutput(new ReadableModelAttribute<>(pr1, "o1", -1500, 1500));
    calicoba.addParameter(new WritableModelAttribute<>(pr2, "p2", -1500, 1500));
    calicoba.addOutput(new ReadableModelAttribute<>(pr2, "o2", -1500, 1500));

    calicoba.addObjective("obj1", cf1);
    calicoba.addObjective("obj2", cf2);

    calicoba.setInfluenceFunction((pName, pValue, objName, objCrit) -> {
      CriticalityFunction cf;
      if (objName.equals("obj1")) {
        cf = cf1;
      } else {
        cf = cf2;
      }
      Map<String, Double> params1 = new HashMap<>();
      Map<String, Double> params2 = new HashMap<>();
      if (pName.equals("p1")) {
        if (cf.getParameterNames().contains("o1")) {
          params1.put("o1", pValue + 1e-9);
          params2.put("o1", pValue);
        }
        if (cf.getParameterNames().contains("o2")) {
          double p2Value = ((ParameterAgent) calicoba
              .getAgent(a -> a instanceof ParameterAgent && ((ParameterAgent) a).getAttributeName().equals("p2")).get())
                  .getAttributeValue();
          params1.put("o2", p2Value);
          params2.put("o2", p2Value);
        }
      } else {
        if (cf.getParameterNames().contains("o1")) {
          double p1Value = ((ParameterAgent) calicoba
              .getAgent(a -> a instanceof ParameterAgent && ((ParameterAgent) a).getAttributeName().equals("p1")).get())
                  .getAttributeValue();
          params1.put("o1", p1Value);
          params2.put("o1", p1Value);
        }
        if (cf.getParameterNames().contains("o2")) {
          params1.put("o2", pValue + 1e-9);
          params2.put("o2", pValue);
        }
      }
      double v1 = Math.abs(cf.get(params1));
      double v2 = Math.abs(cf.get(params2));
      if (v1 > v2) {
        return 1.0;
      } else if (v1 < v2) {
        return -1.0;
      } else {
        return 0.0;
      }
    });

    calicoba.setup();
    for (int i = 0; i < maxCycles; i++) {
      calicoba.step();
    }
  }
}
