model LinearFunction

species TargetModel skills: [calicoba_target_model] {
  float param_x <- 5.0;
  float param_y <- -1.0;
  point reset_point <- {0.0, 0.0};

  float out_1 <- f1(param_x, param_y);
  float out_2 <- f2(param_x, param_y);

  bool should_reset <- false;
  bool reset <- false; // Set by CALICOBA plugin

  float mini <- -50.0;
  float maxi <- 50.0;

  list<float> function_x <- [];
  list<float> function_y <- [];
  list<float> function_out_1 <- [];
  list<float> function_out_2 <- [];

  init {
    do model_init();

  	loop i from: mini to: maxi step: 1 {
  		function_x <+ i;
      function_y <+ i;
  		function_out_1 <+ f1(i, i);
      function_out_2 <+ f2(i, i);
  	}
  }

  reflex update {
  	out_1 <- f1(param_x, param_y);
    out_2 <- f2(param_x, param_y);
  	should_reset <- (out_1 < mini) or (out_1 > maxi) or (out_2 < mini) or (out_2 > maxi);
  	if (should_reset or reset) {
  	  do reset();
  	  if (reset) {
  	    reset <- false;
  	  }
  	}
  }

  float f1(float x, float y) {
  	return 2 * x - 3 * y;
  }

  float f2(float x, float y) {
    return -5 * x + y;
  }

  action reset {
    write "reset";
    write string(param_x) + " " + string(param_y);
    if (reset_point = {0.0, 0.0}) {
      reset_point <- {param_x, param_y};
    }
    param_x <- reset_point.x;
    param_y <- reset_point.y;
    out_1 <- f1(param_x, param_y);
    out_2 <- f2(param_x, param_y);
    write string(out_1) + " " + string(out_2);
  }
}
