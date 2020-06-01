package fr.irit.smac.calicoba.mas.agents;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;

import fr.irit.smac.calicoba.WritableAgentAttribute;
import fr.irit.smac.calicoba.mas.FloatValueMessage;
import fr.irit.smac.calicoba.mas.Message;
import fr.irit.smac.util.Triplet;

/**
 * Parameter agents control the parameters of the target model. Their actions
 * are based on the current objectives and passed situations.
 * 
 * @author Damien Vergnet
 */
public class ParameterAgent extends AgentWithAttribute<WritableAgentAttribute<Double>> {
  /** Tells wether the attribute is a floating point value. */
  private final boolean isFloat;

  /** This agent’s memory. */
  private Map<ParameterAgentContext, ParameterAgentMemoryEntry> memory;
  /** The current context. */
  private ParameterAgentContext currentContext;
  /** The current objective values. */
  private Map<String, Double> currentObjectivesValues;
  /** The context of the last action. */
  private ParameterAgentContext lastContext;
  /** The latest performed action. */
  private double lastAction;
  /** The values of the objectives when the last action was performed. */
  private Map<String, Double> lastObjectivesValues;
  private Optional<String> helpedObjective;
  private Optional<Double> expectedVariation;

  // TEMP
  private boolean manual;
  private Scanner sc = new Scanner(System.in);

  // TEMP fixed value for now
  private static final double AMOUNT = 0.01;
  /** Number of neighbors for KNN. */
  private static final int MAX_NEIGHBORS = 10;

  /**
   * Creates a new Parameter agent for the given parameter.
   * 
   * @param parameter The GAMA agent’s attribute/parameter.
   * @param isFloat   Wether the attribute is a floating point value.
   */
  public ParameterAgent(WritableAgentAttribute<Double> parameter, boolean isFloat) {
    super(parameter);
    this.isFloat = isFloat;
    this.memory = new HashMap<>();
    this.helpedObjective = Optional.empty();
    this.expectedVariation = Optional.empty();
    this.manual = true;
  }

  /**
   * Reads the measures, objectives and other parameters values.
   */
  @Override
  public void perceive() {
    super.perceive();

    Map<String, Double> measures = new HashMap<>();
    Map<String, Double> parameters = new HashMap<>();
    this.currentObjectivesValues = new HashMap<>();
    Message message;

    while ((message = this.getMessage()) != null) {
      if (message instanceof FloatValueMessage) {
        FloatValueMessage msg = (FloatValueMessage) message;
        Agent sender = message.getSender();
        String id = sender.getId();
        double value = msg.getValue();

        if (sender instanceof MeasureAgent) {
          measures.put(id, value);
        }
        else if (sender instanceof ParameterAgent) {
          parameters.put(id, value);
        }
        else if (sender instanceof ObjectiveAgent) {
          this.currentObjectivesValues.put(((ObjectiveAgent) sender).getName(), value);
        }
      }
    }
    parameters.put(this.getId(), this.getGamaAttribute().getValue());

    this.currentContext = new ParameterAgentContext(measures, parameters);

    // Adding the previous context and action to the memory.
    if (this.lastContext != null && !this.memory.containsKey(this.lastContext)) {
      Map<String, Double> variations = new HashMap<>();
      for (Map.Entry<String, Double> e : this.currentObjectivesValues.entrySet()) {
        String key = e.getKey();
        variations.put(key, e.getValue() - this.lastObjectivesValues.get(key));
      }
      ParameterAgentMemoryEntry entry = new ParameterAgentMemoryEntry(this.lastAction, variations);
      this.memory.put(this.lastContext, entry);
    }
  }

  /**
   * Looks for the best action to perform for the current context and objectives
   * then performs it. If none were found, a random action is performed.
   */
  @Override
  public void decideAndAct() {
    super.decideAndAct();

    Triplet<Double, Optional<String>, Optional<Double>> result = this.getAction();
    double action = result.getFirst();

    this.helpedObjective = result.getSecond();
    this.expectedVariation = result.getThird();

    this.addToParameterValue(action);

    this.lastContext = this.currentContext;
    this.lastAction = action;
    this.lastObjectivesValues = this.currentObjectivesValues;
  }

