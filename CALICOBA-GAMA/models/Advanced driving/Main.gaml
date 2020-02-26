model Main

import "Road.gaml"
import "RoadNode.gaml"
import "Vehicle.gaml"
import "Building.gaml"

global {
  file shape_file_buildings <- file("../../includes/UT3_directions/buildings.shp");
  file shape_file_roads <- file("../../includes/UT3_directions/roads.shp");
  file shape_file_nodes <- file("../../includes/UT3_directions/nodes.shp");
  geometry shape <- envelope(shape_file_roads);
  graph road_network;

  bool generate_mode;
  int vehicles_nb <- 1000 min: 1 max: 10000;

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

  float security_distance_coeff_lower <- 1.0;
  float security_distance_coeff_upper <- 3.0;
  float max_acceleration_lower <- 0.5;
  float max_acceleration_upper <- 1.0;
  float speed_coeff_lower <- 0.8;
  float speed_coeff_upper <- 1.2;

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
  }
}
