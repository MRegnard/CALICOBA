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
      string data <- "cycle,x,y,action";
      save data to: "dump.csv" type: csv rewrite: true header: false;
    }
    string data <- string(cycle) + "," + string(target_model.out_x) + "," + string(target_model.out_y) + "," + string(target_model.param_action);
    save data to: "dump.csv" type: csv rewrite: false header: false;
  }
}

experiment "Linear function" type: gui {
  output {
    display "Plots" {
      chart "Function" type: xy size: {0.5, 0.5} position: {0, 0} {
        data "f(x) = 2x" value: rows_list(matrix([target_model.function_x, target_model.function_y])) color: #black marker: false;
        data "Point" value: [target_model.out_x, target_model.out_y] color: #red line_visible: false;
        data "Target point" value: [0.0, reference_system.out_y] color: #green line_visible: false;
      }

      chart "Parameter’s Actions" type: series size: {0.5, 0.5} position: {0.5, 0.5} {
        data "param_action" value: target_model.get_parameter_action("param_action") color: #black marker: false;
      }

      chart "Objective’s Criticalities" type: series size: {0.5, 0.5} position: {0.5, 0} {
        data "out_x" value: world.get_objective("out_x") color: #black marker: false;
        data "out_y" value: world.get_objective("out_y") color: #orange marker: false;
      }
    }
  }
}
