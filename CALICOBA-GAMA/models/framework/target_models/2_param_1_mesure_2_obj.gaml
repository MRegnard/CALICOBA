model TargetModel

species TargetModel skills: [calicoba_target_model] {
  float param_1 <- 50.0 min: 0.0 max: 100.0;
  float param_2 <- 50.0 min: 0.0 max: 100.0;
  float out_1 <- (param_1 + param_2) / 2 min: 0.0 max: 100.0;
  obj_def obj_1 <- obj_def(["out_1", 0.0, 45.0, 60.0]);
  obj_def obj_2 <- obj_def(["param_1", 0.0, 35.0, 60.0]);
  map corr_matrix <- [
    "out_1" :: [
      "param_1" :: 1.0,
      "param_2" :: 1.0
    ]
  ];

  init {
    do model_init();
  }

  reflex update {
    out_1 <- (param_1 + param_2) / 2;
  }
}
