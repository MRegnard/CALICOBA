package fr.irit.smac.calicoba.mas.agents;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.WritableAgentAttribute;
import fr.irit.smac.calicoba.mas.FloatValueMessage;
import fr.irit.smac.calicoba.mas.Message;

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

  // TEMP fixed value for now
  private static final double AMOUNT = 0.1;
  // TODO find a good value
  private static final double PROXIMITY_THRESHOLD = 0.1;

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
          this.currentObjectivesValues.put(id, value);
        }
      }
    }
    parameters.put(this.getId(), this.getGamaAttribute().getValue());

    this.currentContext = new ParameterAgentContext(measures, parameters);
  }

  /**
   * Looks for the best action to perform for the current context and objectives
   * then performs it. If none were found, a random action is performed.
   */
  @Override
  public void decideAndAct() {
    super.decideAndAct();

    double defaultAction = 0;
    double precision = 1e-6;
    Optional<Double> opt = this.getActionForContext(this.currentContext, this.currentObjectivesValues);

    if (!opt.isPresent()) {
      // DEBUG
      System.out.println(this.currentObjectivesValues);
      if (!this.currentObjectivesValues.entrySet().stream().allMatch(e -> e.getValue() <= precision)) {
        defaultAction = Math.signum(Math.random() - 0.5) * AMOUNT;
      }
    }

    double valueToAdd = opt.orElse(defaultAction);
    this.addToParameterValue(valueToAdd);

    this.lastContext = this.currentContext;
    this.lastAction = valueToAdd;
    this.lastObjectivesValues = this.currentObjectivesValues;
  }

  /**
   * Returns the best action to perform for the given context and objectives.
   * 
   * @param context    The context.
   * @param objectives The objectives’ values.
   * @return The best action to perform, if any.
   */
  private Optional<Double> getActionForContext(ParameterAgentContext context, Map<String, Double> objectives) {
    Optional<ParameterAgentMemoryEntry> opt = this.getEntriesForContext(this.currentContext).stream()
        .reduce((min, e) -> {
          for (Map.Entry<String, Double> ee : min.getObjectivesVariations().entrySet()) {
            double v = e.getObjectivesVariations().get(ee.getKey());
            if (v <= 0 && v < ee.getValue()) {
              return e;
            }
          }
          return min;
        });
    return opt.map(e -> e.getAction());
  }

  /**
   * Returns all memory entries for the given context.
   * 
   * @param context The reference context.
   * @return The corresponding entries; can be empty.
   */
  private Collection<ParameterAgentMemoryEntry> getEntriesForContext(ParameterAgentContext context) {
    return this.memory.entrySet().stream().filter(e -> e.getKey().similarity(context) <= PROXIMITY_THRESHOLD)
        .map(e -> e.getValue()).collect(Collectors.toSet());
  }

  /**
   * Adds the given value to the parameter.
   * 
   * @param value The value to add.
   */
  private void addToParameterValue(double value) {
    double v = this.getGamaAttribute().getValue().doubleValue() + value;
    this.getGamaAttribute().setValue(this.isFloat ? v : Math.floor(v));
  }

  /**
   * @return The latest performed action.
   */
  public double getLastAction() {
    return this.lastAction;
  }

  @Override
  public String toString() {
    return super.toString() + String.format("{parameter=%s,isFloat=%b}", this.getGamaAttribute(), this.isFloat);
  }
}
