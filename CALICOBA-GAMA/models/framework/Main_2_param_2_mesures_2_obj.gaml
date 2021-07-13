model Main

global skills: [calicoba] {
  TargetModel target_model;

  float last_crit1;
  float last_crit2;
  int steps_after <- 10 const: true;
  int remaining_steps <- steps_after;

  bool learn <- true;
  float alpha <- 0.5 min: 0.0 max: 1.0;

  init {
    do calicoba_init(learn, alpha);

    create TargetModel number: 1 returns: target;
    target_model <- first(target);

    do calicoba_setup();
  }

  reflex step {
    do calicoba_step();

    float crit1 <- world.get_objective("obj_1");
    float crit2 <- world.get_objective("obj_2");
    if crit1 = last_crit1 and crit2 = last_crit2 {
      if remaining_steps = 0 {
        write "Calibrated (?) in " + string(cycle - steps_after) + " cycles.";
        do pause();
      } else {
        remaining_steps <- remaining_steps - 1;
      }
    }
    last_crit1 <- crit1;
    last_crit2 <- crit2;
    if (cycle > 30) {
      do pause();
    }
  }
}

species TargetModel skills: [calicoba_target_model] {
  float param_1 <- 21.0;
  float _param_2 <- 12.0;
  float out_1 <- param_1;
  float out_2 <- _param_2;
  obj_def obj_1 <- obj_def([["out_1", "out_2"], "obj_1_"]);
  obj_def obj_2 <- obj_def([["out_2"], "obj_2_"]);

  init {
    do model_init();
  }

  reflex update {
    out_1 <- param_1;
    out_2 <- _param_2;
  }

  float obj_1_(float out_1_, float out_2_) {
    return abs(out_1_ - out_2_) - 23;
  }

  float obj_2_(float out_2_) {
    return out_2_ - 12;
  }

  float get_parameter_influence(string param_name, float param_value, string obj_name, float obj_value, TargetModel this) {
    float i;

    if obj_name = "obj_1" {
      if param_name = "param_1" {
        i <- (abs(this.obj_1_(param_value + 1, this._param_2)) >= abs(this.obj_1_(param_value, this._param_2))) ? 1.0 : -1.0;
      } else {
        i <- (abs(this.obj_1_(this.param_1, param_value + 1)) >= abs(this.obj_1_(this.param_1, param_value))) ? 1.0 : -1.0;
      }
    } else {
      if param_name = "param_1" {
        i <- 0.0;
      } else {
        i <- (param_value >= 12) ? 1.0 : -1.0;
      }
    }

    return i;
  }
}

experiment Experiment type: gui {
  parameter "Learn?" var: learn;
  parameter "α" var: alpha;

  output {
    display "Time Series" {
      chart "Criticalities" type: series size: {0.5, 0.5} position: {0, 0} {
        data "obj_1" value: abs(world.get_objective("obj_1")) color: #black marker: false;
        data "obj_2" value: abs(world.get_objective("obj_2")) color: #blue marker: false;
      }

      chart "Influences" type: series size: {0.5, 0.5} position: {0.5, 0} {
        data "p1/obj1" value: world.get_influence("param_1", "obj_1") color: #red marker: false;
        data "p1/obj2" value: world.get_influence("param_1", "obj_2") color: #green marker: false;
//        data "p2/obj1" value: world.get_influence("param_2", "obj_1") color: #blue marker: false;
//        data "p2/obj2" value: world.get_influence("param_2", "obj_2") color: #black marker: false;
      }

      chart "Parameters/Outputs" type: series size: {0.5, 0.5} position: {0, 0.5} {
        data "p1" value: target_model.param_1 color: #red marker: false;
        data "p2" value: target_model._param_2 color: #green marker: false;
      }

      chart "Parameters’ Actions" type: series size: {0.5, 0.5} position: {0.5, 0.5} {
        data "p1" value: world.get_parameter_action("param_1") color: #red marker: false;
//        data "p2" value: world.get_parameter_action("param_2") color: #green marker: false;
      }
    }
  }
}
