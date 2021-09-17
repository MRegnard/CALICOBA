package fr.irit.smac.calicoba.experimentations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
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
import fr.irit.smac.calicoba.test_util.SobolSequenceGenerator;
import fr.irit.smac.util.CsvFileWriter;
import fr.irit.smac.util.FixedCapacityQueue;
import fr.irit.smac.util.Pair;
import fr.irit.smac.util.Triplet;

public class Experiments {
  static final String ROOT = "experiments/";

  public static final Optional<Long> SEED = Optional.ofNullable(1L);
  /** Number of runs for each model. */
  public static final int RUNS_NB = 200;
  /** Maximum number of cycles for each run. */
  public static final int MAX_STEPS_NB = 1000;
  /**
   * Number of cycles the model’s outputs have to be stable to consider it
   * calibrated.
   */
  public static final int CALIBRATION_THRESHOLD = 10;
  public static final boolean LEARN = false;
  public static final double ALPHA = 0;

  private static final Map<Model, List<Map<String, Double>>> MODELS;

  static {
    MODELS = new HashMap<>();
    MODELS.put(new Model1(), Arrays.asList(map(12), map(-12)));
    MODELS.put(new Model2(), Arrays.asList(map(-11, 12), map(35, 12)));
    MODELS.put(new Model3(), Arrays.asList(map(2, 12)));
    // Partial solutions
    MODELS.put(new Model4(), Arrays.asList(map(-21, 20), map(-19, 20)));
  }

  private static Map<String, Double> map(double... values) {
    Map<String, Double> m = new HashMap<>(values.length);
    for (int i = 0; i < values.length; i++) {
      m.put("p" + (values.length > 1 ? i + 1 : ""), values[i]);
    }
    return m;
  }

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
          fw.writeLine(mapToString(pInit), mapToString(parameterValues), mapToString(solutions.get(solutionIndex)),
              minDistance, calibrationSpeed);
        }
      }
    }
  }

  private static double sobolToParam(double s, int amplitude) {
    return Math.floor(s * amplitude - (amplitude / 2.0));
  }

  private static String mapToString(Map<String, Double> m) {
    StringJoiner sj = new StringJoiner(";");
    for (Map.Entry<String, Double> e : m.entrySet()) {
      sj.add(e.getKey() + "=" + e.getValue());
    }
    return sj.toString();
  }

  private static Triplet<Map<String, Double>, List<Double>, Integer> evaluateModel(Model model,
      Map<String, Double> initialParams, List<Map<String, Double>> solutions) {
    String dirname = ROOT + model.getId() + "/" + mapToString(initialParams);
    Calicoba calicoba = new Calicoba(true, dirname, LEARN, ALPHA, false);
    if (SEED.isPresent()) {
      calicoba.setRNGSeed(SEED.get());
    }

    // Init parameters
    Map<String, FixedCapacityQueue<Double>> lastParamValues = new HashMap<>();
    for (String parameterName : model.getParameterNames()) {
      model.setParameter(parameterName, initialParams.get(parameterName));
      Pair<Double, Double> domain = model.getParameterDomain(parameterName);
      calicoba.addParameter(new WritableModelAttribute<>(new ModelParameterProviderSetter(model, parameterName),
          parameterName, domain.getFirst(), domain.getSecond()));
      lastParamValues.put(parameterName, new FixedCapacityQueue<>(CALIBRATION_THRESHOLD));
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
      double criticality1 = cf.get(Collections.singletonMap(outputName, output1));
      double output2 = model.evaluate(testParameters).get(outputName);
      double criticality2 = cf.get(Collections.singletonMap(outputName, output2));

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
    for (int i = 0; !calibrated && i < MAX_STEPS_NB; i++, calibrationSpeed = i) {
      model.update();
      calicoba.step();
      for (Map.Entry<String, FixedCapacityQueue<Double>> e : lastParamValues.entrySet()) {
        e.getValue().add(parameterAgents.get(e.getKey()).getAttributeValue());
      }
      calibrated = lastParamValues.values().stream()
          .allMatch(queue -> queue.size() == CALIBRATION_THRESHOLD && hasConverged(new LinkedList<>(queue)));
    }

    // Get average of N values for each parameter to get more accurate values
    Map<String, Double> paramValues = model.getParameterNames().stream() //
        .collect(Collectors.toMap( //
            Function.identity(), //
            name -> lastParamValues.get(name).stream() //
                .mapToDouble(Double::doubleValue) //
                .average() //
                .getAsDouble() //
        ));

    // Evaluate distance to each known solution (M1)
    List<Double> distances = new ArrayList<>(solutions.size());
    for (Map<String, Double> solution : solutions) {
      double distance = 0;
      for (Map.Entry<String, Double> paramValue : paramValues.entrySet()) {
        distance += Math.pow(solution.get(paramValue.getKey()) - paramValue.getValue(), 2);
      }
      distances.add(Math.sqrt(distance));
    }

    return new Triplet<>(paramValues, distances, calibrationSpeed - CALIBRATION_THRESHOLD);
  }

  /**
   * Checks whether the given series has converged, i.e. if values are periodic.
   */
  private static boolean hasConverged(List<Double> series) {
    return approximatePeriodDetection(series).stream().anyMatch(p -> p.getFirst() < 1e-5);
  }

  /**
   * Tries to find the period of the values in the given series.
   * 
   * @param series Series of values to detect periodicity of.
   * @return A list of distance/period pairs.
   * @author Otunba, R. and Lin, J., 2014. APT: Approximate Period Detection in
   *         Time Series. In SEKE (pp. 490-494).
   */
  private static List<Pair<Double, Integer>> approximatePeriodDetection(List<Double> series) {
    List<Pair<Double, Integer>> patterns = new LinkedList<>();
    double dist = Double.POSITIVE_INFINITY;
    int period = 2;
    double cDist = 0;

    for (int i = 2; i <= series.size() / 2; i++) {
      cDist = euclidianDistance(series, i, dist);
      if (cDist < dist) {
        dist = cDist;
        if (i == period + 1) {
          patterns.remove(patterns.size() - 1);
        }
        period = i;
        patterns.add(new Pair<>(dist, period));
      }
    }

    // TODO compute average periode pattern (eq. 1)

    patterns.sort((a, b) -> a.getFirst().compareTo(b.getFirst()));

    return patterns;
  }

  /**
   * Computes the euclidian distance of the given series to a generated series of
   * the given period. The second series is generated using the first
   * {@code period} elements of the given series.
   * 
   * @param series          Series to use.
   * @param period          Period of the series to generate.
   * @param currentDistance Current minimal distance for early stopping.
   * @return The euclidian distance.
   * @author Otunba, R. and Lin, J., 2014. APT: Approximate Period Detection in
   *         Time Series. In SEKE (pp. 490-494).
   */
  private static double euclidianDistance(List<Double> series, int period, double currentDistance) {
    double newDist = 0;
    for (int j = 0; j < series.size(); j++) {
      newDist += Math.pow(series.get(j) - series.get(j % period), 2);
      if (newDist >= currentDistance * currentDistance) { // Optimisation
        return currentDistance;
      }
    }
    return Math.sqrt(newDist);
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
