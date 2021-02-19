package fr.irit.smac.calicoba.mas.agents;

import java.util.Map;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.mas.agents.data.CorrelationMatrix;
import fr.irit.smac.calicoba.mas.agents.data.VariationRequest;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;

/**
 * This type of agent represents a float output of the target model.
 * 
 * @author Damien Vergnet
 */
public class MeasureAgent extends AgentWithGamaAttribute<ReadableModelAttribute<Double>> {
  private Map<AgentWithGamaAttribute<?>, Boolean> neighbors;
  private boolean sentRequest;

  /**
   * Creates new a measure agent for a given model output.
   * 
   * @param attribute The associated model attribute.
   */
  public MeasureAgent(final ReadableModelAttribute<Double> attribute) {
    super(attribute);
  }

  /**
   * Initializes the neighbors of this agent based on the given correlation
   * matrix.
   * 
   * @param matrix The matrix to use.
   */
  public void init(final CorrelationMatrix matrix) {
    this.neighbors = matrix.getColumnForMeasure(this).entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * @return True if this agent has sent a request during the last call to its
   *         {@link #decideAndAct()} method, false otherwise.
   */
  public boolean hasSentRequest() {
    return this.sentRequest;
  }

  @Override
  public void decideAndAct() {
    super.decideAndAct();

    this.sentRequest = false;
    this.requests.forEach(r -> {
      // Forward requests to all neighbors.
      this.neighbors.entrySet().forEach(e -> {
        VariationRequest vr = r;
        // Influence is negative, switch request’s sign
        if (!e.getValue()) {
          vr = r.getOppositeRequest();
        }
        e.getKey().onRequest(vr);
        this.sentRequest = true;
      });
    });
    this.requests.clear();
  }
}
