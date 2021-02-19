package fr.irit.smac.calicoba.mas.agents;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.math3.util.Pair;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.data.CriticalityFunctionParameters;
import fr.irit.smac.calicoba.mas.agents.data.VariationRequest;
import fr.irit.smac.util.CsvFileWriter;

/**
 * This type of agent represents an objective or constraint on a measure or
 * parameter respectively.
 *
 * @author Damien Vergnet
 */
public class SatisfactionAgent extends Agent {
  /** This agent’s name. */
  private final String name;

  /** The agent this agent monitors and will send requests to. */
  private AgentWithGamaAttribute<?> relativeAgent;
  /** Current value of the relative agent. */
  private double relativeValue;
  /** Current criticality. */
  private double criticality;
  /** Whether the relative value is below the target range. */
  private boolean belowObjective;
  /** Parameters of the criticality function. */
  private final CriticalityFunctionParameters critFunctionParams;

  /**
   * Creates a new satisfaction agent.
   * 
   * @param name               This agent’s name.
   * @param relativeAgent      Name of the agent this one will monitor and send
   *                           requests to.
   * @param critFunctionParams Parameters of the criticality function.
   */
  public SatisfactionAgent(final String name, AgentWithGamaAttribute<?> relativeAgent,
      final CriticalityFunctionParameters critFunctionParams) {
    this.name = name;
    this.relativeAgent = relativeAgent;
    this.critFunctionParams = critFunctionParams;
    // TEMP
    String fname = name + "_" + critFunctionParams.toString();
    try (
        CsvFileWriter fw = new CsvFileWriter(Calicoba.OUTPUT_DIR + "/" + fname + ".csv", false, true, "x", "crit(x)")) {
      double o = 0.1 * (critFunctionParams.sup - critFunctionParams.inf);
      for (this.relativeValue = critFunctionParams.inf - o; this.relativeValue < critFunctionParams.sup
          + o; this.relativeValue += 0.01) {
        fw.writeLine(this.relativeValue, this.computeCriticality().getFirst());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void perceive() {
    super.perceive();
    this.relativeValue = this.relativeAgent.getAttributeValue();
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();
    Pair<Double, Boolean> res = this.computeCriticality();
    this.criticality = res.getFirst();
    this.belowObjective = Optional.ofNullable(res.getSecond()).orElse(false);

    if (this.criticality > 0) {
      this.relativeAgent.onRequest(new VariationRequest(this.name, this.criticality, this.belowObjective));
    }
  }

  /**
   * Computes the criticality from the value of the relative agent using the
   * criticality function’s parameters.
   * 
   * @return A pair containing the criticality value and a boolean indicating
   *         whether the value is below (true), above (false) or within (null) the
   *         target range.
   * @note See Appendix A of Luc Pons’ thesis.
   */
  private Pair<Double, Boolean> computeCriticality() {
    double x = this.relativeValue;
    CriticalityFunctionParameters p = this.critFunctionParams;

    if (x < p.inf || x >= p.sup) {
      return new Pair<>(100.0, x < p.inf);
    } else {
      double γ1 = -2 * 100 / (p.ε1 - p.inf); // Fixed Appendix A definition
      double γ2 = -2 * 100 / (p.sup - p.ε2); // Fixed Appendix A definition
      double δ1 = -γ1 * (p.ε1 - p.η1) / 2;
      double δ2 = -γ2 * (p.η2 - p.ε2) / 2;

      if (x < p.η1) {
        return new Pair<>(γ1 * (Math.pow(x - p.η1, 2) / (2 * (p.η1 - p.inf))) + γ1 * (x - p.η1) + δ1, true);
      } else if (x < p.ε1) {
        return new Pair<>(-γ1 * (Math.pow(x - p.η1, 2) / (2 * (p.ε1 - p.η1))) + γ1 * (x - p.η1) + δ1, true);
      } else if (x <= p.ε2) {
        return new Pair<>(0.0, null);
      } else if (x < p.η2) {
        return new Pair<>(-γ2 * (Math.pow(p.η2 - x, 2) / (2 * (p.η2 - p.ε2))) + γ2 * (p.η2 - x) + δ2, false);
      } else {
        return new Pair<>(γ2 * (Math.pow(p.η2 - x, 2) / (2 * (p.sup - p.η2))) + γ2 * (p.η2 - x) + δ2, false);
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
