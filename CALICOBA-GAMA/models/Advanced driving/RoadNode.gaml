model RoadNode

import "Main.gaml"
import "Vehicle.gaml"

/**
 * This species represents a road node.
 * A node can feature a traffic light and count the number
 * of vehicles that pass over it at each cycle.
 * Schedule is set in global.
 */
species RoadNode schedules: [] skills: [skill_road_node] {
  /** Wether this node features a traffic light. */
  bool is_traffic_signal;
  /** Time it takes to the traffic light to change from one state to another. */
  int time_to_change <- 100;
  /** Time since last traffic light state change. */
  int counter <- rnd(time_to_change);

  /** Wether this node has to count vehicles that pass over it. */
  bool is_traffic_counter <- false;
  /** Number of vehicles that passed over this node. */
  int vehicle_count <- 0;

  /** GPS location of this node. */
  point gps_location -> { CRS_transform(location, "EPSG:4326").location };

  /** Measures this node has to fulfill (if the simulation is not in generation mode). */
  map<float, int> measures <- [];

  /**
   * Reflex to update the traffic light.
   */
  reflex dynamic when: is_traffic_signal {
    counter <- counter + 1;
    if (counter >= time_to_change) {
      counter <- 0;
      stop[0] <- empty(stop[0]) ? roads_in : [];
    }
  }

  /**
   * Increases the vehicle count by 1.
   */
  action increase_count {
    vehicle_count <- vehicle_count + 1;
  }

  /**
   * Adds the vehicle count to the file writer then resets the count to 0.
   */
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
      draw circle(3) color: #blue;
    }
    else {
      draw circle(3) color: #black;
    }
//    if (["RoadNode772", "RoadNode922", "RoadNode736", "RoadNode573"] contains name) {
//      write name + " " + gps_location;
//    }
  }
}
