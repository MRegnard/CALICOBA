model RoadNode

import "Main.gaml"
import "Vehicle.gaml"

species RoadNode schedules: [] skills: [skill_road_node] {
  bool is_traffic_signal;
  int time_to_change <- 100;
  int counter <- rnd(time_to_change);
  bool is_traffic_counter <- false;

  int vehicle_count <- 0;

  point gps_location -> { CRS_transform(location, "EPSG:4326").location };

  map<float, int> measures <- [];

  reflex dynamic when: is_traffic_signal {
    counter <- counter + 1;
    if (counter >= time_to_change) {
      counter <- 0;
      stop[0] <- empty(stop[0]) ? roads_in : [];
    }
  }

  action increase_count {
    vehicle_count <- vehicle_count + 1;
  }

  reflex count when: is_traffic_counter {
    ask world.file_writer {
      do add_people(myself.vehicle_count, myself.gps_location);
    }
    vehicle_count <- 0;
  }

  aspect geom3D {
    if (is_traffic_signal) {
      draw box(1, 1, 10) color: #black;
      draw sphere(5) at: {location.x, location.y, 12} color: empty(stop[0]) ? #green : #red;
    }
    if (is_traffic_counter) {
      draw circle(5) color: #blue;
    }
  }
}
