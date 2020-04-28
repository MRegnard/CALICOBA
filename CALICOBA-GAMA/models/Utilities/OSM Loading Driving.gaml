/**
* Name: OSM Loading Driving
* Author: Patrick Taillandier
* Description: Model to show how to import OSM Files, using them to create agents for a road network, and saving the different agents in shapefiles. 
* The first goal of this model is to prepare data for the driving skill models.
* Tags:  load_file, gis, shapefile, save_file, osm
*/

model OSMdata_to_shapefile 

global {
  float DEFAULT_MAX_SPEED <- 50 °km / °h;
  int DEFAULT_LANES <- 1;

  string directory <- "UT3_directions";
  string osm_file_name <- "campus_test";
  /**
   * Map used to filter the objects to build from the OSM file according to attributes.
   * For an exhaself.ustive list, see: http://wiki.openstreetmap.org/wiki/Map_Features.
   */
  map<string, list<string>> filtering <- [
    "bus" :: ["yes", "designated"],
    "highway" :: ["primary", "secondary", "motorway", "trunk", "tertiary", "residential", "unclassified", "living_street", "road", "motorway_link", "motorway_junction", "trunk_link", "primary_link", "secondary_link", "tertiary_link", "service"],
    "building" :: ["appartments", "bungalow", "cabin", "detached", "dormitory", "farm", "hotel", "house", "houseboat", "residential", "semidetached_house", "commercial", "industrial", "kiosk", "office", "retail", "supermarket", "warehouse", "cathedral", "chapel", "church", "mosque", "religious", "shrine", "synagogue", "temple", "bakehouse", "civic", "government", "hospital", "kindergarten", "public", "school", "toilets", "train_station", "transportation", "university", "barn", "conservatory", "farm_auxiliary", "pavillon", "riding_hall", "sports_hall", "stadium", "hangar", "garage", "garages", "parking", "service", "yes"]
  ];

  //OSM file to load
  file<geometry> osmfile <- osm_file("../../includes/" + directory + "/" + osm_file_name + ".osm.pbf", filtering);

  geometry shape <- envelope(osmfile);
  graph the_graph; 
  map<point, Intersection> nodes_map;

  init {
    write "OSM file loaded: " + length(osmfile) + " geometries";

    // from the OSM file, creation of the selected agents
    loop geom over: osmfile {
      if (shape covers geom) {
        string highway_str <- string(geom get "highway");
        string bus_str <- string(geom get "bus");
        string building_str <- string(geom get "building");

        if (length(geom.points) = 1) {
          if (highway_str != nil) {
            string crossing <- string(geom get "crossing");

            create Intersection with: [
              shape :: geom,
              type :: highway_str,
              crossing :: crossing
            ] {
              nodes_map[location] <- self;
            }
          }
        }
        else if (highway_str != nil and !(highway_str = "service" and (bus_str = "yes" or bus_str = "designated"))) {
          string oneway <- string(geom get "oneway");
          float maxspeed_val <- float(geom get "maxspeed");
          string lanes_str <- string(geom get "lanes");
          int lanes_val <- empty(lanes_str) ? 1 : (length(lanes_str) > 1 ? int(first(lanes_str)) : int(lanes_str));

          create Road with: [
            shape :: geom,
            type :: highway_str,
            oneway :: oneway,
            maxspeed :: maxspeed_val,
            lanes :: lanes_val
          ] {
            if (self.lanes < 1) { // default value for the lanes attribute
              self.lanes <- DEFAULT_LANES;
            }
            if (self.maxspeed = 0) { // default value for the maxspeed attribute
              self.maxspeed <- DEFAULT_MAX_SPEED;
            }
          }
        }
        else if (building_str != nil) {
          create Building with: [
            type :: building_str
          ];
        }
      }
    }
    write "Road and node agents created";

    ask Road.population {
      point ptF <- first(shape.points);
      if (not (ptF in nodes_map.keys)) {
        create Intersection with: [location :: ptF] {
          nodes_map[location] <- self;
        }
      }
      point ptL <- last(shape.points);
      if (not (ptL in nodes_map.keys)) {
        create Intersection with: [location :: ptL] {
          nodes_map[location] <- self;
        }
      }
    }

    write "Supplementary node agents created";
    ask Intersection.population {
      if (empty(Road.population overlapping self)) {
        do die();
      }
    }

    write "node agents filtered";

    save Road type: shp to: "../../includes/" + directory + "/roads.shp" attributes: [
      "lanes" :: lanes,
      "maxspeed" :: maxspeed,
      "oneway" :: oneway
    ];
    save Intersection type: shp to: "../../includes/" + directory + "/nodes.shp" attributes: [
      "type" :: type,
      "crossing" :: crossing
    ];
    save Building type: shp to: "../../includes/" + directory + "/buildings.shp" attributes: [
      "type" :: type
    ];
    write "road and node shapefile saved";
  }
}

species Road {
  rgb color <- rgb(rnd(255), rnd(255), rnd(255));
  string type;
  string oneway;
  float maxspeed;
  int lanes;

  aspect base_ligne {
    draw shape color: color;
  }
}

species Intersection {
  string type;
  string crossing;

  aspect base {
    draw square(3) color: #red;
  }
}

species Building {
  string type;

  aspect base {
    draw shape color: rgb(200, 200, 200);
  }
}

experiment "From OSM to shapefiles" type: gui {
  output {
    display Map type: opengl {
      graphics World {
        draw world.shape.contour;
      }
      species Building aspect: base refresh: false;
      species Road aspect: base_ligne  refresh: false;
      species Intersection aspect: base refresh: false;
    }
  }
}
