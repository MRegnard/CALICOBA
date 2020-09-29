model LinearFunction

species TargetModel skills: [calicoba_target_model] {
  float param_action <- 0.0;
  float out_x <- 0.0;
  float reset_point <- 0.0;

  float out_y <- f(out_x);

  bool should_reset <- false;
  bool reset <- false; // Set by CALICOBA plugin

  float x_min <- -50.0;
  float x_max <- 50.0;

  list<float> function_x <- [];
  list<float> function_y <- [];

  init {
    do model_init();

  	loop i from: x_min to: x_max step: 1 {
  		function_x <+ i;
  		function_y <+ f(i);
  	}
  }

  reflex update {
  	out_x <- out_x + param_action;
  	out_y <- f(out_x);
  	should_reset <- (out_x < x_min) or (out_x > x_max);
  	if (should_reset or reset) {
  	  do reset();
  	  if (reset) {
  	    reset <- false;
  	  }
  	}
  }

  float f(float z) {
  	return 2 * z;
  }

  action reset {
    write "reset";
    write out_x;
    if (reset_point = 0) {
      reset_point <- out_x;
    }
    out_x <- reset_point;
    out_y <- f(out_x);
    write out_x;
    param_action <- 0.0;
  }
}
