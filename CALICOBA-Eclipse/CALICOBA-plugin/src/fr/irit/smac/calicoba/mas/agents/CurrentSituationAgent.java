package fr.irit.smac.calicoba.mas.agents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.data.ModelState;
import fr.irit.smac.util.CsvFileWriter;
import fr.irit.smac.util.Logger;
import fr.irit.smac.util.ValueMap;

public class CurrentSituationAgent extends Agent {
  /** Number of neighbors for KNN. */
  private static final int MAX_NEIGHBORS = 10;

  private static CsvFileWriter csvWriter;

  private ModelState modelState;

  private ValueMap parametersActions;

  private List<ParameterAgent> parameters;
  private List<MeasureEntity> measures;
  private List<ObjectiveAgent> objectives;

  private ValueMap currentObjectiveValues;
  private ValueMap previousCriticalities;
  private ValueMap criticalitiesVariations;

  private ValueMap minimums;
  private ValueMap maximums;

  private boolean store;

  public CurrentSituationAgent() {
    // TODO Auto-generated constructor stub
  }

  public boolean toBeStored() {
    return this.store;
  }

  private void keep() {
    this.store = true;
  }

  public ValueMap getCriticalitiesVariations() {
    return this.criticalitiesVariations;
  }

  @Override
  public void perceive() {
    super.perceive();

    this.updateModelState();
    if (this.previousCriticalities != null) {
      this.updateObjectiveVariationsAndActions();
    }
  }

