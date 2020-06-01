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
    string header <- "cycle,nb preys (ref),nb predators (ref),nb preys (target)," +
        "nb predators (target),parameter α,parameter α action,helped obj," +
        "expected variation,obj preys nb criticality,obj predators nb criticality";
    save header to: "out.csv" type: csv rewrite: true header: false;
  }

  reflex step {
    do calicoba_step();
    // XXX debug
    write target_model.get_agent_memory("param_preys_birth_rate");
    triplet t <- target_model.get_parameter_action("param_preys_birth_rate");
    write t;
    string line <- string(cycle) + "," +
        string(reference_system.out_preys_number) + "," +
        string(reference_system.out_predators_number) + "," +
        string(target_model.out_preys_number) + "," +
        string(target_model.out_predators_number) + "," +
        string(target_model.param_preys_birth_rate) + "," +
        string(t.first) + "," +
        string(t.second) + "," +
        string(t.third) + "," +
        string(world.get_objective("out_preys_number")) + "," +
        string(world.get_objective("out_predators_number"));
    save line to: "out.csv" type: csv header: false rewrite: false;
  }
}

experiment "Lotka Volterra" type: gui {
  output {
    display "Time Series" {
      chart "Lotka Volterra Time Series" type: series background: #white size: {0.5, 0.5} position: {0, 0} {
        data "Number of preys (Reference)" value: reference_system.out_preys_number color: #green marker: false;
        data "Number of predators (Reference)" value: reference_system.out_predators_number color: #red marker: false;
        data "Number of preys (Target)" value: target_model.out_preys_number color: #green marker: false;
        data "Number of predators (Target)" value: target_model.out_predators_number color: #red marker: false;
      }

      chart "Parameters" type: series background: #white size: {0.5, 0.5} position: {0, 0.5} {
        data "Preys birth rate (α)" value: target_model.param_preys_birth_rate color: #black marker: false;
      }

      chart "Parameters’ Actions" type: series background: #white size: {0.5, 0.5} position: {0.5, 0.5} {
        data "Preys birth rate (α)" value: float(target_model.get_parameter_action("param_preys_birth_rate").first) color: #black marker: false;
      }

      chart "Objectives’ Criticalities" type: series background: #white size: {0.5, 0.5} position: {0.5, 0} {
        data "out_preys_number" value: world.get_objective("out_preys_number") color: #green marker: false;
        data "out_predators_number" value: world.get_objective("out_predators_number") color: #red marker: false;
//        // FIXME
//        loop obj over: map<string, float>(world.get_objectives()).pairs {
//          data obj.key value: world.get_objective(obj.key) color: rnd_color(255);
//        }
      }
    }
  }
}
