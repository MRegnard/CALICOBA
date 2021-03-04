model Lotka_Volterra_Target

species TargetModel skills: [calicoba_target_model] {
  float param_preys_birth_rate <- 2/3.0 min: 0.0 max: 1.0; // α
  float param_predation_rate <- 1.0 min: 0.0 max: 1.0; // β
  float param_predation_efficiency <- 1.0 min: 0.0 max: 1.0; // γ
  float param_predators_death_rate <- 1.0 min: 0.0 max: 1.0; // δ
//  float param_preys_birth_rate <- 0.05 min: 0.0 max: 1.0; // α
//  float param_predation_rate <- 0.001 min: 0.0 max: 1.0; // β
//  float param_predation_efficiency <- 0.0002 min: 0.0 max: 1.0; // γ
//  float param_predators_death_rate <- 0.03 min: 0.0 max: 1.0; // δ
//  float param_preys_birth_rate <- 1.0 min: 0.0 max: 1.0; // α
//  float param_predation_rate <- 0.025 min: 0.0 max: 1.0; // β
//  float param_predation_efficiency <- 1.0 min: 0.0 max: 1.0; // γ
//  float param_predators_death_rate <- 1.0 min: 0.0 max: 1.0; // δ

  float init_preys_number <- 1.0;
  float init_predators_number <- 1.0;
//  float init_preys_number <- 250.0;
//  float init_predators_number <- 45.0;

  float out_preys_number <- init_preys_number min: 0.0;
  float out_predators_number <- init_predators_number min: 0.0;
  float out_preys_var <- 0.0;
  float out_predators_var <- 0.0;

  obj_def obj_preys_number <- obj_def(["out_preys_number", 0.0, 20.0, 40.0]);
  obj_def obj_predators_number <- obj_def(["out_predators_number", 0.0, 10.0, 20.0]);
//  obj_def obj_preys_number <- obj_def(["out_preys_number", 0.0, 200.0, 1000.0]);
//  obj_def obj_predators_number <- obj_def(["out_predators_number", 0.0, 30.0, 600.0]);

  map corr_matrix <- [
    "out_preys_var" :: [
      "param_preys_birth_rate" :: 1,
      "param_predation_rate" :: -1
    ],
    "out_predators_var" :: [
      "param_predation_efficiency" :: 1,
      "param_predators_death_rate" :: -1
    ],
    "out_preys_number" :: [
      "out_preys_var" :: 1,
      "out_predators_var" :: -1
    ],
    "out_predators_number" :: [
      "out_preys_var" :: 1,
      "out_predators_var" :: 1
    ]
  ];

  float integration_time_step <- 0.01;
  float t;

  init {
    do model_init();
  }

  equation lotka_volterra {
    // Evolution of the number of preys duting an integration time step
    diff(out_preys_number, t) = out_preys_number * (param_preys_birth_rate - param_predation_rate * out_predators_number);
    // Evolution of the number of predator during an integration time step
    diff(out_predators_number, t) = out_predators_number * (param_predation_efficiency * out_preys_number - param_predators_death_rate);
  }

  reflex solving {
    float old_preys_number <- out_preys_number;
    float old_predators_number <- out_predators_number;
    // Using Runge-Kutta 4 method.
    solve lotka_volterra method: "rk4" step_size: integration_time_step;
    out_preys_var <- out_preys_number - old_preys_number;
    out_predators_var <- out_predators_number - old_predators_number;
  }
}
