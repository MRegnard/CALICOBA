model FileWriter

import "Vehicle.gaml"

species FileWriter schedules: [] {
  string output_directory;
  string output_file;

  float save_interval <- 10 °s;
  float _last_dump <- 0.0 °s;
  float _next_dump -> { _last_dump + save_interval };

  map<float, map<point, int>> _data;

  init {
    if (!folder_exists(output_directory)) {
      file _ <- new_folder(output_directory);
    }
  }

  action add_people(int vehicle_count, point node_location) {
    if (!(_data contains_key _next_dump)) {
      _data[_next_dump] <- [];
    }
    if (!(_data[_next_dump] contains_key node_location)) {
      _data[_next_dump][node_location] <- 0;
    }
    _data[_next_dump][node_location] <- _data[_next_dump][node_location] + vehicle_count;
  }

  reflex dump when: world.time >= _next_dump {
    do _dump_json();
    _last_dump <- world.time;
  }

  action _dump_json {
    string key <- "data";
    map<string, list> data <- [key :: []];

    loop entry over: _data.pairs {
      float timestamp <- entry.key;
      map value <- entry.value;
      list nodes <- [];

      loop entry2 over: value.pairs {
        nodes <+ ["location" :: string(entry2.key), "people_nb" :: entry2.value];
      }
      data[key] <+ ["timestamp" :: timestamp, "nodes" :: nodes];
    }

    string file_name <- output_directory + output_file + ".json";
    json_file f <- json_file(file_name, data);
    save f;
    write "Data saved to '" + file_name + "'";
  }
}
