package fr.irit.smac.calicoba.mas.agents.phases;

import java.util.HashMap;

import fr.irit.smac.calicoba.mas.agents.data.VariationRequest;

/**
 * This class abstracts the representations of satisfaction agents phases.
 * 
 * @author Damien Vergnet
 */
public class Representations extends HashMap<String, ClientRepresentation> {
  private static final long serialVersionUID = -1226332934593810290L;

  /**
   * Creates an empty representation.
   */
  public Representations() {
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
      if (!this.containsKey(id)) {
        this.put(id, new ClientRepresentation());
      }
      this.get(id).update(r, agentValue, stepNb);
    });
  }

  /**
   * Estimates the delay for the given satisfaction agent.
   * 
   * @param satisfactionAgentName The satisfaction agent’s name.
   * @return The delay.
   */
  public int estimateDelay(String satisfactionAgentName) {
    return this.get(satisfactionAgentName).estimateDelay();
  }
}
