model TargetModel

species TargetModel skills: [calicoba_target_model] {
  float param_1 <- 50.0 min: 0.0 max: 100.0;
  obj_def obj_1 <- obj_def(["param_1", 0.0, 25.0, 60.0]);
  map corr_matrix <- [];

  init {
    do model_init();
  }
}
