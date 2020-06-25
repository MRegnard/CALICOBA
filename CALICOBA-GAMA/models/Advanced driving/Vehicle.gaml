model Driver

import "Main.gaml"

/**
 * This species represents a vehicle.
 * They have the advanced driving skill.
 * Schedule is set in global.
 */
species Vehicle schedules: [] skills: [advanced_driving] {
  rgb color <- rnd_color(255);
  /** Node this vehicle has to reach next. */
  RoadNode next_node;

  /**
   * Whenever this vehicle has no target, computes a new target from a random road node.
   */
  reflex time_to_go when: final_target = nil {
    current_path <- compute_path(graph: world.road_network, target: one_of(RoadNode.population));
    next_node <- RoadNode(current_target);
  }

  /**
   * If this vehicle has a target, drives then increases the vehicle count of the node
   * it passed over if it is a vehicle counter.
   */
  reflex move when: final_target != nil {
    do drive();
    if (next_node != RoadNode(current_target)) {
      if (next_node.is_traffic_counter) {
        ask next_node {
          do increase_count();
        }
      }
      next_node <- RoadNode(current_target);
    }
  }

  /**
   * Computes the location of this vehicle.
   * 
   * @return the location as a point
   */
  point compute_location {
    float val <- Road(current_road).lanes - current_lane + 0.5;
    if (on_linked_road) {
      val <- -val;
    }
    return location + {cos(heading + 90) * val, sin(heading + 90) * val};
  }

  aspect car3D {
    if (current_road != nil) {
      point loc <- compute_location();
      draw box(vehicle_length, 1, 1) at: loc rotate: heading color: color;
      draw triangle(0.5) depth: 1.5 at: loc rotate: heading + 90 color: color;
    }
  }

  aspect car2D {
    if (current_road != nil) {
      point loc <- compute_location();
      draw triangle(0.5) depth: 1.5 at: loc rotate: heading + 90 color: color;
    }
  }
}
