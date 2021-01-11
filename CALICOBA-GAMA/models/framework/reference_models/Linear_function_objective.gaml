model LinearFunctionObjective

species ReferenceSystem skills: [calicoba_reference_system] {
  float out_1 <- 0.0;
  float out_2 <- 0.0;

  init {
    do system_init();
  }
}
