model Main

import "reference_models/Lotka_Volterra.gaml"
import "target_models/Lotka_Volterra.gaml"

global {
  ReferenceSystem reference_system;
  TargetModel target_model;

  init {
    create ReferenceSystem number: 1 returns: ref;
    reference_system <- first(ref);
    create TargetModel number: 1 returns: target;
    target_model <- first(target);
    calicoba_setup;
  }

  reflex step {
    calicoba_step;
  }
}

experiment "Lotka Volterra" type: gui {
  output {
    display "Time Series" {
      chart "Lotka Volterra Time Series (Reference)" type: series background: #white size: {1, 0.5} position: {0, 0} {
        data "Number of preys (Reference)" value: reference_system.preys_number color: #green;
        data "Number of predators (Reference)" value: reference_system.predators_number color: #red;
        data "Number of preys (Target)" value: target_model.preys_number color: #blue;
        data "Number of predators (Target)" value: target_model.predators_number color: #black;
      }
      chart "Parameters" type: series background: #white size: {1, 0.5} position: {0, 0.5} {
        data "Preys birth rate" value: target_model.param_preys_birth_rate color: #blue;
        data "Preys birth rate" value: target_model.get_parameter_action("param_preys_birth_rate") color: #blue;
      }
    }
  }
}
