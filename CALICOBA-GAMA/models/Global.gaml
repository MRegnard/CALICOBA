model Global

import "Person.gaml"
import "Building.gaml"
import "Road.gaml"
import "Observation Zone.gaml"
import "File Writer.gaml"

global {
  bool generate_data;

  string scenario_name <- "scenario";

  bool show_names <- false;

  int people_nb <- 1000 min: 0 max: 10000;

  string map_name <- "UT3_extended" among: ["city", "UT3", "UT3_extended", "UT3_test"];
  /** Shapefile of the buildings. */
  file _building_shapefile <- file("../includes/" + map_name + "/buildings.shp");
  /** Shapefile of the roads. */
  file _road_shapefile <- file("../includes/" + map_name + "/roads.shp");
  /** File containing observation zones data. */
  file _oz_file <- json_file("../includes/" + map_name + "/observation_zones.json");
  /** Shape of the environment. */
  geometry shape <- envelope(_road_shapefile);
  /** Graph of the road network. */
  graph road_network;
  /** Map containing all the weights for the road network graph. */
  map<Road, float> road_weights;

  FileWriter file_writer;

  float step <- 10 #s;

  init {
    create Building from: _building_shapefile;
    create Road from: _road_shapefile with: [
      max_speed :: float(read("max_speed")),
      oneway :: bool(read("oneway"))
    ];
    create Person number: people_nb with: [
      location :: any_location_in(one_of(Building.population))
    ];

    road_weights <- Road.population as_map (each :: each.shape.perimeter);
    road_network <- as_edge_graph(Road.population);

    loop observation_zone over: map<string, list<map>>(_oz_file.contents)["observation_zones"] {
      write observation_zone["location"];
      list<float> p <- observation_zone["location"];
      point position <- convert_point(p);
      write position;

      list<list<float>> vs <- observation_zone["verteces"];
      list<point> verteces <- vs collect convert_point(each);

      create ObservationZone number: 1 with: [
        name :: string(observation_zone["label"]),
        location :: position,
        fov :: polygon([position] + verteces)
      ];
    }

    if (!generate_data) {
      do init_observation_zones();
    }

    create FileWriter number: 1 returns: fw with: [
      output_file :: "../output/" + map_name + "/" + scenario_name
    ];
    file_writer <- first(fw);
  }

  /**
   * Converts a point in WGS84 CRS into GAMA’s CRS.
   * 
   * @param p the WSG84 point to convert
   */
  point convert_point(list<float> p) {
    return point(to_GAMA_CRS({p[1], p[0]}, "WGS84"));
  }

  /**
   * Updates the speed of the roads according to the weights.
   */
  reflex update_road_speed {
    road_weights <- Road.population as_map (each :: each.shape.perimeter / each.speed_coeff);
    road_network <- road_network with_weights road_weights;
  }

  /**
   * Writes the measured data to a file.
   */
  reflex dump_data {
    ask file_writer {
      do dump();
    }
  }

  /**
   * Returns the building that contains the given coordinates.
   * 
   * @param p the coordinate
   * @returns the building this coordinate is in
   */
  Building building_at(point p) {
    return first(Building.population where (each overlaps p));
  }

  action init_observation_zones {
    json_file f <- json_file("../includes/" + map_name + "/" + scenario_name + ".json");
    list<map> contents <- f.contents["data"];

    ask ObservationZone.population {
      loop entry over: contents {
        float timestamp <- float(entry["timestamp"]);

        loop oz over: list<map>(entry["observation_zones"]) {
          if (oz["label"] = self.name) {
            self.desired_states <+ timestamp :: list<point>(list<map>(oz["people"]) collect each["location"]);
          }
        }

      }
      write self.desired_states;
    }
  }

  action show_mouse_location {
    write "Mouse location: " + string(#user_location);
  }
}























