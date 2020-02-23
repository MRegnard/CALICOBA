model Driver

import "Main.gaml"

species Driver skills: [advanced_driving] {
  rgb color <- rnd_color(255);

  reflex time_to_go when: final_target = nil {
    current_path <- compute_path(graph: world.road_network, target: one_of(RoadNode.population));
  }

  reflex move when: final_target != nil {
    do drive();
  }

  aspect car3D {
    if (current_road != nil) {
      point loc <- compute_location();
      draw box(vehicle_length, 1, 1) at: loc rotate: heading color: color;
      draw triangle(0.5) depth: 1.5 at: loc rotate: heading + 90 color: color;
    }
  }

  point compute_location {
    float val <- Road(current_road).lanes - current_lane + 0.5;
    val <- on_linked_road ? -val : val;
    return location + {cos(heading + 90) * val, sin(heading + 90) * val};
  }
}
