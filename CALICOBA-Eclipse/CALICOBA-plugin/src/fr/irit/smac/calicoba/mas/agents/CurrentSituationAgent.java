package fr.irit.smac.calicoba.mas.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.math3.util.Pair;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.World;
import fr.irit.smac.calicoba.mas.messages.ActionProposalMessage;
import fr.irit.smac.calicoba.mas.messages.ActionProposalRequestMessage;
import fr.irit.smac.calicoba.mas.messages.FloatValueMessage;
import fr.irit.smac.calicoba.mas.messages.PerformedActionMessage;
import fr.irit.smac.calicoba.mas.messages.PerformedActionRequestMessage;
import fr.irit.smac.calicoba.mas.messages.ValueRequestMessage;
import fr.irit.smac.util.ValueMap;

public class CurrentSituationAgent extends Agent<CurrentSituationAgent.State> {
  /** Number of neighbors for KNN. */
  private static final int MAX_NEIGHBORS = 10;

  private ModelState currentModelState;
  private ModelState previousModelState;

  /** The current objectives’ values. */
  private ValueMap currentObjectivesValues;
  /** The values of the objectives during the previous cycle. */
  private ValueMap previousObjectivesValues;
  private ValueMap objectivesVariations;

  private List<ActionProposalMessage> actionProposals;
  private Map<String, Pair<Double, SituationAgent>> parametersActions;

  private ValueMap tempMeasures;
  private ValueMap tempParameters;
  private ValueMap tempObjectives;
  private Map<String, Pair<Double, SituationAgent>> tempActions;

  private List<ParameterAgent> parameters;
  private List<MeasureAgent> measures;
  private List<ObjectiveAgent> objectives;

  private ValueMap minimums;
  private ValueMap maximums;

  private boolean firstIteration;

  private int remainingValuesToUpdate;
  private int remainingProposals;

  public CurrentSituationAgent() {
    super();
    this.firstIteration = true;
    this.setState(State.REQUESTING_UPDATES);
  }

  @Override
  public void onGamaCycleBegin() {
    super.onGamaCycleBegin();

    if (this.firstIteration) {
      this.parameters = this.getWorld().getAgentsForType(ParameterAgent.class);
      this.measures = this.getWorld().getAgentsForType(MeasureAgent.class);
      this.objectives = this.getWorld().getAgentsForType(ObjectiveAgent.class);
    }

    if (this.hasState(State.WAITING)) {
      this.setState(State.REQUESTING_OBJ_PARAMS);
    }
  }

  @Override
  public void onGamaCycleEnd() {
    super.onGamaCycleEnd();

    this.firstIteration = false;
  }

  /*
   * Perception
   */

  @Override
  public void perceive() {
    super.perceive();

    switch (this.getState()) {
      case REQUESTING_UPDATES:
        this.onRequestingUpdates();
        break;

      case UPDATING:
        this.onUpdating();
        break;

      case DISPATCHING:
        this.onDispatching();
        break;

      case REQUESTING_OBJ_PARAMS:
        this.onRequestingObjectivesAndParameters();
        break;

      case UPDATING_OBJ_PARAMS:
        this.onUpdating2();
        break;

      default:
        this.flushMessages();
        break;
    }
  }

  private void onRequestingUpdates() {
    this.tempMeasures = new ValueMap();
    this.tempParameters = new ValueMap();
    // Used during first iteration only
    this.tempObjectives = new ValueMap();

    this.flushMessages();
  }

  private void onUpdating() {
    this.iterateOverMessages(m -> {
      if (m instanceof FloatValueMessage) {
        FloatValueMessage fm = (FloatValueMessage) m;
        double value = fm.getValue();
        Agent<?> sender = m.getSender();

        switch (fm.getValueNature()) {
          case MEASURE_VALUE:
            this.tempMeasures.put(((MeasureAgent) sender).getAttributeName(), value);
            this.remainingValuesToUpdate--;
            break;

          case PARAM_VALUE:
            this.tempParameters.put(((ParameterAgent) sender).getAttributeName(), value);
            this.remainingValuesToUpdate--;
            break;

          // Used during first iteration only
          case CRITICALITY:
            this.tempObjectives.put(((ObjectiveAgent) sender).getName(), value);
            this.remainingValuesToUpdate--;
            break;

          default:
            break;
        }
      }
    });
  }

  private void onDispatching() {
    this.actionProposals.clear();

    this.iterateOverMessages(m -> {
      if (m instanceof ActionProposalMessage) {
        this.actionProposals.add((ActionProposalMessage) m);
        this.remainingProposals -= this.actionProposals.size();
      }
    });
  }

  private void onRequestingObjectivesAndParameters() {
    this.tempObjectives = new ValueMap();
    this.tempActions = new HashMap<>();

    this.flushMessages();
  }

  private void onUpdating2() {
    this.iterateOverMessages(m -> {
      if (m instanceof FloatValueMessage) {
        FloatValueMessage fm = (FloatValueMessage) m;
        double value = fm.getValue();
        Agent<?> sender = m.getSender();

        switch (fm.getValueNature()) {
          case CRITICALITY:
            this.tempObjectives.put(((ObjectiveAgent) sender).getName(), value);
            this.remainingValuesToUpdate--;
            break;

          default:
            break;
        }
      }
      else if (m instanceof PerformedActionMessage) {
        PerformedActionMessage pam = (PerformedActionMessage) m;
        ParameterAgent parameter = (ParameterAgent) pam.getSender();
        double action = pam.getAction();
        SituationAgent chosenSituation = pam.getChosenSituation();

        this.tempActions.put(parameter.getAttributeName(), new Pair<>(action, chosenSituation));

        this.remainingValuesToUpdate--;
      }
    });
  }

