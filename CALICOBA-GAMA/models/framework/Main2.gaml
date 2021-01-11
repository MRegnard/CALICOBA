model Main

import "reference_models/Linear_function_objective.gaml"
import "target_models/Linear_function.gaml"

global skills: [calicoba] {
  ReferenceSystem reference_system;
  TargetModel target_model;

  init {
    do calicoba_init(step_interval: 0);

    create ReferenceSystem number: 1 returns: ref;
    reference_system <- first(ref);
    create TargetModel number: 1 returns: target;
    target_model <- first(target);

    do calicoba_setup();
    do dump();
  }

  reflex step {
    do calicoba_step();
    do dump();
  }

  action dump {
    if (cycle = 0) {
      string data <- "cycle,x,y,out1,out2";
      save data to: "out.csv" type: csv rewrite: true header: false;
    }
    string data <- string(cycle) + "," + string(target_model.param_x) + "," + string(target_model.param_y) + "," + string(target_model.out_1) + "," + string(target_model.out_1);
    save data to: "out.csv" type: csv rewrite: false header: false;
  }
}

experiment "Linear function" type: gui {
  output {
    display "Plots" {
      chart "Parameters’ Values" type: series size: {0.5, 0.5} position: {0, 0} {
        data "param_x" value: target_model.param_x color: #green marker: false;
        data "param_y" value: target_model.param_y color: #red marker: false;
      }

      chart "Objective’s Criticalities" type: series size: {0.5, 0.5} position: {0.5, 0} {
        data "out_1" value: world.get_objective("out_1") color: #black marker: false;
        data "out_2" value: world.get_objective("out_2") color: #orange marker: false;
      }

      chart "Parameter’s Actions" type: series size: {0.5, 0.5} position: {0, 0.5} {
        data "param_x" value: target_model.get_parameter_action("param_x") color: #green marker: false;
        data "param_y" value: target_model.get_parameter_action("param_y") color: #red marker: false;
      }
    }
  }
}
