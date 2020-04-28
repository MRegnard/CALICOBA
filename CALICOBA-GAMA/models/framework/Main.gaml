model Main

import "reference_models/Lotka_Volterra.gaml"
import "target_models/Lotka_Volterra.gaml"

global {
  ReferenceModel reference_model;
  TargetModel target_model;

  init {
    create ReferenceModel number: 1 returns: ref;
    reference_model <- first(ref);
    create TargetModel number: 1 returns: target;
    target_model <- first(target);
  }
}

experiment "Lotka Volterra" type: gui {
  output {
    display "Time Series" {
      chart "Lotka Volterra Time Series (Reference)" type: series background: #white size: {1, 0.5} position: {0, 0} {
        data "Number of preys" value: reference_model.preys_number color: #green;
        data "Number of predators" value: reference_model.predators_number color: #red;
      }
      chart "Lotka Volterra Time Series (Target)" type: series background: #white size: {1, 0.5} position: {0, 0.5} {
        data "Number of preys" value: target_model.preys_number color: #green;
        data "Number of predators" value: target_model.predators_number color: #red;
      }
    }
  }
}
