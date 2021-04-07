package fr.irit.smac.calicoba.mas.agents;

import java.io.IOException;
import java.util.Optional;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.data.CriticalityFunctionParameters;
import fr.irit.smac.calicoba.mas.agents.data.Direction;
import fr.irit.smac.calicoba.mas.agents.data.VariationRequest;
import fr.irit.smac.util.CsvFileWriter;
import fr.irit.smac.util.Pair;

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
  private AgentWithGamaAttribute<?, ?> relativeAgent;
  /** Current value of the relative agent. */
  private double relativeValue;
  /** Current criticality. */
  private double criticality;
  /** Whether the relative value is below the target range. */
  private boolean belowObjective;
  /** Parameters of the criticality function. */
  private final CriticalityFunctionParameters critFunctionParams;

  private CsvFileWriter fw;

  /**
   * Creates a new satisfaction agent.
   * 
   * @param name               This agent’s name.
   * @param relativeAgent      Name of the agent this one will monitor and send
   *                           requests to.
   * @param critFunctionParams Parameters of the criticality function.
   */
  public SatisfactionAgent(final String name, AgentWithGamaAttribute<?, ?> relativeAgent,
      final CriticalityFunctionParameters critFunctionParams) {
    this.name = name;
    this.relativeAgent = relativeAgent;
    this.critFunctionParams = critFunctionParams;
  }

  @Override
  public void perceive() {
    super.perceive();
    this.relativeValue = this.relativeAgent.getAttributeValue();
    // DEBUG
    System.out.println(this.relativeValue);
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    Pair<Double, Boolean> res = this.computeCriticality();
    this.criticality = res.getFirst();
    this.belowObjective = Optional.ofNullable(res.getSecond()).orElse(false);
    Direction direction;

    if (this.criticality > 0) {
      direction = this.belowObjective ? Direction.INCREASE : Direction.DECREASE;
    } else {
      direction = Direction.STAY;
    }

    this.relativeAgent.onRequest(new VariationRequest(this.name, this.criticality, direction));

    // TEST
    if (this.getWorld().canDumpData()) {
      if (this.getWorld().getCycle() == 0) {
        String fName = this.name + "_" + this.critFunctionParams;
        try (CsvFileWriter fw = new CsvFileWriter(Calicoba.OUTPUT_DIR + "/" + fName + ".csv", false, true, "x",
            "crit(x)")) {
          double init = this.relativeValue;
          double o = 0.1 * Math.abs(this.critFunctionParams.sup - this.critFunctionParams.inf);
          for (this.relativeValue = this.critFunctionParams.inf - o; this.relativeValue < this.critFunctionParams.sup
              + o; this.relativeValue += 0.01) {
            fw.writeLine(this.relativeValue, this.computeCriticality().getFirst());
          }
          this.relativeValue = init;
        } catch (IOException e) {
          e.printStackTrace();
        }

        String fname = this.getName();
        try {
          this.fw = new CsvFileWriter(Calicoba.OUTPUT_DIR + "/" + fname + ".csv", false, true, "cycle", "measure",
              "crit");
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      try {
        this.fw.writeLine(new Number[] { this.getWorld().getCycle(), this.relativeValue, this.criticality });
        this.fw.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
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
      double g1 = -2 * 100 / (p.nullMin - p.inf); // Fixed
      double g2 = -2 * 100 / (p.sup - p.nullMax); // Fixed
      double d1 = -g1 * (p.nullMin - p.infl1) / 2;
      double d2 = -g2 * (p.infl2 - p.nullMax) / 2;

      if (x < p.infl1) {
        return new Pair<>(g1 * (Math.pow(x - p.infl1, 2) / (2 * (p.infl1 - p.inf))) + g1 * (x - p.infl1) + d1, true);
      } else if (x < p.nullMin) {
        return new Pair<>(-g1 * (Math.pow(x - p.infl1, 2) / (2 * (p.nullMin - p.infl1))) + g1 * (x - p.infl1) + d1,
            true);
      } else if (x <= p.nullMax) {
        return new Pair<>(0.0, null);
      } else if (x < p.infl2) {
        return new Pair<>(-g2 * (Math.pow(p.infl2 - x, 2) / (2 * (p.infl2 - p.nullMax))) + g2 * (p.infl2 - x) + d2,
            false);
      } else {
        return new Pair<>(g2 * (Math.pow(p.infl2 - x, 2) / (2 * (p.sup - p.infl2))) + g2 * (p.infl2 - x) + d2, false);
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

  /**
   * For tests.
   * 
   * @return The value of the relative agent.
   */
  double getRelativeValue() {
    return this.relativeValue;
  }

  @Override
  public String toString() {
    return String.format("obj_%s(%f)", this.getName(), this.criticality);
  }
}
