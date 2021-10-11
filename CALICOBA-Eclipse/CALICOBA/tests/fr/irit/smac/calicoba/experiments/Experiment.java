package fr.irit.smac.calicoba.experiments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.calicoba.mas.agents.criticality.BaseCriticalityFunction;
import fr.irit.smac.calicoba.mas.agents.criticality.CriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProviderSetter;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.PeriodDetector;
import fr.irit.smac.calicoba.test_util.Utils;
import fr.irit.smac.util.CsvFileWriter;
import fr.irit.smac.util.Pair;

public class Experiment {
  private static final String ROOT = "experiment/";

  private static final double P1 = -1;
  private static final double P2 = 0;

  public static void main(String[] args) throws IOException {
    String root = "calicoba_output/" + ROOT;

    Path path = Paths.get(root);
    if (!Files.exists(path)) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Model model = new Model5();

    if (model.getParameterNames().size() == 1) {
      Pair<Double, Double> d = model.getParameterDomain("p1");
      double step = (d.getSecond() - d.getFirst()) / 100;
      try (CsvFileWriter fw = new CsvFileWriter(root + model.getId() + "_function.csv", false, true, "p_1", "o_1")) {
        for (double p1 = d.getFirst(); p1 <= d.getSecond(); p1 += step) {
          double o1 = model.evaluate(Collections.singletonMap("p1", p1)).get("o1");
          fw.writeLine(new Object[] { p1, o1 });
        }
      }
    }

    List<String> parameterNames = new ArrayList<>(model.getParameterNames());
    parameterNames.sort(null);

    Map<String, Double> initialParams = new HashMap<>();
    initialParams.put("p1", P1);
//    initialParams.put("p2", P2);

    String dirname = ROOT + model.getId() + "/" + Utils.mapToString(initialParams);
    Calicoba calicoba = new Calicoba(true, dirname, ExperimentsConfig.LEARN, ExperimentsConfig.ALPHA, false);
    ExperimentsConfig.SEED.ifPresent(calicoba::setRNGSeed);

    // Init parameters
    Map<String, PeriodDetector> lastParamValues = new HashMap<>();
    for (String parameterName : model.getParameterNames()) {
      model.setParameter(parameterName, initialParams.get(parameterName));
      Pair<Double, Double> domain = model.getParameterDomain(parameterName);
      calicoba.addParameter(new WritableModelAttribute<>(new ModelParameterProviderSetter(model, parameterName),
          parameterName, domain.getFirst(), domain.getSecond()));
      lastParamValues.put(parameterName, new PeriodDetector(ExperimentsConfig.CALIBRATION_THRESHOLD));
    }

    // Init outputs and objectives
    Map<String, CriticalityFunction> critFunctions = new HashMap<>();
    for (String outputName : model.getOutputNames()) {
      Pair<Double, Double> domain = model.getOutputDomain(outputName);
      calicoba.addOutput(new ReadableModelAttribute<>(new ModelOutputProvider(model, outputName), outputName,
          domain.getFirst(), domain.getSecond()));

      String objectiveName = "obj_" + outputName;
      critFunctions.put(objectiveName, new BaseCriticalityFunction(Arrays.asList(outputName)) {
        @Override
        protected double getImpl(final Map<String, Double> parameterValues) {
          return parameterValues.get(this.parameterNames.get(0))
              - (-4 * Math.sqrt(3 - Math.sqrt(3)) * (3 + Math.sqrt(3)));
        }
      });
      calicoba.addObjective(objectiveName, critFunctions.get(objectiveName));
    }

    calicoba.setInfluenceFunction((pName, pValue, objName, objCrit) -> {
      // Offset current parameter value a little bit to observe criticality
      // variations
      Map<String, Double> currentParameters = model.getParameterNames().stream()
          .collect(Collectors.toMap(Function.identity(), name -> model.getParameter(name)));
      Map<String, Double> testParameters = new HashMap<>(currentParameters);
      testParameters.put(pName, pValue + 1e-6);
      CriticalityFunction cf = critFunctions.get(objName);
      String outputName = objName.substring(4);
      double output1 = model.evaluate(currentParameters).get(outputName);
      double criticality1 = Math.abs(cf.get(Collections.singletonMap(outputName, output1)));
      double output2 = model.evaluate(testParameters).get(outputName);
      double criticality2 = Math.abs(cf.get(Collections.singletonMap(outputName, output2)));

      if (criticality1 < criticality2) {
        return 1.0;
      } else if (criticality1 > criticality2) {
        return -1.0;
      } else {
        return 0.0;
      }
    });

    // Run model until converged or max iterations number is reached
    Map<String, ParameterAgent> parameterAgents = calicoba.getAgentsForType(ParameterAgent.class).stream()
        .collect(Collectors.toMap(a -> a.getAttributeName(), Function.identity()));
    boolean calibrated = false;
    calicoba.setup();
    for (int i = 0; !calibrated && i < ExperimentsConfig.MAX_STEPS_NB; i++) {
      model.update();
      calicoba.step();
      for (Map.Entry<String, PeriodDetector> e : lastParamValues.entrySet()) {
        e.getValue().add(parameterAgents.get(e.getKey()).getAttributeValue());
      }
      calibrated = lastParamValues.values().stream().allMatch(d -> d.isFull() && d.hasConverged());
    }
  }

  private static class ModelParameterProviderSetter implements IValueProviderSetter<Double> {
    private Model model;
    private final String parameterName;

    public ModelParameterProviderSetter(Model model, final String parameterName) {
      this.model = model;
      this.parameterName = parameterName;
    }

    @Override
    public Double get() {
      return this.model.getParameter(this.parameterName);
    }

    @Override
    public void set(Double value) {
      this.model.setParameter(this.parameterName, value);
    }
  }

  private static class ModelOutputProvider implements IValueProvider<Double> {
    private final Model model;
    private final String outputName;

    public ModelOutputProvider(final Model model, final String outputName) {
      this.model = model;
      this.outputName = outputName;
    }

    @Override
    public Double get() {
      return this.model.getOutput(this.outputName);
    }
  }
}
