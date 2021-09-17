package fr.irit.smac.calicoba.mas.agents;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.mas.agents.actions.Direction;
import fr.irit.smac.calicoba.mas.agents.criticality.AllTimeAbsoluteNormalizer;
import fr.irit.smac.calicoba.mas.agents.criticality.CriticalityFunction;
import fr.irit.smac.calicoba.mas.agents.criticality.Normalizer;
import fr.irit.smac.calicoba.mas.agents.messages.CriticalityMessage;
import fr.irit.smac.util.CsvFileWriter;

/**
 * This type of agent represents an objective or constraint on a set of model
 * outputs.
 *
 * @author Damien Vergnet
 */
public class ObjectiveAgent extends Agent {
  /** This agent’s name. */
  private final String name;

  private List<ParameterAgent> parameterAgents;
  private List<OutputAgent> outputAgents;
  /** Current value of the monitored outputs. */
  private Map<String, Double> outputValues;

  private CriticalityFunction function;
  /** Raw value returned by the objective function. */
  private double objectiveValue;
  /** Current normalized criticality. */
  private double criticality;

  private Normalizer critNormalizer;

  private CsvFileWriter fw;

  /**
   * Creates a new satisfaction agent.
   * 
   * @param name          This agent’s name.
   * @param function      The criticality function.
   * @param relativeAgent The list of output agents this agent uses in its
   *                      criticality function.
   */
  public ObjectiveAgent(final String name, CriticalityFunction function, OutputAgent... outputAgents) {
    this.name = name;
    this.function = function;
    this.outputAgents = Arrays.asList(outputAgents);
    this.critNormalizer = new AllTimeAbsoluteNormalizer();
  }

  @Override
  public void perceive() {
    super.perceive();

    if (this.parameterAgents == null) {
      this.parameterAgents = this.getWorld().getAgentsForType(ParameterAgent.class);
    }

    this.outputValues = this.outputAgents.stream()
        .collect(Collectors.toMap(OutputAgent::getAttributeName, OutputAgent::getAttributeValue));
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    double oldValue = this.objectiveValue;
    this.objectiveValue = this.function.get(this.outputValues);
    // Normalize new objective value
    this.criticality = this.critNormalizer.normalize(this.objectiveValue);
    final Direction variation;
    if (oldValue < this.objectiveValue) {
      variation = Direction.INCREASE;
    } else if (oldValue > this.objectiveValue) {
      variation = Direction.DECREASE;
    } else {
      variation = Direction.NONE;
    }

    this.parameterAgents.forEach(pa -> pa.onMessage(new CriticalityMessage(this, this.criticality, variation)));

    if (this.getWorld().canDumpData()) {
      if (this.getWorld().getCycle() == 0) {
        int outputsNb = this.outputValues.size();

        String fname = this.getName();
        String[] header = new String[3 + outputsNb];
        header[0] = "cycle";
        int i = 0;
        for (Map.Entry<String, Double> e : this.outputValues.entrySet()) {
          header[i + 1] = e.getKey();
          i++;
        }
        header[header.length - 2] = "raw value";
        header[header.length - 1] = "crit";
        try {
          this.fw = new CsvFileWriter(this.getWorld().dumpDirectory() + fname + ".csv", false, true, header);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      Number[] values = new Number[3 + this.outputAgents.size()];
      values[0] = this.getWorld().getCycle();
      int i = 0;
      for (Map.Entry<String, Double> e : this.outputValues.entrySet()) {
        values[i + 1] = e.getValue();
        i++;
      }
      values[values.length - 2] = this.objectiveValue;
      values[values.length - 1] = this.criticality;
      try {
        this.fw.writeLine((Object[]) values);
        this.fw.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * @return This agent’s name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * @return This agent’s current criticality.
   */
  public double getCriticality() {
    return this.criticality;
  }

  @Override
  public String toString() {
    return String.format("obj_%s(%f)", this.getName(), this.criticality);
  }
}
