package fr.irit.smac.calicoba.mas.agents;

import java.io.IOException;
import java.util.Arrays;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.data.VariationRequest;
import fr.irit.smac.calicoba.mas.agents.data.Way;
import fr.irit.smac.calicoba.mas.agents.phases.Action;
import fr.irit.smac.calicoba.mas.agents.phases.Representations;
import fr.irit.smac.calicoba.mas.model_attributes.WritableAgentAttribute;
import fr.irit.smac.util.CsvFileWriter;

/**
 * This type of agent represents a float input of the target model. Parameter
 * agents can modify the value of the input their are associated to based on
 * requests received from other agents.
 *
 * @author Damien Vergnet
 */
public class ParameterAgent extends AgentWithGamaAttribute<WritableAgentAttribute<Double>> {
  /** Variation direction for the last executed action. */
  private Way lastWay;
  /** Action currently executed. */
  private Action currentAction;
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
  public ParameterAgent(WritableAgentAttribute<Double> parameter) {
    super(parameter);
    this.representations = new Representations();
    double attributeRange = parameter.getMax() - parameter.getMin();
    this.deltaMin = 0.0001 * attributeRange;
    this.deltaMax = 0.001 * attributeRange;
  }

  /**
   * Returns the last action performed by this agent.
   * 
   * @return True if this agent increased its value, false if it decreased it,
   *         null otherwise.
   */
  public Boolean getLastAction() {
    return this.lastWay != null ? this.lastWay.increase() : null;
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    if (!this.requests.isEmpty()) {
      this.representations.update(this.requests, this.getAttributeValue(), this.getWorld().getCycle());
      VariationRequest mostCriticalRequest = this.requests.stream()
          .max((r1, r2) -> Double.compare(r1.criticality, r2.criticality)).get();

      if ((this.currentAction == null || this.currentAction.isDelayOver())
          && mostCriticalRequest.criticality == Calicoba.instance().getMostCritical().getCriticality()) {
        // TEMP
        int d = 6;
        if (this.getAttributeName().equals("param_preys_birth_rate")
            || this.getAttributeName().equals("param_predation_efficiency")) {
          d = 1;
        }
        this.currentAction = new Action(mostCriticalRequest.senderName, mostCriticalRequest.way, d);
//            this.representations.estimateDelay(mostCriticalRequest.senderName));
      }

      if (this.currentAction != null && !this.currentAction.isDelayOver()) {
        if (!this.currentAction.isExecuted()) {
//          this.updateValue(this.currentAction.getWay());
          this.addToParameterValue(0.05 * (this.currentAction.getWay().increase() ? 1 : -1)); // TEMP
          this.lastWay = this.currentAction.getWay(); // TEMP
          this.currentAction.setExecuted();
        } else {
          this.currentAction.decreaseRemainingSteps();
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
   * Updates the value of this parameter according to the given direction.
   * 
   * @param way The way in which to modify this parameter’s value.
   * @note See Appendix B of Luc Pons’ thesis.
   */
  private void updateValue(Way way) {
    if (this.lastWay != null) {
      if (way.increase()) {
        if (this.lastWay.increase()) {
          this.delta *= 2;
        } else {
          this.delta /= 3;
        }
        this.capDelta();
        this.addToParameterValue(this.delta);

      } else {
        if (this.lastWay.increase()) {
          this.delta /= 3;
        } else {
          this.delta *= 2;
        }
        this.capDelta();
        this.addToParameterValue(-this.delta);
      }

    } else {
      this.delta = this.deltaMin;
      this.addToParameterValue(this.delta * (way.increase() ? 1 : -1));
    }

    this.lastWay = way;
  }

  /**
   * Caps the value of delta between deltaMin and deltaMax.
   */
  private void capDelta() {
    this.delta = Math.max(this.deltaMin, Math.min(this.deltaMax, this.delta));
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
