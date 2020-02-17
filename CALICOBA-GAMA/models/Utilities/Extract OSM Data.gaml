/**
 * Name: OSM file to Agents
 * Author:  Patrick Taillandier
 * Description: Model which shows how to import a OSM File in GAMA and use it to create Agents. In this model, a filter is done to take only into account the roads 
 * and the buildings contained in the file. 
 * Tags:  load_file, osm, gis
 */
model ExtractOSMData

global {
  float DEFAULT_MAX_SPEED <- 50 °km / °h;
  int DEFAULT_LANES <- 1;

  string directory <- "UT3_directions";
  string osm_file_name <- "campus_test";
  /**
   * Map used to filter the objects to build from the OSM file according to attributes.
   * For an exhaustive list, see: http://wiki.openstreetmap.org/wiki/Map_Features.
   */
  map<string, list<string>> filtering <- map([
    "bus" :: ["yes", "designated"],
    "highway" :: ["primary", "secondary", "motorway", "trunk", "tertiary", "residential", "unclassified", "living_street", "road", "motorway_link", "motorway_junction", "trunk_link", "primary_link", "secondary_link", "tertiary_link", "service"],
    "building" :: ["appartments", "bungalow", "cabin", "detached", "dormitory", "farm", "hotel", "house", "houseboat", "residential", "semidetached_house", "commercial", "industrial", "kiosk", "office", "retail", "supermarket", "warehouse", "cathedral", "chapel", "church", "mosque", "religious", "shrine", "synagogue", "temple", "bakehouse", "civic", "government", "hospital", "kindergarten", "public", "school", "toilets", "train_station", "transportation", "university", "barn", "conservatory", "farm_auxiliary", "pavillon", "riding_hall", "sports_hall", "stadium", "hangar", "garage", "garages", "parking", "service", "yes"]
  ]);
  /** OSM file to load. */
  osm_file osmfile <- file<geometry>(osm_file("../../includes/" + directory + "/" + osm_file_name + ".osm.pbf", filtering));

  geometry shape <- envelope(osmfile);

  init {
    create OSMAgent from: osmfile with: [
      highway_str :: string(read("highway")),
      bus_str :: string(read("bus")),
      building_str :: string(read("building")),
      max_speed_str :: string(read("maxspeed")),
      lanes_str :: string(read("lanes")),
      oneway_str :: string(read("oneway")),
      junction_str :: string(read("junction"))
    ];

    ask OSMAgent.population {
      if (length(self.shape.points) > 1) {
        // Create road.
        if (self.highway_str != nil and !(self.highway_str = "service" and (self.bus_str = "yes" or self.bus_str = "designated"))) {
          float max_speed <- float(self.max_speed_str) °km / °h;
          int lanes <- int(self.lanes_str);

          create Road with: [
            shape :: shape,
            max_speed :: max_speed = 0 ? DEFAULT_MAX_SPEED : max_speed,
            lanes :: lanes = 0 ? DEFAULT_LANES : lanes,
            oneway :: oneway_str
          ];
        } // Create building.
        else if (self.building_str != nil) {
          create Building with: [
            shape :: shape
          ];
        }
      } // Create road node.
      else if (length(shape.points) = 1) {
//        if (highway_str = "traffic_signals") {
          create RoadNode with: [
            is_traffic_signal :: traffic_signal_str != nil 
          ];
//        }
      }

      do die();
    }
  }

  reflex export {
    graph<geometry, Road> road_graph <- as_edge_graph(Road.population);
    // Sort by biggest component first.
    list<list<Road>> components <- list<list<Road>>(connected_components_of(road_graph, true) sort_by -length(each));

    write components collect length(each);
    // Keep only the biggest component.
    loop i from: 1 to: length(components) - 1 {
      write components[i];
      ask components[i] {
        do die();
      }
    }

    save Road.population to: "../../includes/" + directory + "/roads.shp" type: shp with: [
      max_speed :: "maxspeed",
      oneway :: "oneway",
      lanes :: "lanes"
    ];
    save RoadNode.population to: "../../includes/" + directory + "/nodes.shp" type: shp with: [
      is_traffic_signal :: "is_traffic_signal"
    ];
    save Building.population to: "../../includes/" + directory + "/buildings.shp" type: shp;
  }
}

/**
 * Generic OSM agent used to extract useful data.
 */
species OSMAgent {
  string highway_str;
  string building_str;
  string bus_str;
  string max_speed_str;
  string lanes_str;
  string oneway_str;
  string junction_str;
  string traffic_signal_str;
}

species Road {
  rgb color <- rnd_color(255);
  float max_speed;
  int lanes;
  string oneway;

  aspect default {
    draw shape color: color;
    if (max_speed != 0) {
      draw string(round(max_speed °h / °km)) color: #black;
    }
  }
}

species RoadNode {
  bool is_traffic_signal;
}

species Building {
  aspect default {
    draw shape color: rgb(200, 200, 200);
  }
}

experiment "Load OSM" type: gui {
  output {
    display Map type: opengl {
      species Building refresh: false;
      species Road refresh: false;
    }
  }
}
