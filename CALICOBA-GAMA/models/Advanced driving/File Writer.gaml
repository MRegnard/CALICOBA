model FileWriter

import "Vehicle.gaml"

species FileWriter {
  string output_directory;
  string output_file;

  map<float, map> _data;

  init {
    if (!folder_exists(output_directory)) {
      file _ <- new_folder(output_directory);
    }
  }

  action add_people(list<Vehicle> drivers, string node_name, float timestamp) {
    if (!(_data contains_key timestamp)) {
      _data[timestamp] <- [];
    }
    _data[timestamp] <+ node_name :: length(drivers);
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

    string file_name <- output_directory + output_file + ".json";
    json_file f <- json_file(file_name, data);
    save f;
    write "Data saved to '" + file_name + "'";
  }
}
