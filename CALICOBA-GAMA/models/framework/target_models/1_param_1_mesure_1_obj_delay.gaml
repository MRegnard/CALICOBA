model TargetModel

species TargetModel skills: [calicoba_target_model] {
  float param_1 <- 10.0 min: 0.0 max: 100.0;
  float out_1 <- param_1 min: 0.0 max: 100.0;
  obj_def obj_1 <- obj_def(["out_1", 0.0, 25.0, 60.0]);
  map corr_matrix <- [
    "out_1" :: [
      "param_1" :: 1.0
    ]
  ];

  int delay <- 3;
  int remaining_steps <- delay;

  init {
    do model_init();
  }

  reflex update {
    if (remaining_steps = 0) {
      out_1 <- param_1;
      remaining_steps <- delay;
    } else {
      remaining_steps <- remaining_steps - 1;
    }
  }
}