  /*
   * Decision and action
   */

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    switch (this.getState()) {
      case REQUESTING_UPDATES:
        this.requestValues();
        break;

      case UPDATING:
        this.checkUpdateFinished();
        break;

      case REQUESTING_PROPOSALS:
        this.selectSituations();
        break;

      case DISPATCHING:
        this.dispatchProposals();
        break;

      case REQUESTING_OBJ_PARAMS:
        this.requestObjectivesAndActions();
        break;

      case UPDATING_OBJ_PARAMS:
        this.checkUpdate2Finished();
        break;

      case COMPUTING:
        this.updateOrCreateSituation();
        break;

      default:
        break;
    }
  }

  private void requestValues() {
    ValueRequestMessage message = new ValueRequestMessage(this);

    this.parameters.forEach(p -> p.onMessage(message));
    this.measures.forEach(m -> m.onMessage(message));
    this.remainingValuesToUpdate = this.parameters.size() + this.measures.size();
    if (this.firstIteration) {
      this.objectives.forEach(o -> o.onMessage(message));
      this.remainingValuesToUpdate += this.objectives.size();
    }

    this.setState(State.UPDATING);
  }

  private void checkUpdateFinished() {
    if (this.remainingValuesToUpdate <= 0) {
      this.previousModelState = this.currentModelState;
      this.currentModelState = new ModelState(this.tempMeasures, this.tempParameters);
      if (this.firstIteration) {
        this.currentObjectivesValues = this.tempObjectives;
      }

      this.updateMinimumsAndMaximums();
      this.setState(State.REQUESTING_PROPOSALS);
    }
  }

  private void selectSituations() {
    Map<SituationAgent, Double> situations = this.getClosestSituations(MAX_NEIGHBORS, 3);

    situations.keySet().forEach(s -> s.onMessage(new ActionProposalRequestMessage(this)));
    this.remainingProposals = situations.size();

    this.setState(State.DISPATCHING);
  }

  private void dispatchProposals() {
    this.parameters.forEach(p -> this.actionProposals.forEach(a -> p.onMessage(a)));

    if (this.remainingProposals <= 0) {
      this.setState(State.WAITING);
    }
  }

  private void requestObjectivesAndActions() {
    ValueRequestMessage message1 = new ValueRequestMessage(this);
    PerformedActionRequestMessage message2 = new PerformedActionRequestMessage(this);

    this.objectives.forEach(o -> o.onMessage(message1));
    this.parameters.forEach(p -> p.onMessage(message2));
    this.remainingValuesToUpdate = this.objectives.size() + this.parameters.size();

    this.setState(State.UPDATING_OBJ_PARAMS);
  }

  private void checkUpdate2Finished() {
    if (this.remainingValuesToUpdate <= 0) {
      this.previousObjectivesValues = this.currentObjectivesValues;
      this.currentObjectivesValues = this.tempObjectives;
      this.parametersActions = this.tempActions;

      this.setState(State.COMPUTING);
    }
  }

  private void updateOrCreateSituation() {
    ValueMap objectivesVariations = new ValueMap();

    for (Map.Entry<String, Double> e : this.currentObjectivesValues.entrySet()) {
      String key = e.getKey();
      objectivesVariations.put(key, e.getValue() - this.previousObjectivesValues.get(key));
    }
    this.objectivesVariations = objectivesVariations;

    this.createNewSituation();
  }

  private void createNewSituation() {
    ValueMap parametersAction = new ValueMap(this.parametersActions.size());

    for (Map.Entry<String, Pair<Double, SituationAgent>> entry : this.parametersActions.entrySet()) {
      parametersAction.put(entry.getKey(), entry.getValue().getFirst());
    }

    this.getWorld().addAgent(new SituationAgent(
        this.previousModelState, parametersAction, this.objectivesVariations));
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
  private Map<SituationAgent, Double> knn(List<SituationAgent> situations,
      int k, Function<ModelState, Double> getDistance) {

    Map<SituationAgent, Double> knn = new HashMap<>(k);

    for (SituationAgent situation : situations) {
      double distance = getDistance.apply(situation.getModelState());

      if (knn.size() < k) {
        knn.put(situation, distance);
      }
      else {
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
   * Updates the minimum and maximum values of all measures and parameters.
   */
  private void updateMinimumsAndMaximums() {
    World world = Calicoba.instance().getWorld();
    List<ParameterAgent> params = world.getAgentsForType(ParameterAgent.class);
    List<MeasureAgent> measures = world.getAgentsForType(MeasureAgent.class);

    ValueMap mins = new ValueMap();
    ValueMap maxs = new ValueMap();

    Consumer<AgentWithAttribute<?, ?>> c = a -> {
      String id = a.getId();
      mins.put(id, a.getAttributeMinValue());
      maxs.put(id, a.getAttributeMaxValue());
    };

    params.forEach(c);
    measures.forEach(c);

    this.minimums = mins;
    this.maximums = maxs;
  }

  public enum State {
    REQUESTING_UPDATES, UPDATING, REQUESTING_PROPOSALS, DISPATCHING, WAITING, REQUESTING_OBJ_PARAMS,
    UPDATING_OBJ_PARAMS, COMPUTING;
  }
}
