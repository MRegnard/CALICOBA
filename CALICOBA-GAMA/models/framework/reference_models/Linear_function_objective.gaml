model LinearFunctionObjective

species ReferenceSystem skills: [calicoba_reference_system] {
  float out_x <- 0.0;
  float out_y <- 0.0;

  init {
    do system_init();
  }
}
