model Main

import "reference_models/Lotka_Volterra.gaml"
import "target_models/Lotka_Volterra.gaml"

global skills: [calicoba] {
  ReferenceSystem reference_system;
  TargetModel target_model;

  init {
    do calicoba_init(step_interval: 5);

    create ReferenceSystem number: 1 returns: ref;
    reference_system <- first(ref);
    create TargetModel number: 1 returns: target;
    target_model <- first(target);

    do calicoba_setup();
  }

  reflex step {
    do calicoba_step();
  }
}

experiment "Lotka Volterra" type: gui {
  output {
    display "Time Series" {
      chart "Lotka Volterra Time Series" type: series size: {0.5, 0.5} position: {0, 0} {
        data "Number of preys (Reference)" value: reference_system.out_preys_number color: #green marker: false;
        data "Number of predators (Reference)" value: reference_system.out_predators_number color: #red marker: false;
        data "Number of preys (Target)" value: target_model.out_preys_number color: #darkgreen marker: false;
        data "Number of predators (Target)" value: target_model.out_predators_number color: #darkred marker: false;
      }

      chart "Parameters" type: series size: {0.5, 0.5} position: {0, 0.5} {
        data "Preys birth rate (α)" value: target_model.param_preys_birth_rate color: #black marker: false;
      }

      chart "Parameters’ Actions" type: series size: {0.5, 0.5} position: {0.5, 0.5} {
        data "Preys birth rate (α)" value: target_model.get_parameter_action("param_preys_birth_rate") color: #black marker: false;
      }

      chart "Objectives’ Criticalities" type: series size: {0.5, 0.5} position: {0.5, 0} {
        data "out_preys_number" value: world.get_objective("out_preys_number") color: #green marker: false;
        data "out_predators_number" value: world.get_objective("out_predators_number") color: #red marker: false;
      }
    }
  }
}
