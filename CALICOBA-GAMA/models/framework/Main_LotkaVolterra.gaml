model Main

import "target_models/Lotka_Volterra.gaml"

global skills: [calicoba] {
  TargetModel target_model;

  bool do_calibration <- true;

  init {
    do calicoba_init();

    create TargetModel number: 1 returns: target;
    target_model <- first(target);

    do calicoba_setup();
  }

  reflex step {
    if (do_calibration) {
      do calicoba_step();
    }
  }
}

experiment Experiment type: gui {
  parameter "Do calibration?" var: do_calibration;

  output {
    display "Time Series" {
//      chart "Lotka Volterra Phase Portrait" type: xy size: {0.5, 0.5} position: {0, 0} {
//        data 'Equilibrium point' value: {target_model.param_predators_death_rate / target_model.param_predation_efficiency, target_model.param_preys_birth_rate / target_model.param_predation_rate} color: #blue;
//        data 'Number of preys according to number of predators' value: {target_model.out_preys_number, target_model.out_predators_number} color: #black marker: false;
//      }

      chart "Lotka Volterra Time Series" type: series size: {0.5, 0.5} position: {0, 0} {
        data "Number of preys" value: target_model.out_preys_number color: #darkgreen marker: false;
        data "Number of predators" value: target_model.out_predators_number color: #darkred marker: false;
      }

      chart "Parameters" type: series size: {0.5, 0.5} position: {0, 0.5} {
        data "Preys birth rate (α)" value: target_model.param_preys_birth_rate color: #red marker: false;
        data "Predation rate (β)" value: target_model.param_predation_rate color: #green marker: false;
        data "Predation efficiency (γ)" value: target_model.param_predation_efficiency color: #black marker: false;
        data "Predators death rate (δ)" value: target_model.param_predators_death_rate color: #blue marker: false;
      }

      chart "Parameters’ Actions" type: series size: {0.5, 0.5} position: {0.5, 0.5} {
        data "Preys birth rate (α)" value: target_model.get_parameter_action("param_preys_birth_rate") color: #red marker: false;
        data "Predation rate (β)" value: target_model.get_parameter_action("param_predation_rate") color: #green marker: false;
        data "Predation efficiency (γ)" value: target_model.get_parameter_action("param_predation_efficiency") color: #black marker: false;
        data "Predators death rate (δ)" value: target_model.get_parameter_action("param_predators_death_rate") color: #blue marker: false;
      }

      chart "Objectives’ Criticalities" type: series size: {0.5, 0.5} position: {0.5, 0} {
        data "out_preys_number" value: world.get_objective("obj_preys_number") color: #green marker: false;
        data "out_predators_number" value: world.get_objective("obj_predators_number") color: #red marker: false;
      }
    }
  }
}
