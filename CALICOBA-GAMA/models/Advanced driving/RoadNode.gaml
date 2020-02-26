model RoadNode

import "Main.gaml"

species RoadNode skills: [skill_road_node] {
  bool is_traffic_signal;
  int time_to_change <- 100;
  int counter <- rnd(time_to_change);

  reflex dynamic when: is_traffic_signal {
    counter <- counter + 1;
    if (counter >= time_to_change) {
      counter <- 0;
      stop[0] <- empty(stop[0]) ? roads_in : [];
    }
  }

  reflex count {
    // TODO
  }

  aspect geom3D {
    if (is_traffic_signal) {
      draw box(1, 1, 10) color: #black;
      draw sphere(5) at: {location.x, location.y, 12} color: empty(stop[0]) ? #green : #red;
    }
  }
}