  /**
   * Returns the best action to perform.
   * 
   * @return The best action to perform, if any, the objective for which the
   *         action is beneficial and the expected variation.
   */
  private Triplet<Double, Optional<String>, Optional<Double>> getAction() {
    // TEMP to validate the memory.
    if (this.manual) {
      System.out.print("> ");
      String input = this.sc.nextLine().trim().toLowerCase();

      if (!"m".equals(input)) {
        double action = 0;

        if ("i".equals(input)) {
          action = AMOUNT;
        }
        else if ("d".equals(input)) {
          action = -AMOUNT;
        }

        return new Triplet<>(action, Optional.empty(), Optional.empty());
      }
      else {
        this.manual = false;
      }
    }

    // Should always exist.
    String mostCritical = this.currentObjectivesValues.entrySet().stream()
        .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
        .map(e -> e.getKey()).get();

    // Keep helping the last helped objective as long as it is the most critical.
    if (this.helpedObjective.isPresent() && this.helpedObjective.get().equals(mostCritical)) {
      double variation = this.currentObjectivesValues.get(this.helpedObjective.get())
          - this.lastObjectivesValues.get(this.helpedObjective.get());
      double action = this.lastAction;
      double expectedVariation = variation;

      // The criticality increased, do the opposite action.
      if (variation > 0) {
        action = -action;
        expectedVariation = -expectedVariation;
      }

      return new Triplet<>(action, this.helpedObjective, Optional.of(expectedVariation));
    }

    Optional<ParameterAgentMemoryEntry> opt = this.getEntriesForContext(this.currentContext).stream()
        // Filter out entries where the current most critical objective’s criticality
        // increased.
        .filter(e -> e.getObjectivesVariations().get(mostCritical) <= 0)
        // Get the entry where the criticality decreased the most.
        .min((e1, e2) -> e1.getObjectivesVariations().get(mostCritical)
            .compareTo(e2.getObjectivesVariations().get(mostCritical)));

    if (opt.isPresent()) {
      ParameterAgentMemoryEntry entry = opt.get();
      return new Triplet<>(entry.getAction(), Optional.of(mostCritical),
          Optional.of(entry.getObjectivesVariations().get(mostCritical)));
    }

    double defaultAction = 0;
    double random = Math.random();

    if (random < 0.33) {
      defaultAction = -AMOUNT;
    }
    else if (random > 0.66) {
      defaultAction = AMOUNT;
    }

    return new Triplet<>(defaultAction, Optional.empty(), Optional.empty());
  }

  /**
   * Returns the K closest memory entries for the given context based on the KNN
   * algorithm.
   * 
   * @param context The reference context.
   * @return The corresponding entries; can be empty.
   */
  private Collection<ParameterAgentMemoryEntry> getEntriesForContext(ParameterAgentContext context) {
    List<Pair<Double, ParameterAgentMemoryEntry>> knn = new LinkedList<>();

    for (Map.Entry<ParameterAgentContext, ParameterAgentMemoryEntry> e : this.memory.entrySet()) {
      double distance = e.getKey().distance(context);
      Pair<Double, ParameterAgentMemoryEntry> entry = new Pair<>(distance, e.getValue());

      if (knn.size() < MAX_NEIGHBORS) {
        knn.add(entry);
      }
      else {
        int indexOfMax = knn.indexOf(Collections.max(knn, (a, b) -> (int) Math.signum(a.getKey() - b.getKey())));

        if (knn.get(indexOfMax).getKey() > distance) {
          knn.remove(indexOfMax);
          knn.add(entry);
        }
      }
    }

    return knn.stream().map(Pair::getValue).collect(Collectors.toSet());
  }

  /**
   * Adds the given value to the parameter.
   * 
   * @param value The value to add.
   */
  private void addToParameterValue(double value) {
    double v = this.getGamaAttribute().getValue().doubleValue() //
        + (this.isFloat ? value : Math.floor(value));
    this.getGamaAttribute().setValue(v);
  }

  /**
   * @return The latest performed action, the associated objective that it should
   *         help and the expected variation.
   */
  public Triplet<Double, Optional<String>, Optional<Double>> getLastAction() {
    return new Triplet<>(this.lastAction, this.helpedObjective, this.expectedVariation);
  }

  /**
   * @return A copy of the memory of this agent.
   */
  public Map<ParameterAgentContext, ParameterAgentMemoryEntry> getMemory() {
    return new HashMap<>(this.memory);
  }

  @Override
  public String toString() {
    return super.toString() + String.format("{parameter=%s,isFloat=%b}", this.getGamaAttribute(), this.isFloat);
  }
}
