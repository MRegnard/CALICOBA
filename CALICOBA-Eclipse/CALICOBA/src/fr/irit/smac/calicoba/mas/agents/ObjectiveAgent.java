package fr.irit.smac.calicoba.mas.agents;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.mas.agents.criticality.AllTimeAbsoluteNormalizer;
import fr.irit.smac.calicoba.mas.agents.criticality.CriticalityFunction;
import fr.irit.smac.calicoba.mas.agents.criticality.Normalizer;
import fr.irit.smac.calicoba.mas.agents.messages.CriticalityMessage;
import fr.irit.smac.calicoba.mas.agents.messages.OscillationDetectedMessage;
import fr.irit.smac.util.CsvFileWriter;
import fr.irit.smac.util.FixedCapacityQueue;
import fr.irit.smac.util.Logger;
import fr.irit.smac.util.Utilities;

/**
 * This type of agent represents an objective or constraint on a measure or
 * parameter respectively.
 *
 * @author Damien Vergnet
 */
public class ObjectiveAgent extends Agent {
  /** This agent’s name. */
  private final String name;

  private List<ParameterAgent> parameterAgents;
  /** The agent this agent monitors and will send requests to. */
  private List<MeasureAgent> measureAgents;
  /** Current value of the relative measures. */
  private Map<String, Double> measureValues;

  private CriticalityFunction function;
  /** Raw value returned by the objective function. */
  private double objectiveValue;
  /** Current normalized criticality. */
  private double criticality;
  private double criticalityVariation;

  private Normalizer critNormalizer;

  /** Maximum queue length for cycles detection. */
  private static final int MAX_QUEUE_LENGTH = 10;
  // Lists used to detect criticality cycles
  private FixedCapacityQueue<Double> lastCriticalities;

  private CsvFileWriter fw;

  /**
   * Creates a new satisfaction agent.
   * 
   * @param name          This agent’s name.
   * @param function      The criticality function.
   * @param relativeAgent The list of measure agents this agent uses in its
   *                      criticality function.
   */
  public ObjectiveAgent(final String name, CriticalityFunction function, MeasureAgent... measureAgents) {
    this.name = name;
    this.function = function;
    this.measureAgents = Arrays.asList(measureAgents);
    this.critNormalizer = new AllTimeAbsoluteNormalizer();
    this.lastCriticalities = new FixedCapacityQueue<>(MAX_QUEUE_LENGTH);
  }

  @Override
  public void perceive() {
    super.perceive();

    if (this.parameterAgents == null) {
      this.parameterAgents = this.getWorld().getAgentsForType(ParameterAgent.class);
    }

    this.measureValues = this.measureAgents.stream()
        .collect(Collectors.toMap(MeasureAgent::getAttributeName, MeasureAgent::getAttributeValue));
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    double oldCrit = this.criticality;
    this.objectiveValue = this.function.get(this.measureValues);
    // Normalize new objective value
    this.criticality = this.critNormalizer.normalize(this.objectiveValue);
    this.criticalityVariation = Math.abs(this.criticality) - Math.abs(oldCrit);

    // Update queue
    this.lastCriticalities.add(this.criticality);
    Logger.debug(this.lastCriticalities);
    Logger.debug(Arrays.toString(Utilities.getCycle(this.lastCriticalities).orElse(new Number[0])));

    if (Utilities.hasCycle(this.lastCriticalities)) {
      this.parameterAgents.forEach(pa -> pa.onMessage(new OscillationDetectedMessage(this)));
    }

    this.parameterAgents.forEach(pa -> pa.onMessage(new CriticalityMessage(this, this.criticality)));

    // TEST
    if (this.getWorld().canDumpData()) {
      if (this.getWorld().getCycle() == 0) {
        int measuresNb = this.measureValues.size();

        String fname = this.getName();
        String[] header = new String[3 + measuresNb];
        header[0] = "cycle";
        int i = 0;
        for (Map.Entry<String, Double> e : this.measureValues.entrySet()) {
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

      Number[] values = new Number[3 + this.measureAgents.size()];
      values[0] = this.getWorld().getCycle();
      int i = 0;
      for (Map.Entry<String, Double> e : this.measureValues.entrySet()) {
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

  public double getCriticalityVariation() {
    return this.criticalityVariation;
  }

  @Override
  public String toString() {
    return String.format("obj_%s(%f)", this.getName(), this.criticality);
  }
}
