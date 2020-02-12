model Main

import "Road.gaml"
import "RoadNode.gaml"
import "Driver.gaml"

global {
  file shape_file_roads <- file("../includes/Rouen/roads.shp");
  file shape_file_nodes <- file("../includes/Rouen/nodes.shp");
  geometry shape <- envelope(shape_file_roads);
  graph road_network;
  int nb_people <- 1000;

  init {
    create RoadNode from: shape_file_nodes with: [is_traffic_signal :: (string(read("type")) = "traffic_signals")];
    ask RoadNode.population where each.is_traffic_signal {
      stop << flip(0.5) ? roads_in : [];
    }

    create Road from: shape_file_roads with: [
      lanes :: int(read("lanes")),
      maxspeed :: float(read("maxspeed")) #km / #h,
      oneway :: string(read("oneway"))
    ] {
      self.geom_display <- self.shape + (2.5 * self.lanes);
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

    create Driver number: nb_people {
      speed <- 30 #km / #h;
      vehicle_length <- 3.0 #m;
      right_side_driving <- true;
      proba_lane_change_up <- rnd(1.0);
      proba_lane_change_down <- rnd(0.5, 1.0);
      location <- one_of(RoadNode.population where empty(each.stop)).location;
      security_distance_coeff <- rnd(1.0, 3.0);
      proba_respect_priorities <- rnd(0.8, 1.0);
      proba_respect_stops <- [rnd(0.998, 1.0)];
      proba_block_node <- rnd(0.0, 0.003);
      proba_use_linked_road <- 0.0;
      max_acceleration <- rnd(0.5, 1.0);
      speed_coeff <- rnd(0.8, 1.2);
    }
  }
}
