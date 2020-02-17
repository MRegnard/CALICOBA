model Main

import "Road.gaml"
import "RoadNode.gaml"
import "Driver.gaml"

global {
  file shape_file_roads <- file("../../includes/UT3_directions/roads.shp");
  file shape_file_nodes <- file("../../includes/UT3_directions/nodes.shp");
  geometry shape <- envelope(shape_file_roads);
  graph road_network;
  int nb_people <- 1000;

  init {
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

    // 2 niveaux de paramètres : micro/macro
    create Driver number: nb_people {
      self.speed <- 30 °km / °h;
      self.vehicle_length <- 3 °m;
      self.right_side_driving <- true;
      self.proba_lane_change_up <- rnd(1.0);
      self.proba_lane_change_down <- rnd(0.5, 1.0);
      self.location <- one_of(RoadNode.population where empty(each.stop)).location;
      self.security_distance_coeff <- rnd(1.0, 3.0);
      self.proba_respect_priorities <- rnd(0.8, 1.0);
      self.proba_respect_stops <- [rnd(0.998, 1.0)];
      self.proba_block_node <- rnd(0.0, 0.003);
      self.proba_use_linked_road <- 0.0;
      self.max_acceleration <- rnd(0.5, 1.0);
      self.speed_coeff <- rnd(0.8, 1.2);
    }
  }
}
