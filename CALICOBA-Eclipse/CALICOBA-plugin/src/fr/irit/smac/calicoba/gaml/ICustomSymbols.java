package fr.irit.smac.calicoba.gaml;

/**
 * Interface declaring all custom symbols.
 * 
 * @author Damien Vergnet
 */
public interface ICustomSymbols {
  String CALICOBA_SKILL = "calicoba";
  String CALICOBA_INIT = "calicoba_init";
  String CALICOBA_SETUP = "calicoba_setup";
  String CALICOBA_STEP = "calicoba_step";
  String GET_OBJECTIVES = "get_objectives";
  String GET_OBJECTIVE = "get_objective";

  String REFERENCE_SYSTEM_SKILL = "calicoba_reference_system";
  String REFERENCE_SYSTEM_INIT = "system_init";

  String TARGET_MODEL_SKILL = "calicoba_target_model";
  String TARGET_MODEL_INIT = "model_init";
  String TARGET_MODEL_GET_PARAM_ACTION = "get_parameter_action";
  String GET_AGENT_MEMORY = "get_agent_memory";
}
