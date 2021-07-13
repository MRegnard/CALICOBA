model IDM

global skills: [calicoba] {
  TargetModel target_model;
  bool do_calibration <- true;
  float step <- 0.1 °s;
  file road_shapefile <- file("../../includes/road.shp");
  graph road_graph;
  geometry shape <- envelope(road_shapefile);

  /** Global cars’ target point. */
  point target;

  init {
    do calicoba_init();
    create TargetModel number: 1 returns: tm;
    target_model <- tm[0];
    create RoadSection from: road_shapefile;
    point last_point <- last(RoadSection.population[0].shape.points);

    create RoadSection {
      self.shape <- line([{0.1, 400}, last_point]);
    }

    create RoadSection {
      self.shape <- line([last_point, {last_point.x + 200, last_point.y}]);
      self.is_counter <- true;
    }

    road_graph <- as_edge_graph(RoadSection.population);
    create Road {
      self.road_sections <- [RoadSection.population[0]];
      self.color <- #gray;
      self.generates_cars <- true;
    }

    create Road {
      self.road_sections <- [RoadSection.population[1]];
      self.color <- #darkgray;
      self.generates_cars <- true;
    }

    create Road {
      self.road_sections <- [RoadSection.population[2]];
      self.inbound_roads <- [Road.population[0], Road.population[1]];
      self.color <- #black;
    }

    loop road over: Road.population {
      loop section over: road.road_sections {
        section.road <- road;
      }
    }

    target <- last(last(last(Road.population).road_sections).shape.points);
    do calicoba_setup();
  }

  reflex step {
    if do_calibration {
      Road.population[0].car_generation_rate <- target_model.param_car_generation_rate_0;
      Road.population[1].car_generation_rate <- target_model.param_car_generation_rate_1;
      target_model.out_road2_cars_count <- float(RoadSection.population[2].car_nb);
      do calicoba_step();
    }
  }
}

species TargetModel skills: [calicoba_target_model] {
  float param_car_generation_rate_0 <- 5.0 min: 0.0;
  float param_car_generation_rate_1 <- 5.0 min: 0.0;
  float out_road2_cars_count min: 0.0;

  init {
    do model_init();
  }
}

/**
 * Car species using moving skill: implement IDM.
 */
species Car skills: [moving] {
  /** Default shape, length is an IDM parameter. */
  geometry default_shape <- rectangle(length, 1.5 °m);
  /** The car target. */
  point target;
  /** The next car if any. */
  Car next_car;

  // IDM params
  /** Car’s total length (m). */
  float length <- 4 °m;
  /** Car’s maximum speed (m/s). */
  float desired_speed <- 30 °m / °s;
  /** Distance to next car (m). */
  float spacing <- 1 °m;
  /** Driver’s reaction time (s). */
  float reaction_time <- 1.5 °s;
  /** Car’s maximum acceleration (m/s²). */
  float max_acceleration <- 4 °m / °s ^ 2;
  /** Car’s max acceleration (m/s²). */
  float desired_deceleration <- 3 °m / °s ^ 2;

  /** Car’s true acceleration (m/s²). */
  float acceleration <- 0 °m / °s ^ 2 min: -desired_deceleration max: max_acceleration;

  reflex move {
    if next_car = nil or dead(next_car) {
      // Compute acceleration
      acceleration <- max_acceleration * (1 - ((speed / desired_speed) ^ 4));
    } else {
      // Compute acceleration taking the next car into account
      float delta_speed <- next_car.speed - speed;
      float actual_gap <- (self distance_to next_car using topology(world.road_graph)) - length;
      float desired_minimum_gap <- spacing + (reaction_time * speed) - ((speed * delta_speed) / (2 * sqrt(max_acceleration * desired_deceleration)));
      acceleration <- max_acceleration * (1 - ((speed / desired_speed) ^ 4) - ((desired_minimum_gap / actual_gap) ^ 2));
    }

    speed <- speed + (acceleration * step);
    do goto on: world.road_graph target: target speed: speed;
    if location = target {
      do die();
    }
  }

  aspect default {
    shape <- default_shape rotated_by heading at_location location;
    draw shape color: #blue;
  }
}

/**
 * A road section is a portion of road geometry.
 */
species RoadSection {
  /** The road this section is a part of. */
  Road road;
  /** Whether this section should count vehicles. */
  bool is_counter <- false;
  int save_interval <- 1 °cycle;
  /** Number of cars currently present on this section. */
  int car_nb <- 0;

  string output_file_name <-  "../../output/IDM/" + name + ".csv" const: true;

  /**
   * This reflex initializes the output CSV file.
   * Only executed at cycle 0.
   */
  reflex post_init when: world.cycle = 0 {
    if is_counter {
      save "cycle,cars nb" to: output_file_name;
    }
  }

  /**
   * Counts and saves the number of cars on this road section during the last save_interval cycles.
   */
  reflex count when: is_counter and every(save_interval) {
    car_nb <- length(Car.population where (each.current_edge = self));
    save (string(cycle) + "," + string(car_nb)) to: output_file_name rewrite: false;
  }

  aspect default {
    rgb c <- road.color;
    if is_counter {
      c <- #red;
    }
    draw shape color: c;
  }
}

