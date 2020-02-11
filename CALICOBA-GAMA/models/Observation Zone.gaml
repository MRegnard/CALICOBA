model ObservationZone

import "Global.gaml"

species ObservationZone {
  /** Sensor's field of view. */
  geometry fov;
  /** List of all people in this observation zone's field of view. */
  list<Person> visible_people -> Person.population where (fov overlaps each);

  map<float, list<point>> desired_states <- [];

  reflex dump {
    ask world.file_writer {
      do add_people(myself.visible_people, myself.name, world.time);
    }
  }

  aspect default {
    draw fov color: rgb(255, 0, 0, 128);
  }
}
