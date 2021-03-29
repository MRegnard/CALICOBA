package fr.irit.smac.calicoba.mas.agents;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.data.Direction;
import fr.irit.smac.calicoba.mas.agents.data.VariationRequest;
import fr.irit.smac.calicoba.mas.agents.phases.Action;
import fr.irit.smac.calicoba.mas.agents.phases.Representations;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProviderSetter;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.util.CsvFileWriter;

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
  private Representations representations;
  private CsvFileWriter fw;

  /**
   * Creates a new parameter agent for a given model input.
   *
   * @param parameter The associated model attribute.
   */
  public ParameterAgent(WritableModelAttribute<Double, IValueProviderSetter<Double>> parameter) {
    super(parameter);
    this.representations = new Representations();
    double attributeRange = parameter.getMax() - parameter.getMin();
    this.deltaMin = 0.0001 * attributeRange;
    this.deltaMax = 0.001 * attributeRange;
    this.lastDirection = Optional.empty();
    this.currentAction = Optional.empty();
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

  // TODO prendre en compte la direction STAY
  @Override
  public void decideAndAct() {
    super.decideAndAct();

    if (!this.requests.isEmpty()) {
      this.representations.update(this.requests, this.getAttributeValue(), this.getWorld().getCycle());
      VariationRequest mostCriticalRequest = this.selectRequest();

      if (!this.currentAction.isPresent() || this.currentAction.get().isDelayOver()) {
        // TEMP
//        int d = 6;
//        if (this.getAttributeName().equals("param_preys_birth_rate")
//            || this.getAttributeName().equals("param_predation_efficiency")) {
//          d = 1;
//        }
        this.currentAction = Optional.of(new Action(mostCriticalRequest.senderName, mostCriticalRequest.direction, // d));
            this.representations.estimateDelay(mostCriticalRequest.senderName)));
      }

      if (this.currentAction.isPresent() && !this.currentAction.get().isDelayOver()) {
        if (!this.currentAction.get().isExecuted()) {
          this.updateValue(this.currentAction.get().getDirection());
          this.currentAction.get().setExecuted();
        } else {
          this.currentAction.get().decreaseRemainingSteps();
        }
      }

      this.requests.clear();
    }

    // TEMP
    if (this.getWorld().getCycle() == 0) {
      String fname = this.getAttributeName();
      try {
        String[] objs = this.representations.keySet().stream().sorted().toArray(String[]::new);
        objs = Arrays.copyOf(objs, objs.length + 1);
        objs[objs.length - 1] = "cycle";
        this.fw = new CsvFileWriter(Calicoba.OUTPUT_DIR + "/" + fname + ".csv", false, true, objs);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    try {
      Object[] values = this.representations.entrySet().stream() //
          .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())) //
          .map(e -> e.getValue().estimateDelay()) //
          .toArray(Object[]::new);
      values = Arrays.copyOf(values, values.length + 1);
      values[values.length - 1] = this.getWorld().getCycle();
      this.fw.writeLine(values);
      this.fw.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Selects a request from all received requests using the following algorithm:
   * 
   * <pre>
   * group each request by their criticality
   * sort the generated pairs (criticality, requests) by descending criticality
   * for each pair (criticality, requests):
   *   if all requests have the same direction:
   *     return one of these requests
   * return one request from the most critical ones
   * </pre>
   * 
   * @return The selected request.
   */
  private VariationRequest selectRequest() {
    SortedMap<Double, List<VariationRequest>> freq = this.requests.stream()
        // Sorted by decreasing criticalities
        .collect(Collectors.groupingBy(r -> -r.criticality, TreeMap::new, Collectors.toList()));

    for (List<VariationRequest> l : freq.values()) {
      // All requested directions for the current criticality are the same
      // -> return one of these requests
      if (l.stream().allMatch(r -> r.direction.equals(l.get(0).direction))) {
        return l.get(0);
      }
    }

    List<VariationRequest> l = freq.get(freq.firstKey());
    return l.get(this.getWorld().getRNG().nextInt(l.size()));
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
            default:
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
            default:
              delta = this.delta;
              break;
          }
          break;

        default:
          delta = this.delta;
          break;
      }

      return Math.max(this.deltaMin, Math.min(this.deltaMax, delta));
    }).orElse(this.deltaMin);

    this.delta = 0.05; // TEMP
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
