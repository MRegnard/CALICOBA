package fr.irit.smac.calicoba.mas.agents.data;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import fr.irit.smac.calicoba.mas.agents.AgentWithGamaAttribute;
import fr.irit.smac.calicoba.mas.agents.MeasureAgent;

/**
 * A correlation matrix defines the influences between measures and parameters
 * or other measures.
 * 
 * @author Damien Vergnet
 */
public class CorrelationMatrix {
  private final Map<MeasureAgent, Map<AgentWithGamaAttribute<?, ?>, Boolean>> columns;

  /**
   * Creates a new matrix.
   * 
   * @param columns A map associating measure agents to pairs of measure/parameter
   *                agents and their corresponding influence coefficient (> 0 for
   *                +, < 0 for -).
   */
  public CorrelationMatrix(final Map<MeasureAgent, Map<AgentWithGamaAttribute<?, ?>, Boolean>> columns) {
    this.columns = columns;
  }

  /**
   * Returns the coefficients for the given measure.
   * 
   * @param measureAgent The measure agent.
   * @return A map associating measure or parameter agents to their respective
   *         influence coefficent.
   */
  public Map<AgentWithGamaAttribute<?, ?>, Boolean> getColumnForMeasure(MeasureAgent measureAgent) {
    return Optional.ofNullable(this.columns.get(measureAgent)).orElse(Collections.emptyMap());
  }
}
