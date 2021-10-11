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
import fr.irit.smac.calicoba.test_util.SobolSequenceGenerator;
import fr.irit.smac.calicoba.test_util.Utils;
import fr.irit.smac.util.CsvFileWriter;
import fr.irit.smac.util.Logger;
import fr.irit.smac.util.Pair;
import fr.irit.smac.util.Triplet;

public class BatchExperiments {
  static final String ROOT = "experiments/";

  /** Number of runs for each model. */
  private static final int RUNS_NB = 200;

  private static final Map<Model, List<Map<String, Double>>> MODELS;

  static {
    MODELS = new HashMap<>();
    MODELS.put(new Model1(), Arrays.asList(map(12), map(-12)));
    MODELS.put(new Model2(), Arrays.asList(map(-11, 12), map(35, 12)));
    MODELS.put(new Model3(), Arrays.asList(map(2, 12), map(-2, 12)));
    // Partial solutions
    MODELS.put(new Model4(), Arrays.asList(map(-21, 20), map(-19, 20)));
  }

  private static Map<String, Double> map(double... values) {
    Map<String, Double> m = new HashMap<>(values.length);
    for (int i = 0; i < values.length; i++) {
      m.put("p" + (i + 1), values[i]);
    }
    return m;
  }

  public static void main(String[] args) throws IOException {
    Logger.setStdoutLevel(Logger.Level.INFO);

    String root = "calicoba_output/" + ROOT;

    Path path = Paths.get(root);
    if (!Files.exists(path)) {
      Files.createDirectories(path);
    }

    for (Map.Entry<Model, List<Map<String, Double>>> e : MODELS.entrySet()) {
      Model model = e.getKey();
      List<Map<String, Double>> solutions = e.getValue();
      List<String> parameterNames = new ArrayList<>(model.getParameterNames());
      parameterNames.sort(null);

      try (CsvFileWriter fw = new CsvFileWriter(root + model.getId() + ".csv", false, true, "P(0)", "result",
          "closest solution", "distance", "speed")) {
        for (double[] v : new SobolSequenceGenerator(parameterNames.size(), RUNS_NB)) {
          Map<String, Double> pInit = new HashMap<>();
          for (int i = 0; i < v.length; i++) {
            pInit.put(parameterNames.get(i), sobolToParam(v[i], 100));
          }

          Triplet<Map<String, Double>, List<Double>, Integer> result = evaluateModel(model, pInit, solutions);

          Map<String, Double> parameterValues = result.getFirst();
          int calibrationSpeed = result.getThird();
          List<Double> distances = result.getSecond();
          double minDistance = Double.POSITIVE_INFINITY;
          int solutionIndex = -1;
          for (int i = 0; i < distances.size(); i++) {
            double x = distances.get(i);
            if (x < minDistance) {
              minDistance = x;
              solutionIndex = i;
            }
          }
          fw.writeLine(Utils.mapToString(pInit), Utils.mapToString(parameterValues),
              Utils.mapToString(solutions.get(solutionIndex)), minDistance, calibrationSpeed);
        }
      }
    }
  }

  private static double sobolToParam(double s, int amplitude) {
    return Math.floor(s * amplitude - (amplitude / 2.0));
  }

  private static Triplet<Map<String, Double>, List<Double>, Integer> evaluateModel(Model model,
      Map<String, Double> initialParams, List<Map<String, Double>> solutions) {
    String dirname = ROOT + model.getId() + "/" + Utils.mapToString(initialParams);
    Calicoba calicoba = new Calicoba(true, dirname, ExperimentsConfig.LEARN, ExperimentsConfig.ALPHA, false);
    ExperimentsConfig.SEED.ifPresent(calicoba::setRNGSeed);

    // Init parameters
    Map<String, PeriodDetector> periodDetectors = new HashMap<>();
    for (String parameterName : model.getParameterNames()) {
      if (ExperimentsConfig.FREE_PARAM.map(s -> !s.equals(parameterName)).orElse(false)) {
        model.setParameter(parameterName, solutions.get(0).get(parameterName));
      } else {
        model.setParameter(parameterName, initialParams.get(parameterName));
      }
      Pair<Double, Double> domain = model.getParameterDomain(parameterName);
      calicoba.addParameter(new WritableModelAttribute<>(new ModelParameterProviderSetter(model, parameterName),
          parameterName, domain.getFirst(), domain.getSecond()));
      periodDetectors.put(parameterName, new PeriodDetector(ExperimentsConfig.CALIBRATION_THRESHOLD));
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
          return parameterValues.get(this.parameterNames.get(0)); // model output - 0
        }
      });
      calicoba.addObjective(objectiveName, critFunctions.get(objectiveName));
    }

    calicoba.setInfluenceFunction((pName, pValue, objName, objCrit) -> {
      // Offset current parameter value by a little bit to observe criticality
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
    // M2
    int calibrationSpeed = 0;
    for (int i = 0; !calibrated && i < ExperimentsConfig.MAX_STEPS_NB; i++, calibrationSpeed = i) {
      model.update();
      calicoba.step();
      for (Map.Entry<String, PeriodDetector> e : periodDetectors.entrySet()) {
        e.getValue().add(parameterAgents.get(e.getKey()).getAttributeValue());
      }
      calibrated = periodDetectors.values().stream().allMatch(d -> d.isFull() && d.hasConverged());
    }

    // Get average of N values for each parameter to get more accurate values
    Map<String, Double> paramValues = model.getParameterNames().stream() //
        .collect(Collectors.toMap( //
            Function.identity(), //
            name -> periodDetectors.get(name).stream() //
                .mapToDouble(Double::doubleValue) //
                .average() //
                .getAsDouble() //
        ));

    // Evaluate distance to each known solution (M1)
    List<Double> distances = new ArrayList<>(solutions.size());
    for (Map<String, Double> solution : solutions) {
      double distance = 0;
      for (Map.Entry<String, Double> e : paramValues.entrySet()) {
        distance += Math.pow(solution.get(e.getKey()) - e.getValue(), 2);
      }
      distances.add(Math.sqrt(distance));
    }

    return new Triplet<>(paramValues, distances, calibrationSpeed - ExperimentsConfig.CALIBRATION_THRESHOLD);
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
