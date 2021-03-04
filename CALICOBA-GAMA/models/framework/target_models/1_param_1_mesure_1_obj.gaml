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

  init {
    do model_init();
  }

  reflex update {
    out_1 <- param_1;
  }
}
