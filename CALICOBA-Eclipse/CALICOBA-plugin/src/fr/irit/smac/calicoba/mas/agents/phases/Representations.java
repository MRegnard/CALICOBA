package fr.irit.smac.calicoba.mas.agents.phases;

import java.util.HashMap;
import java.util.Map;

import fr.irit.smac.calicoba.mas.agents.data.VariationRequest;

/**
 * This class abstracts the representations of satisfaction agents phases.
 * 
 * @author Damien Vergnet
 */
public class Representations {
  private Map<String, ClientRepresentation> clientRepresentations;

  /**
   * Creates an empty representation.
   */
  public Representations() {
    this.clientRepresentations = new HashMap<>();
  }

  /**
   * Updates the representations. Adds a new step to the current phase of each
   * satisfaction agent in the requests.
   * 
   * @param requests   The requests received by the parameter agent.
   * @param agentValue The parameter agent’s current value.
   * @param stepNb     The current simulation step.
   */
  public void update(Iterable<VariationRequest> requests, double agentValue, int stepNb) {
    requests.forEach(r -> {
      String id = r.senderName;
      if (!this.clientRepresentations.containsKey(id)) {
        this.clientRepresentations.put(id, new ClientRepresentation());
      }
      this.clientRepresentations.get(id).update(r, agentValue, stepNb);
    });
  }

  /**
   * Estimates the delay for the given satisfaction agent.
   * 
   * @param satisfactionAgentName The satisfaction agent’s name.
   * @return The delay.
   */
  public int estimateDelay(String satisfactionAgentName) {
    return this.clientRepresentations.get(satisfactionAgentName).estimateDelay();
  }
}
