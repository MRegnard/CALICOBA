package fr.irit.smac.calicoba.mas.agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.data.ActionProposal;
import fr.irit.smac.calicoba.mas.agents.data.ModelState;
import fr.irit.smac.util.ValueMap;

public class Mediator {
  /** Number of neighbors for KNN. */
  private static final int MAX_NEIGHBORS = 10;

  private Calicoba world;

  private List<ParameterAgent> parameters;
  private List<MeasureEntity> measures;
  private List<ObjectiveAgent> objectives;

  private ModelState currentModelState;
  private ModelState previousModelState;

  private ValueMap minimums;
  private ValueMap maximums;

  /** The current objectives’ criticalities. */
  private ValueMap currentCriticalities;
  /** The criticalities of the objectives during the previous cycle. */
  private ValueMap previousCriticalities;
  private ValueMap criticalitiesVariations;

  private Map<String, Pair<Double, SituationAgent>> parametersActions;

  public Mediator(Calicoba world, List<ParameterAgent> parameters, List<MeasureEntity> measures,
      List<ObjectiveAgent> objectives) {
    super();
    this.world = world;
    this.parameters = parameters;
    this.measures = measures;
    this.objectives = objectives;
  }

  public ValueMap getCriticalitiesVariations() {
    return this.criticalitiesVariations.clone();
  }

  public void update() {
    this.updateModelState();

    this.previousCriticalities = this.currentCriticalities;
    this.currentCriticalities = new ValueMap();
    this.objectives.forEach(o -> this.currentCriticalities.put(o.getName(), o.getCriticality()));
  }

  public void updateMemory() {
    // Get actions performed by parameters on the last cycle
    this.parametersActions = new HashMap<>();
    this.parameters.forEach(p -> {
      this.parametersActions.put(p.getAttributeName(), p.getLastAction());
    });

    // Compute variations of criticalities between current and last cycles
    this.criticalitiesVariations = new ValueMap();
    for (Map.Entry<String, Double> e : this.currentCriticalities.entrySet()) {
      String key = e.getKey();
      this.criticalitiesVariations.put(key, e.getValue() - this.previousCriticalities.get(key));
    }

    this.updateOrCreateSituation();
  }

  public void selectSituations() {
    // XXX constant
    List<ActionProposal> proposals = this.getClosestSituations(MAX_NEIGHBORS, 3).keySet().stream()
        .map(SituationAgent::getActionProposal).collect(Collectors.toList());
    this.parameters.forEach(p -> p.suggestActions(proposals));
  }

  private void updateOrCreateSituation() {
    boolean isSituationChosen = this.parametersActions.values().stream().anyMatch(p -> p.getSecond() != null);

    if (this.previousModelState != null && !isSituationChosen) {
      this.createNewSituation();
    }
  }

  private void createNewSituation() {
    ValueMap parametersAction = new ValueMap(this.parametersActions.size());

    for (Map.Entry<String, Pair<Double, SituationAgent>> entry : this.parametersActions.entrySet()) {
      parametersAction.put(entry.getKey(), entry.getValue().getFirst());
    }

    this.world.addAgent(new SituationAgent(this.previousModelState, parametersAction, this.criticalitiesVariations));
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
    List<SituationAgent> situations = this.world.getAgentsForType(SituationAgent.class);

    // Nominal situation: exactly one situation is under the threshold.
    List<SituationAgent> closestSituations = situations.stream().filter(s -> {
      ModelState state = s.getModelState();
      double threshold = 1e-2; // XXX constant
      return state.distanceMeasures(this.currentModelState, this.minimums, this.maximums) < threshold
          && state.distanceParameters(this.currentModelState, this.minimums, this.maximums) < threshold;
    }).collect(Collectors.toList());

    if (closestSituations.size() == 1) {
      return Collections.singletonMap(closestSituations.get(0), 0.0);
    }

    Map<SituationAgent, Double> knnMeasures = this.knn(situations, n * k,
        ms -> ms.distanceMeasures(this.currentModelState, this.minimums, this.maximums));

    Map<SituationAgent, Double> knnParameters = this.knn(new ArrayList<>(knnMeasures.keySet()), k,
        ms -> ms.distanceParameters(this.currentModelState, this.minimums, this.maximums));

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

  /**
   * Updates values and the minimum and maximum values of all measures and
   * parameters.
   */
  private void updateModelState() {
    ValueMap measureValues = new ValueMap();
    ValueMap parameterValues = new ValueMap();
    ValueMap mins = new ValueMap();
    ValueMap maxs = new ValueMap();

    Function<ValueMap, Consumer<AgentWithGamaAttribute<?>>> getAttributeMinMax = m -> (a -> {
      String id = a.getId();
      m.put(id, a.getAttributeValue());
      mins.put(id, a.getAttributeMinValue());
      maxs.put(id, a.getAttributeMaxValue());
    });

    this.parameters.forEach(getAttributeMinMax.apply(parameterValues));
    this.measures.forEach(getAttributeMinMax.apply(measureValues));

    this.minimums = mins;
    this.maximums = maxs;
    this.previousModelState = this.currentModelState;
    this.currentModelState = new ModelState(measureValues, parameterValues);
  }
}
