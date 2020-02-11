model Road

import "Person.gaml"

species Road {
  /** Capacity of the road considering its perimeter. */
  float capacity <- 1 + shape.perimeter / 30;
  /** Number of people on the road. */
  int nb_people <- 0 update: length(Person.population at_distance 1 #m);
  /** Speed coefficient computed using the number of people on the road and the capicity of the road. */
  float speed_coeff <- 1.0 update: exp(-nb_people / capacity) min: 0.1;
  /** Maximum authorized speed on this road section (float to avoid cast errors during calculations). */
  float max_speed;
  /** If true, this road is oneway only. */
  bool oneway;

  aspect default {
    draw shape color: #red;
//    string s <- "";
//    if (max_speed != 0) {
//      s <- s + string(max_speed);
//    }
//    if (oneway) {
//      s <- s + "-";
//    }
//    draw s color: #black;
  }
}
