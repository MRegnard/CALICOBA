model Main

import "Road.gaml"
import "RoadNode.gaml"
import "Vehicle.gaml"
import "Building.gaml"
import "File Writer.gaml"

global {
  string map_name <- "UT3_directions" among: ["UT3_directions"];
  string scenario_name <- "test";

  file shape_file_buildings <- file("../../includes/" + map_name + "/buildings.shp");
  file shape_file_roads <- file("../../includes/" + map_name + "/roads.shp");
  file shape_file_nodes <- file("../../includes/" + map_name + "/nodes.shp");
  geometry shape <- envelope(shape_file_roads);
  graph road_network;

  bool initialized <- false;

  bool generate_mode;
  int vehicles_nb <- 1000 min: 1 max: 10000;
  int save_interval <- 10 min: 1;

  float proba_lane_change_up_lower <- 0.0 min: 0.0 max: 1.0;
  float proba_lane_change_up_upper <- 1.0 min: 0.0 max: 1.0;
  float proba_lane_change_down_lower <- 0.5 min: 0.0 max: 1.0;
  float proba_lane_change_down_upper <- 1.0 min: 0.0 max: 1.0;
  float proba_respect_priorities_lower <- 0.8 min: 0.0 max: 1.0;
  float proba_respect_priorities_upper <- 1.0 min: 0.0 max: 1.0;
  float proba_respect_stops_lower <- 0.998 min: 0.0 max: 1.0;
  float proba_respect_stops_upper <- 1.0 min: 0.0 max: 1.0;
  float proba_block_node_lower <- 0.0 min: 0.0 max: 1.0;
  float proba_block_node_upper <- 0.003 min: 0.0 max: 1.0;
  float proba_use_linked_road_lower <- 0.0 min: 0.0 max: 1.0;
  float proba_use_linked_road_upper <- 0.0 min: 0.0 max: 1.0;

  float security_distance_coeff_lower <- 1.0 min: 0.0;
  float security_distance_coeff_upper <- 3.0 min: 0.0;
  float max_acceleration_lower <- 0.5 min: 0.0;
  float max_acceleration_upper <- 1.0 min: 0.0;
  float speed_coeff_lower <- 0.8 min: 0.0;
  float speed_coeff_upper <- 1.2 min: 0.0;

  FileWriter file_writer;

  init {
    create Building from: shape_file_buildings;
    create RoadNode from: shape_file_nodes with: [is_traffic_signal :: (string(read("type")) = "traffic_signals")];

    ask RoadNode.population where each.is_traffic_signal {
      self.stop << flip(0.5) ? self.roads_in : [];
    }

    create Road from: shape_file_roads with: [
      lanes :: int(read("lanes")),
      maxspeed :: float(read("maxspeed")) °km / °h,
      oneway :: string(read("oneway"))
    ] {
      self.geom_display <- self.shape + 2.5 * self.lanes;
      switch oneway {
        match "no" {
          create Road {
            self.lanes <- myself.lanes;
            self.shape <- polyline(reverse(myself.shape.points));
            self.maxspeed <- myself.maxspeed;
            self.geom_display <- myself.geom_display;
            self.linked_road <- myself;
            myself.linked_road <- self;
          }
        }
        match "-1" {
          self.shape <- polyline(reverse(self.shape.points));
        }
      }
    }

    map general_speed_map <- Road.population as_map (each :: (each.shape.perimeter / each.maxspeed));
    road_network <- as_driving_graph(Road.population, RoadNode.population) with_weights general_speed_map;

    create Vehicle number: vehicles_nb {
      self.location <- one_of(RoadNode.population where empty(each.stop)).location;
      self.right_side_driving <- true;

      self.vehicle_length <- 3 °m;
      self.speed <- 30 °km / °h;

      self.proba_lane_change_up <- rnd(myself.proba_lane_change_up_lower, myself.proba_lane_change_up_upper);
      self.proba_lane_change_down <- rnd(myself.proba_lane_change_down_lower, myself.proba_lane_change_down_upper);
      self.proba_respect_priorities <- rnd(myself.proba_respect_priorities_lower, myself.proba_respect_priorities_upper);
      self.proba_respect_stops <- [rnd(myself.proba_respect_stops_lower, myself.proba_respect_stops_upper)];
      self.proba_block_node <- rnd(myself.proba_block_node_lower, myself.proba_block_node_upper);
      self.proba_use_linked_road <- rnd(myself.proba_use_linked_road_lower, myself.proba_use_linked_road_upper);

      self.security_distance_coeff <- rnd(myself.security_distance_coeff_lower, myself.security_distance_coeff_upper);
      self.max_acceleration <- rnd(myself.max_acceleration_lower, myself.max_acceleration_upper) °m / (°s * °s);
      self.speed_coeff <- rnd(myself.speed_coeff_lower, myself.speed_coeff_upper);
    }

    create FileWriter number: 1 returns: fw with: [
      output_directory :: "../../output/" + map_name + "/",
      save_interval :: save_interval
    ];
    file_writer <- first(fw);

    write "Extracting counters…";
    file counters_file <- json_file("../../includes/" + map_name + "/counters.json");

    loop counter_location over: list<list<float>>(counters_file.contents["nodes"]) {
      point p <- point(to_GAMA_CRS(point(counter_location), "WGS84"));
      RoadNode n <- RoadNode(p);

      n.is_traffic_counter <- true;
      write "\t" + n.name + " set as counter.";
    }
    write "Done.";
  }

  /**
   * Reflex needed to use the correct value of the generate_mode variable, set in experiments.
   * It is executed only once, right after the init function.
   */
  reflex post_init when: not initialized {
    file_writer.output_file <- scenario_name + (generate_mode ? "" : "_simulated");

    if (not generate_mode) {
      write "Extracting node data…";
      file data_file <- json_file("../../includes/" + map_name + "/" + scenario_name + ".json");

      loop entry over: list<map<string, unknown>>(data_file.contents["data"]) {
        float timestamp <- float(entry["timestamp"]);

        loop node over: list<map<string, unknown>>(entry["nodes"]) {
          int people_nb <- int(node["people_nb"]);
          point p <- point(to_GAMA_CRS(point(node["location"]), "WGS84"));
          RoadNode n <- RoadNode(p);

          if (n.is_traffic_counter) {
            n.measures <+ timestamp :: people_nb;
          }
        }
      }
      write "Done.";
    }

    initialized <- true;
  }
}

// Schedule cannot be set on global anymore.
// World is always executed first.
species Scheduler schedules: Vehicle + RoadNode + FileWriter {}