/**
 * A road is a sequence of road sections.
 */
species Road {
  /** Sequence of road sections. */
  list<RoadSection> road_sections;
  /** Total length of this road (sum of sections’ lengths). */
  float total_length;
  /** Point from which cars enter this road. */
  point entry_point;

  /** List of all roads that lead to this road. */
  list<Road> inbound_roads <- [];
  /** The car that last entered this road, if any. */
  Car last_entered_car <- nil;
  /** List of all alive cars that entered this road. */
  list<Car> last_entered_cars <- [];

  /** Whether this road should generate cars. */
  bool generates_cars <- false;
  /** Number of cars to generate every second (vehicles/s) */
  float car_generation_rate;
  /** The car that was generated last, if any. */
  Car last_generated_car <- nil;

  /** Road’s car density (vehicles/m). */
  float car_density -> length(get_cars()) / total_length;
  /** Road’s mean speed (m/s). */
  float mean_speed -> mean(get_cars() collect each.speed) °m / °s;

  /** Road’s color. */
  rgb color <- #grey;

  /**
   * This reflex initializes the total_length field as well as checking.
   * Only executed at cycle 0.
   */
  reflex post_init when: world.cycle = 0 {
    if not empty(inbound_roads) and generates_cars {
      error "Middle roads cannot generate cars: " + self;
    }

    total_length <- sum(road_sections collect each.shape.perimeter);
    entry_point <- road_sections[0].shape.points[0];
  }

  /**
   * This reflex generates new cars depending on the car_generation_rate parameter.
   */
  reflex generate_car when: generates_cars and every(1 / (car_generation_rate * world.step) °s) {
    // Created cars start from the begining of the road; the target is the same for all cars
    create Car {
      self.location <- myself.road_sections[0].shape.points[0];
      self.target <- world.target;
      self.next_car <- myself.last_generated_car;
      myself.last_generated_car <- self;
    }
  }

  /**
   * This reflex updates the next_car field of the first car on each inbound road.
   */
  reflex update_next_car when: not empty(inbound_roads) {
    // Get n last entered cars (n = number of inbound roads),
    // i.e. n closest cars to entry point that are not already in the last_entered_cars list,
    // sorted by increasing distance to this point.
    list<Car> last_cars <-
      closest_to(get_cars(), entry_point, length(inbound_roads))
      where (not (each in self.last_entered_cars))
      sort_by (each distance_to self.entry_point);

    if length(last_cars) != 0 {
      // More than 1 car entered this road at the same time,
      // reset their next_car according to their order.
      if length(last_cars) > 1 {
        loop i from: 0 to: length(last_cars) - 1 {
          Car car <- last_cars[i];
          if i < length(last_cars) - 1 {
            // Set next car of each newly entered cars.
            car.next_car <- last_cars[i + 1];
          } else {
            // First newly entered car has the last entered car of previous cycles as its next car.
            car.next_car <- last_entered_car;
          }
        }
      }

      // Set new last entered car.
      last_entered_car <- last_cars[0];
      // Insert newly entered cars into memory, remove dead cars.
      last_entered_cars <- last_entered_cars where (not dead(each)) union last_cars;
    }

    // Set next_car of first car on each inbound road.
    list<Car> next_inbound_cars <- inbound_roads
      collect (each.get_cars() closest_to self.entry_point)
      where (each != nil and each != self.last_entered_car and each.next_car != self.last_entered_car);
    ask next_inbound_cars {
      self.next_car <- myself.last_entered_car;
    }
  }

  /**
   * Returns the list of cars on this road.
   */
  list<Car> get_cars {
    return Car.population where (each.current_edge in self.road_sections);
  }

  aspect default {
    if generates_cars {
      draw circle(3 °m, road_sections[0].shape.points[0]) color: #green;
    }
  }
}

experiment "IDM using moving skill" {
  parameter "Calibrate?" var: do_calibration category: "Meta";

  output {
    display "IDM moving skill" type: java2D {
      species RoadSection;
      species Car;
      species Road;
    }

    display Charts {
      chart "Parameters" type: series size: {0.5, 0.5} position: {0, 0} {
        data "Road 0 car generation rate" value: target_model.param_car_generation_rate_0 color: #red marker: false;
        data "Road 1 car generation rate" value: target_model.param_car_generation_rate_1 color: #green marker: false;
      }

      chart "Parameters’ Actions" type: series size: {0.5, 0.5} position: {0.5, 0} {
        data "Road 0 car generation rate" value: target_model.get_parameter_action("param_car_generation_rate_0") color: #red marker: false;
        data "Road 1 car generation rate" value: target_model.get_parameter_action("param_car_generation_rate_1") color: #green marker: false;
      }

      chart "Outputs" type: series size: {0.5, 0.5} position: {0, 0.5} {
        data "Number of cars" value: target_model.out_road2_cars_count color: #darkgreen marker: false;
      }

      chart "Objectives’ Criticalities" type: series size: {0.5, 0.5} position: {0.5, 0.5} {
        data "out_preys_number" value: world.get_objective("out_road2_cars_count") color: #green marker: false;
      }
    }
  }
}
