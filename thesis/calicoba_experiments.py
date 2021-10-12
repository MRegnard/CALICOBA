#!/usr/bin/python3
import argparse
import logging
import math
import pathlib
import typing as typ

import numpy as np

import calicoba
import models
import test_utils

Map = typ.Dict[str, float]


def desired_parameters(*values: float) -> Map:
    return {f'p{i + 1}': v for i, v in enumerate(values)}


def desired_outputs(*values: float) -> Map:
    return {f'o{i + 1}': v for i, v in enumerate(values)}


DEFAULT_DIR = pathlib.Path('calicoba_output/experiments')
DEFAULT_RUNS_NB = 200
DEFAULT_MAX_STEPS_NB = 1000
DEFAULT_CALIBRATION_THRESHOLD = 60
DEFAULT_LOGGING_LEVEL = 'info'


def sobol_to_param(v: float, amplitude: float) -> int:
    return math.floor(v * amplitude - (amplitude / 2))


def map_to_string(m: Map, sep: str = ';') -> str:
    return sep.join(f'{k}={v}' for k, v in m.items())


def main():
    arg_parser = argparse.ArgumentParser(description='Run experiments on CALICOBA.')
    arg_parser.add_argument('-m', '--model', dest='model_id', type=str, default=None,
                            help='ID of the model to experiment on')
    arg_parser.add_argument('-p', '--params', metavar='VALUE', dest='param_values', type=float, nargs='+', default=[],
                            help='list of parameter values')
    arg_parser.add_argument('--free-param', metavar='NAME', dest='free_param', type=str,
                            help='name of the parameter to let free while all others are fixed')
    arg_parser.add_argument('-r', '--runs', metavar='NB', dest='runs', type=int, default=DEFAULT_RUNS_NB,
                            help=f'number of runs (default: {DEFAULT_RUNS_NB})')
    arg_parser.add_argument('--max-steps', metavar='NB', dest='max_steps', type=int, default=DEFAULT_MAX_STEPS_NB,
                            help=f'maximum number of simulation steps (default: {DEFAULT_MAX_STEPS_NB})')
    arg_parser.add_argument('-t', '--threshold', metavar='NB', dest='threshold', type=int,
                            default=DEFAULT_CALIBRATION_THRESHOLD,
                            help=f'calibration threshold (default: {DEFAULT_CALIBRATION_THRESHOLD})')
    arg_parser.add_argument('-s', '--seed', dest='seed', type=int,
                            help='seed for the random numbers generator')
    arg_parser.add_argument('-o', '--output-dir', metavar='PATH', dest='output_dir', type=pathlib.Path,
                            default=DEFAULT_DIR, help=f'output directory for dumped files (default: {DEFAULT_DIR})')
    arg_parser.add_argument('--no-dump', dest='dump', action='store_false',
                            help='prevent dumping of generated data to files')
    arg_parser.add_argument('-l', '--level', metavar='LEVEL', dest='logging_level', type=str,
                            default=DEFAULT_LOGGING_LEVEL, choices=('debug', 'info', 'warning', 'error', 'critical'),
                            help='logging level among debug, info, warning, error and critical '
                                 f'(default: {DEFAULT_LOGGING_LEVEL})')
    args = arg_parser.parse_args()
    p_model_id: typ.Optional[str] = args.model_id
    p_param_values: typ.Tuple[str] = args.param_values
    p_free_param: typ.Optional[str] = args.free_param
    p_runs_nb: int = args.runs if not p_param_values else 1
    p_max_steps: int = args.max_steps
    p_threshold: int = args.threshold
    p_seed: int = args.seed
    p_dump: bool = args.dump
    p_output_dir: typ.Optional[pathlib.Path] = args.output_dir.absolute() if p_dump else None
    p_logging_level: int = vars(logging)[args.logging_level.upper()]

    logging.basicConfig()
    logger = logging.getLogger(__name__)
    logger.setLevel(logging.INFO)

    log_message = 'Running experiments'
    if p_model_id:
        log_message += f' model "{p_model_id}"'
    else:
        log_message += ' all models'
    if p_runs_nb:
        log_message += f' for {p_runs_nb} run(s)'
        if not p_model_id:
            log_message += ' each'
    if p_param_values:
        log_message += f' with parameter values ' + ', '.join(map(str, p_param_values))
    logger.info(log_message)

    if p_dump and not p_output_dir.exists():
        p_output_dir.mkdir(parents=True)
    model_factory = models.get_model_factory(models.FACTORY_SIMPLE)
    solutions = {
        'model_1': ([desired_parameters(-12), desired_parameters(12)], desired_outputs(0)),
        'model_2': ([desired_parameters(-11, 12), desired_parameters(35, 12)], desired_outputs(0, 0)),
        'model_3': ([desired_parameters(2, 12), desired_parameters(-2, 12)], desired_outputs(0, 0)),
        # Partial solutions
        'model_4': ([desired_parameters(-21, 20), desired_parameters(-19, 20)], desired_outputs(0, 0)),
        'model_5': ([desired_parameters(math.sqrt(3 - math.sqrt(3)))],
                    desired_outputs(-4 * math.sqrt(3 - math.sqrt(3)) * (3 + math.sqrt(3)))),
        'gramacy_and_lee_2012': ([desired_parameters(0.548563)], desired_outputs(-0.869011)),
    }
    models_ = {k: (model_factory.generate_model(k), v) for k, v in solutions.items()}
    if p_model_id:
        models_ = {p_model_id: models_[p_model_id]}
    else:
        models_ = models_
    global_results = {}
    for model, (solutions, target_outputs) in models_.values():
        logger.info(f'Testing model "{model.id}"')
        global_results[model.id] = []
        param_names = list(model.parameters_names)
        if p_param_values:
            params_iterator = [p_param_values]
        else:
            params_iterator = (tuple(
                sobol_to_param(v, min(np.abs(model.get_parameter_domain(param_names[i])))) for i, v in enumerate(point))
                for point in test_utils.SobolSequence(len(model.parameters_names), p_runs_nb)
            )
        tested_params = []
        for run, p in enumerate(params_iterator):
            p_init = {param_names[i]: v for i, v in enumerate(p)}
            logger.info(f'Model "{model.id}": run {run + 1}/{p_runs_nb}')
            if p in tested_params:
                logger.info('Already tested, skipped')
                continue
            tested_params.append(p)
            param_values, calibration_speed, distances_to_solutions = evaluate_model(
                model,
                p_init,
                solutions,
                target_outputs,
                free_param=p_free_param,
                max_steps=p_max_steps,
                threshold=p_threshold,
                seed=p_seed,
                output_dir=p_output_dir,
                logger=logger,
                logging_level=p_logging_level
            )
            solution_index = int(np.argmin(distances_to_solutions))
            global_results[model.id].append({
                'p_init': p_init,
                'result': param_values,
                'closest_solution': solutions[solution_index],
                'distance': distances_to_solutions[solution_index],
                'speed': calibration_speed,
            })

            if p_dump and p_runs_nb > 1:
                logger.info('Saving results')
                with (p_output_dir / (model.id + '.csv')).open(mode='w', encoding='UTF-8') as f:
                    f.write('P(0),result,closest solution,distance,speed\n')
                    for result in global_results[model.id]:
                        values = [
                            map_to_string(result['p_init']),
                            map_to_string(result['result']),
                            map_to_string(result['closest_solution']),
                            str(result['distance']),
                            str(result['speed']),
                        ]
                        f.write(','.join(values) + '\n')


