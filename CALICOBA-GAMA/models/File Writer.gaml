model FileWriter

import "Person.gaml"
import "Observation Zone.gaml"

species FileWriter {
  string output_file;

  map<float, map> _data;

  action add_people(list<Person> people, string oz_name, float timestamp) {
    if (!(_data contains_key timestamp)) {
      _data[timestamp] <- [];
    }
    _data[timestamp] <+ oz_name :: (people collect (["label" :: each.name, "location" :: [each.location.x, each.location.y]]));
  }

  action dump {
    do _dump_json();
  }

  action _dump_json {
    string key <- "data";
    map<string, list> data <- [key :: []];

    loop entry over: _data.pairs {
      float timestamp <- entry.key;
      map value <- entry.value;
      list observation_zones <- [];

      loop entry2 over: value.pairs {
        observation_zones <+ ["label" :: entry2.key, "people" :: entry2.value];
      }
      data[key] <+ ["timestamp" :: timestamp, "observation_zones" :: observation_zones];
    }

    string file_name <- output_file + ".json";
    json_file f <- json_file(file_name, data);
    save f;
    write "Data saved to '" + file_name + "'";
  }
}
