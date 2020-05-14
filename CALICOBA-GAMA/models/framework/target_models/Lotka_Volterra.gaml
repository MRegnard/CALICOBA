model Lotka_Volterra_Target

species TargetModel skills: [calicoba_target_model] {
  float param_preys_birth_rate <- 0.04;
  float predation_rate <- 0.001;
  float predators_death_rate <- 0.03;
  float predation_efficiency <- 0.0002;

  float init_preys_number <- 250.0;
  float init_predators_number <- 45.0;

  float preys_number <- init_preys_number;
  float predators_number <- init_predators_number;

  float out_preys_number <- preys_number;
  float out_predators_number -> predators_number;

  /** Integration time step used in the Runge-Kutta 4 method. */
  float integration_time_step <- 0.01;
  float t;

  init {
    do model_init();
  }

  equation lotka_volterra {
    // Evolution of the number of preys duting an integration time step
    diff(preys_number, t) = preys_number * (param_preys_birth_rate - predation_rate * predators_number);
    // Evolution of the number of predator during an integration time step
    diff(predators_number, t) = -predators_number * (predators_death_rate - predation_efficiency * preys_number);
  }

  reflex solving {
    // Using Runge-Kutta 4 method.
    solve lotka_volterra method: "rk4" step_size: integration_time_step;
    out_preys_number <- preys_number;
  }
}