def evaluate_model(model: models.Model, p_init: Map, solutions: typ.Sequence[Map], target_outputs: Map, *,
                   free_param: str = None, max_steps: int = DEFAULT_MAX_STEPS_NB,
                   threshold: int = DEFAULT_CALIBRATION_THRESHOLD, seed: int = None, output_dir: pathlib.Path = None,
                   logger: logging.Logger = None, logging_level: int = logging.INFO) \
        -> typ.Tuple[Map, int, typ.List[float]]:
    def get_influence(p_name: str, p_value: float, obj_name: str, _: float) -> float:
        delta = 1e-6
        current_params = {name: model.get_parameter(name) for name in model.parameters_names}
        test_params = dict(current_params)
        test_params[p_name] = p_value + delta
        cf = crit_functions[obj_name]
        output1 = model.evaluate(**current_params)
        crit1 = abs(cf(**{p_name: output1[p_name] for p_name in cf.parameter_names}))
        output2 = model.evaluate(**test_params)
        crit2 = abs(cf(**{p_name: output2[p_name] for p_name in cf.parameter_names}))

        if crit1 < crit2:
            return 1
        elif crit1 > crit2:
            return -1
        else:
            return 0

    for param_name in model.parameters_names:
        if free_param and free_param != param_name:
            p_init[param_name] = solutions[1][param_name]

    logger.info(f'Starting from {map_to_string(p_init)}')

    model.reset()
    system = calicoba.Calicoba(calicoba.CalicobaConfig(
        dump_directory=output_dir / model.id / map_to_string(p_init),
        free_parameter=free_param,
        influence_function=get_influence,
        seed=seed,
        logging_level=logging_level,
    ))

    period_detectors = {}
    for param_name in model.parameters_names:
        model.set_parameter(param_name, p_init[param_name])
        inf, sup = model.get_parameter_domain(param_name)
        system.add_parameter(ModelInput(inf, sup, param_name, model))
        period_detectors[param_name] = test_utils.PeriodDetector(threshold)

    crit_functions = {}
    for output_name in model.outputs_names:
        inf, sup = model.get_output_domain(output_name)
        system.add_output(ModelOutput(inf, sup, output_name, model))
        objective_name = 'obj_' + output_name
        crit_functions[objective_name] = SimpleCriticalityFunction(target_outputs[output_name], output_name)
        system.add_objective(objective_name, crit_functions[objective_name])

    param_agents: typ.Dict[str, calicoba.agents.ParameterAgent] = {
        a.name: a for a in system.get_agents_for_type(calicoba.agents.ParameterAgent)
    }
    system.setup()
    # M2
    calibration_speed = 0
    for i in range(max_steps):
        model.update()
        system.step()
        for param_name, detector in period_detectors.items():
            detector.append(param_agents[param_name].value)
        calibration_speed = i
        # Have the model’s parameters converged?
        if all([d.is_full and d.has_converged for d in period_detectors.values()]):
            break

    # Get average of N last values for each parameter to get the tendency
    param_values = {name: np.mean(period_detectors[name].values()) for name in model.parameters_names}
    # Evaluate distance to each known solution (M1)
    distances = []
    for s in solutions:
        d = 0
        for k, v in param_values.items():
            d += (s[k] - v) ** 2
        distances.append(math.sqrt(d))

    return param_values, calibration_speed - threshold, distances


class SimpleCriticalityFunction(calicoba.agents.CriticalityFunction):
    def __init__(self, target_value: float, *parameter_names):
        super().__init__(*parameter_names)
        self._target_value = target_value

    def __call__(self, **kwargs: float):
        return kwargs[self.parameter_names[0]] - self._target_value


class ModelOutput(calicoba.data_sources.DataOutput):
    def __init__(self, inf: float, sup: float, name: str, model: models.Model):
        super().__init__(inf, sup, name)
        self._model = model

    def get_data(self) -> float:
        return self._model.get_output(self.name)


class ModelInput(ModelOutput, calicoba.data_sources.DataInput):
    def get_data(self) -> float:
        return self._model.get_parameter(self.name)

    def set_data(self, value: float):
        self._model.set_parameter(self.name, value)


if __name__ == '__main__':
    main()
