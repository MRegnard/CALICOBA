model Main

import "target_models/2_param_1_mesure_2_obj.gaml"

global skills: [calicoba] {
  TargetModel target_model;

  init {
    do calicoba_init(step_interval: 5);

    create TargetModel number: 1 returns: target;
    target_model <- first(target);

    do calicoba_setup();
  }

  reflex step {
    do calicoba_step();
  }
}

experiment Experiment type: gui {
  output {
    display "Time Series" {
      chart "Parameters/Measures" type: series size: {0.5, 0.5} position: {0, 0.5} {
        data "p1" value: target_model.param_1 color: #red marker: false;
        data "p2" value: target_model.param_2 color: #green marker: false;
        data "m1" value: target_model.out_1 color: #blue marker: false;
      }

      chart "Parameters’ Actions" type: series size: {0.5, 0.5} position: {0.5, 0.5} {
        data "p1" value: target_model.get_parameter_action("param_1") color: #red marker: false;
        data "p2" value: target_model.get_parameter_action("param_2") color: #green marker: false;
      }

      chart "Criticalities" type: series size: {0.5, 0.5} position: {0, 0} {
        data "obj_1" value: world.get_objective("obj_1") color: #red marker: false;
        data "obj_2" value: world.get_objective("obj_2") color: #green marker: false;
      }
    }
  }
}
