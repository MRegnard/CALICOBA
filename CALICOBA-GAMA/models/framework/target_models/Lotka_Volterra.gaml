model Lotka_Volterra_Target

species TargetModel skills: [calicoba_target_model] {
  float param_preys_birth_rate <- 0.04;
  float predation_rate <- 0.001;
  float predators_death_rate <- 0.03;
  float predation_efficiency <- 0.0002;

  float init_preys_number <- 200.0;
  float init_predators_number <- 40.0;

  float out_preys_number <- init_preys_number;
  float out_predators_number <- init_predators_number;

  /** Integration time step used in the Runge-Kutta 4 method. */
  float integration_time_step <- 0.01;
  float t;

  init {
    do model_init();
  }

  equation lotka_volterra {
    // Evolution of the number of preys duting an integration time step
    diff(out_preys_number, t) = out_preys_number * (param_preys_birth_rate - predation_rate * out_predators_number);
    // Evolution of the number of predator during an integration time step
    diff(out_predators_number, t) = -out_predators_number * (predators_death_rate - predation_efficiency * out_preys_number);
  }

  reflex solving {
    // Using Runge-Kutta 4 method.
    solve lotka_volterra method: "rk4" step_size: integration_time_step;
  }

  bool shouldReset {
  	return out_preys_number <= 0 or out_predators_number <= 0;
  }

  action reset {
  	t <- 0.0;
    out_preys_number <- init_preys_number;
    out_predators_number <- init_predators_number;
  }
}
