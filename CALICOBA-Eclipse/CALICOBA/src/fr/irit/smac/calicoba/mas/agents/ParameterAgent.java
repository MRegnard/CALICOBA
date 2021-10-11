package fr.irit.smac.calicoba.mas.agents;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.experiments.ExperimentsConfig;
import fr.irit.smac.calicoba.mas.agents.actions.Action;
import fr.irit.smac.calicoba.mas.agents.actions.Direction;
import fr.irit.smac.calicoba.mas.agents.messages.CriticalityMessage;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProviderSetter;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.util.CsvFileWriter;
import fr.irit.smac.util.Logger;

/**
 * This type of agent represents a float input of the target model. Parameter
 * agents can modify the value of the input their are associated to based on
 * requests received from other agents.
 *
 * @author Damien Vergnet
 */
public class ParameterAgent extends
    AgentWithGamaAttribute<WritableModelAttribute<Double, IValueProviderSetter<Double>>, IValueProviderSetter<Double>> {
  /** Variation direction for the last executed action. */
  private Optional<Direction> lastDirection;
  /** Action currently executed. */
  private Optional<Action> currentAction;
  /** Current AVT delta. */
  private double delta;
  /** Lowest allowed AVT delta. */
  private final double deltaMin;
  /** Highest allowed AVT delta. */
  private final double deltaMax;

  private Map<String, Double> influences;

  private CsvFileWriter fw;

  private Scanner sc;

  /**
   * Creates a new parameter agent for a given model input.
   *
   * @param parameter The associated model attribute.
   */
  public ParameterAgent(WritableModelAttribute<Double, IValueProviderSetter<Double>> parameter) {
    super(parameter);
    this.influences = new HashMap<>();
    double attributeRange = parameter.getMax() - parameter.getMin();
    this.deltaMin = 1e-5 * attributeRange;
    this.deltaMax = 1e-2 * attributeRange;
    this.lastDirection = Optional.empty();
    this.currentAction = Optional.empty();
    this.sc = new Scanner(System.in);
  }

  /**
   * Returns the last action performed by this agent.
   * 
   * @return 1 if this agent increased its value, -1 if it decreased it, 0
   *         otherwise.
   */
  public int getLastAction() {
    return this.lastDirection.map(Direction::getAction).orElse(0);
  }

  /**
   * Returns the influence of this parameter on the given objective.
   * 
   * @param objectiveName Objective’s name.
   * @return The influence coefficient.
   * @throws IllegalArgumentException If no objective with the given name exists.
   */
  public double getInfluence(final String objectiveName) {
    if (!this.influences.containsKey(objectiveName)) {
      throw new IllegalArgumentException(String.format("No objective with name \"%s\".", objectiveName));
    }
    return this.influences.get(objectiveName);
  }

  @Override
  public void perceive() {
    super.perceive();

    if (this.influences.isEmpty()) {
      this.getWorld().getAgentsForType(ObjectiveAgent.class).forEach(oa -> this.influences.put(oa.getName(), 0.5));
    }
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();
    Logger.debug(this.getAttributeName());
    double oldValue = this.getAttributeValue();
    int action = 0;
    int delay = 0;
    String obj = "";

    this.updateInfluences();

    Set<CriticalityMessage> critMessages = this.getMessagesForType(CriticalityMessage.class);
    if (ExperimentsConfig.FREE_PARAM.map(p -> !this.getAttributeName().equals(p)).orElse(false)) { // TEST
      critMessages.clear();
    }
    if (!critMessages.isEmpty()) {
      if (!this.currentAction.isPresent() || this.currentAction.get().isDelayOver()) {
        CriticalityMessage selectedMessage = this.selectRequest();
        double influence = this.influences.get(selectedMessage.getSenderName());
        Direction direction;

        if (selectedMessage.getCriticality() != 0 && influence != 0) {
          direction = influence > 0 ? Direction.DECREASE : Direction.INCREASE;
        } else {
          direction = Direction.NONE;
        }

        if (this.getWorld().manualActions()) {
          System.out.println(String.format("Action for %s:", this.getAttributeName()));
          int i = Integer.parseInt(this.sc.nextLine().trim());
          if (i > 0) {
            direction = Direction.INCREASE;
          } else if (i < 0) {
            direction = Direction.DECREASE;
          } else {
            direction = Direction.NONE;
          }
        }

        this.currentAction = Optional.of(new Action(selectedMessage.getSenderName(), direction, 0));
        obj = selectedMessage.getSenderName();
        Logger.debug(String.format("%s helped %s", this.getAttributeName(), obj));
      }

      if (this.currentAction.isPresent() && !this.currentAction.get().isDelayOver()) {
        if (!this.currentAction.get().isExecuted()) {
          this.updateValue(this.currentAction.get().getDirection());
          action = this.currentAction.get().getDirection().getAction();
          this.currentAction.get().setExecuted();
        } else {
          this.currentAction.get().decreaseRemainingSteps();
        }
      }
    }

    this.messages.clear();

    if (this.getWorld().canDumpData()) {
      if (this.getWorld().getCycle() == 0) {
        String fname = this.getAttributeName();
        try {
          this.fw = new CsvFileWriter(this.getWorld().dumpDirectory() + fname + ".csv", false, true, "cycle", "value",
              "action", "helped obj", "delay");
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      try {
        this.fw.writeLine(new Object[] { this.getWorld().getCycle(), oldValue, action, obj, delay });
        this.fw.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Selects a request from all received requests using the following algorithm:
   * 
   * <pre>
   * group each request by their criticality
   * sort the generated pairs (criticality, requests) by descending criticality
   * for each pair (criticality, requests):
   *   if there is only one request:
   *     return this request
   * return one request from the most critical ones
   * </pre>
   * 
   * @return The selected request.
   */
  private CriticalityMessage selectRequest() {
    SortedMap<Double, List<CriticalityMessage>> freq = this.messages.stream()
        .filter(m -> m instanceof CriticalityMessage) //
        .map(m -> (CriticalityMessage) m)
        // Sorted by decreasing absolute criticalities
        .collect(Collectors.groupingBy(r -> -Math.abs(r.getCriticality()), TreeMap::new, Collectors.toList()));

    for (List<CriticalityMessage> l : freq.values()) {
      // There is only one request for the current criticality, select it.
      if (l.size() == 1) {
        return l.get(0);
      }
    }

    List<CriticalityMessage> l = freq.get(freq.firstKey());
    return l.get(this.getWorld().getRNG().nextInt(l.size()));
  }

  private void updateInfluences() {
    final double epsilon = 1e-5; // Precision around 0

    if (this.lastDirection.isPresent()) {
      Map<String, Double> infl = new HashMap<>();
      for (Map.Entry<String, Double> e : this.influences.entrySet()) {
        final String objName = e.getKey();
        final Double inflValue = e.getValue();
        final ObjectiveAgent objAgent = ((ObjectiveAgent) this.getWorld()
            .getAgent(a -> a instanceof ObjectiveAgent && ((ObjectiveAgent) a).getName().equals(objName)).get());

        double newInfl;
        if (this.getWorld().learnsInfluences()) {
          final Direction critVar = this.getMessagesForType(CriticalityMessage.class).stream()
              .filter(m -> m.getSender() == objAgent) //
              .findFirst() //
              .map(CriticalityMessage::getVariationDirection) //
              .orElse(Direction.NONE);

          if (critVar == Direction.NONE) {
            newInfl = 0;
          } else if (critVar == this.lastDirection.get()) {
            newInfl = 1;
          } else {
            newInfl = -1;
          }
          final double alpha = this.getWorld().getAlpha();
          newInfl = alpha * inflValue + (1 - alpha) * newInfl;
          if (Math.abs(newInfl) < epsilon) {
            newInfl = 0;
          }
        } else {
          final double objValue = objAgent.getCriticality();
          newInfl = this.getWorld().getInfluenceFunction().apply(this.getAttributeName(), this.getAttributeValue(),
              objName, objValue);
        }

        infl.put(objName, newInfl);
      }
      this.influences = infl;
      Logger.debug(this.influences);
    }
  }

  /**
   * Updates the value of this parameter according to the given direction.
   * 
   * @param direction The direction in which to modify this parameter’s value.
   * @note See Appendix B of Luc Pons’ thesis.
   */
  private void updateValue(Direction direction) {
    this.delta = this.lastDirection.map(d -> {
      double delta;

      switch (direction) {
        case INCREASE:
          switch (d) {
            case INCREASE:
              delta = 2 * this.delta;
              break;
            case DECREASE:
              delta = this.delta / 3;
              break;
            default: // NONE
              delta = this.delta;
              break;
          }
          break;

        case DECREASE:
          switch (d) {
            case INCREASE:
              delta = this.delta / 3;
              break;
            case DECREASE:
              delta = 2 * this.delta;
              break;
            default: // NONE
              delta = this.delta;
              break;
          }
          break;

        default: // NONE
          delta = this.delta;
          break;
      }

      return Math.max(this.deltaMin, Math.min(this.deltaMax, delta));
    }).orElse(this.deltaMin);

    if (this.getWorld().manualActions()) {
      this.delta = direction.getAction() < 0 ? 1.1 : 1;
    }
    Logger.debug("δ = " + this.delta);
    this.addToParameterValue(this.delta * direction.getAction());
    this.lastDirection = Optional.of(direction);
  }

  /**
   * Adds the given value to the parameter. The value is capped between min and
   * max values of the associated GAMA attribute.
   *
   * @param value The value to add.
   */
  private void addToParameterValue(double value) {
    double newValue = this.getAttribute().getValue() + value;
    newValue = Math.min(this.getAttributeMaxValue(), newValue);
    newValue = Math.max(this.getAttributeMinValue(), newValue);
    this.getAttribute().setValue(newValue);
  }
}
