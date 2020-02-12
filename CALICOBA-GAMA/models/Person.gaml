model Person

import "Global.gaml"

species Person skills: [moving] {
  /** Target point of the agent. */
  point target;
  /** Probability of leaving the building. */
  float leaving_proba <- 0.05;
  /** Speed of the agent. */
  float speed <- 5 #km / #h;

  rgb color <- rnd_color(255);

  /**
   * Reflex to leave the building to another building.
   */
  reflex leave when: target = nil and flip(leaving_proba) {
    point new_target <- any_location_in(one_of(Building.population));
    if (world.building_at(location) != world.building_at(new_target)) {
      target <- new_target;
    }
  }

  /**
   * Reflex to move to the target building following the road network.
   */
  reflex move when: target != nil {
    path p <- goto(target: target, on: world.road_network, recompute_path: false, move_weights: world.road_weights, return_path: true);
    if (p = nil or empty(p.edges)) {
      target <- nil;
    }
  }

  aspect default {
    draw circle(5 #m) color: color;
    if (world.show_names) {
      draw name color: #black;
    }
  }
}