  /**
   * Updates values and the minimum and maximum values of all measures and
   * parameters.
   */
  private void updateModelState() {
    ValueMap measureValues = new ValueMap();
    ValueMap parameterValues = new ValueMap();
    ValueMap objectiveValues = new ValueMap();
    ValueMap mins = new ValueMap();
    ValueMap maxs = new ValueMap();

    Function<ValueMap, Consumer<AgentWithGamaAttribute<?>>> getAttributeMinMax = m -> (a -> {
      String id = a.getId();
      m.put(id, a.getAttributeValue());
      mins.put(id, a.getAttributeMinValue());
      maxs.put(id, a.getAttributeMaxValue());
    });

    this.parameters = this.getWorld().getAgentsForType(ParameterAgent.class);
    this.measures = this.getWorld().getAgentsForType(MeasureEntity.class);
    this.objectives = this.getWorld().getAgentsForType(ObjectiveAgent.class);
    this.parameters.forEach(getAttributeMinMax.apply(parameterValues));
    this.measures.forEach(getAttributeMinMax.apply(measureValues));
    this.objectives.forEach(obj -> objectiveValues.put(obj.getName(), obj.getCriticality()));

    this.minimums = mins;
    this.maximums = maxs;
    this.modelState = new ModelState(measureValues, parameterValues);
    if (this.currentObjectiveValues != null) {
      this.previousCriticalities = this.currentObjectiveValues;
    }
    this.currentObjectiveValues = objectiveValues;

    if (csvWriter == null) {
      String[] header = new String[1 + this.parameters.size() + this.measures.size() + this.objectives.size()];
      header[0] = "cycle";
      int i = 1;
      for (int j = 0; j < this.parameters.size(); j++, i++) {
        header[i] = this.parameters.get(j).getAttributeName();
      }
      for (int j = 0; j < this.measures.size(); j++, i++) {
        header[i] = this.measures.get(j).getAttributeName();
      }
      for (int j = 0; j < this.objectives.size(); j++, i++) {
        header[i] = "crit_" + this.objectives.get(j).getName();
      }

      String fname = Calicoba.OUTPUT_DIR + "current_situation.csv";
      Logger.info("CSV output file: " + fname);
      try {
        csvWriter = new CsvFileWriter(fname, true, true, header);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    Number[] line = new Number[1 + this.parameters.size() + this.measures.size() + this.objectives.size()];
    line[0] = Calicoba.instance().getCycle();
    int i = 1;
    for (int j = 0; j < this.parameters.size(); j++, i++) {
      line[i] = this.parameters.get(j).getAttributeValue();
    }
    for (int j = 0; j < this.measures.size(); j++, i++) {
      line[i] = this.measures.get(j).getAttributeValue();
    }
    for (int j = 0; j < this.objectives.size(); j++, i++) {
      line[i] = this.objectives.get(j).getCriticality();
    }

    try {
      csvWriter.writeLine(line);
      csvWriter.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void updateObjectiveVariationsAndActions() {
    // Get actions performed by parameters on the last cycle
    this.parametersActions = new ValueMap();
    this.parameters.forEach(p -> {
      this.parametersActions.put(p.getAttributeName(), p.getLastAction().getFirst());
    });

    // Compute variations of criticalities between current and last cycles
    this.criticalitiesVariations = new ValueMap();
    for (Map.Entry<String, Double> e : this.currentObjectiveValues.entrySet()) {
      String key = e.getKey();
      this.criticalitiesVariations.put(key, e.getValue() - this.previousCriticalities.get(key));
    }
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    boolean situationSelected = this.getWorld().getAgentsForType(SituationAgent.class).stream()
        .anyMatch(SituationAgent::isSelected);
    if (!situationSelected && this.criticalitiesVariations != null) {
      this.getWorld().addAgent(this.createNewSituation());
      Logger.info("Created new situation from current one.");
    }
    this.selectSituations();
  }

  public void selectSituations() {
    // XXX constant
    Map<SituationAgent, Double> agents = this.getClosestSituations(MAX_NEIGHBORS, 3);
    agents.keySet().forEach(SituationAgent::select);
    Logger.info(agents);
  }

  /*
   * KNN
   */

  /**
   * Returns the K closest situations for the current model state. Uses the KNN
   * algorithm.
   *
   * @param k The maximum number of situations to select.
   * @param n A coefficient to apply to k for the measure-based selection.
   * @return The selected situations with their normalized distance; may be empty.
   */
  private Map<SituationAgent, Double> getClosestSituations(int k, int n) {
    List<SituationAgent> situations = this.getWorld().getAgentsForType(SituationAgent.class);

    Map<SituationAgent, Double> knnMeasures = this.knn(situations, n * k,
        ms -> ms.distanceMeasures(this.modelState, this.minimums, this.maximums));

    Map<SituationAgent, Double> knnParameters = this.knn(new ArrayList<>(knnMeasures.keySet()), k,
        ms -> ms.distanceParameters(this.modelState, this.minimums, this.maximums));

    return knnParameters;
  }

  /**
   * Returns the K closest situations in the given list based on the given
   * distance function. Uses the KNN algorithm.
   *
   * @param situations  The list of situations.
   * @param k           The number of closest situations to select.
   * @param getDistance A function that returns the distance relative to a
   *                    situation.
   * @return The selected situations with their distance; can be empty.
   */
  private Map<SituationAgent, Double> knn(List<SituationAgent> situations, int k,
      Function<ModelState, Double> getDistance) {

    Map<SituationAgent, Double> knn = new HashMap<>(k);

    for (SituationAgent situation : situations) {
      double distance = getDistance.apply(situation.getModelState());

      if (knn.size() < k) {
        knn.put(situation, distance);
      } else {
        SituationAgent farthestSituation = null;
        double maxDistance = Double.NEGATIVE_INFINITY;

        // Look for the farthest selected situation.
        for (Map.Entry<SituationAgent, Double> e : knn.entrySet()) {
          double d = e.getValue();
          if (d >= maxDistance) {
            maxDistance = d;
            farthestSituation = e.getKey();
          }
        }

        // Remove farthest situation if the current one is closer.
        if (knn.get(farthestSituation) > distance) {
          knn.remove(farthestSituation);
          knn.put(situation, distance);
        }
      }
    }

    return knn;
  }

  public SituationAgent createNewSituation() {
    return new SituationAgent(this.modelState, this.parametersActions, this.criticalitiesVariations);
  }
}
