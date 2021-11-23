#!/usr/bin/python3
from __future__ import annotations

import argparse
import logging
import math
import pathlib
import typing as typ

import calicoba
import experiments_utils as exp_utils
import models
import test_utils

Map = typ.Dict[str, float]


def desired_parameters(*values: float) -> Map:
    return {f'p{i + 1}': v for i, v in enumerate(values)}


def desired_outputs(*values: float) -> Map:
    return {f'o{i + 1}': v for i, v in enumerate(values)}


DEFAULT_DIR = pathlib.Path('output/experiments/calicoba')
DEFAULT_RUNS_NB = 200
DEFAULT_MAX_STEPS_NB = 1000
DEFAULT_LOGGING_LEVEL = 'info'


def sobol_to_param(v: float, mini: float, maxi: float) -> float:
    return v * (maxi - mini) + mini


def map_to_string(m: Map, sep: str = ';') -> str:
    return sep.join(f'{k}={v}' for k, v in m.items())


def main():
    arg_parser = argparse.ArgumentParser(description='Run experiments on CALICOBA.')
    arg_parser.add_argument('-m', '--model', dest='model_id', type=str, default=None,
                            help='ID of the model to experiment on')
    arg_parser.add_argument('-p', '--params', metavar='VALUE', dest='param_values', type=float, nargs='+', default=[],
                            help='list of parameter values')
    arg_parser.add_argument('-t', '--threshold', dest='null_threshold', type=float, default=1e-4,
                            help='seed for the random numbers generator')
    arg_parser.add_argument('--free-param', metavar='NAME', dest='free_param', type=str,
                            help='name of the parameter to let free while all others are fixed')
    arg_parser.add_argument('-r', '--runs', metavar='NB', dest='runs', type=int, default=DEFAULT_RUNS_NB,
                            help=f'number of runs (default: {DEFAULT_RUNS_NB})')
    arg_parser.add_argument('--max-steps', metavar='NB', dest='max_steps', type=int, default=DEFAULT_MAX_STEPS_NB,
                            help=f'maximum number of simulation steps (default: {DEFAULT_MAX_STEPS_NB})')
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
    p_null_threshold: float = args.null_threshold
    p_runs_nb: int = args.runs if not p_param_values else 1
    p_max_steps: int = args.max_steps
    p_seed: int = args.seed
    p_dump: bool = args.dump
    p_output_dir: typ.Optional[pathlib.Path] = args.output_dir.absolute() if p_dump else None
    p_logging_level: int = vars(logging)[args.logging_level.upper()]

    logging.basicConfig()
    logger = logging.getLogger(__name__)
    logger.setLevel(p_logging_level)

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

    solutions = {
        'model_1': ([desired_parameters(-12), desired_parameters(12)], desired_outputs(0)),
        'model_2': ([desired_parameters(-11, 12), desired_parameters(35, 12)], desired_outputs(0, 0)),
        'model_3': ([desired_parameters(2, 12), desired_parameters(-2, 12)], desired_outputs(0, 0)),
        # Partial solutions
        'model_4': ([desired_parameters(-21, 20), desired_parameters(-19, 20)], desired_outputs(0, 0)),
        'model_5': ([desired_parameters(math.sqrt(3 - math.sqrt(3)))],
                    desired_outputs(-4 * math.sqrt(3 - math.sqrt(3)) * (3 + math.sqrt(3)))),
        'square': ([desired_parameters(0)], desired_outputs(0)),
        'gramacy_and_lee_2012': ([desired_parameters(0.548563)], desired_outputs(-0.869011)),
        'ackley_function': ([desired_parameters(0)], desired_outputs(0)),
        'levy_function': ([desired_parameters(1)], desired_outputs(0)),
        'rastrigin_function': ([desired_parameters(0)], desired_outputs(0)),
        'styblinski_tang_function': ([desired_parameters(-39.16599)], desired_outputs(-2.903534))
    }

    model_factory = models.get_model_factory(models.FACTORY_SIMPLE)
    models_ = {k: (model_factory.generate_model(k), v) for k, v in solutions.items()}
    if p_model_id:
        models_ = {p_model_id: models_[p_model_id]}
    else:
        models_ = models_

    global_results = {}
    for model, (target_parameters, target_outputs) in models_.values():
        logger.info(f'Testing model "{model.id}"')
        global_results[model.id] = []
        param_names = list(model.parameters_names)
        if p_param_values:
            params_iterator = [p_param_values]
        else:
            params_iterator = (tuple(
                sobol_to_param(v, *model.get_parameter_domain(param_names[i])) for i, v in enumerate(point))
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
            result = evaluate_model(
                model,
                p_init,
                target_outputs,
                p_null_threshold,
                free_param=p_free_param,
                max_steps=p_max_steps,
                seed=p_seed,
                output_dir=p_output_dir,
                logger=logger,
                logging_level=p_logging_level
            )
            global_results[model.id].append({
                'p_init': p_init,
                'result': result,
            })

        if p_dump and p_runs_nb > 1:
            logger.info('Saving results')
            with (p_output_dir / (model.id + '.csv')).open(mode='w', encoding='utf8') as f:
                f.write('p(0),solution found,error,speed,# of visited points,# of unique visited points\n')
                for result in global_results[model.id]:
                    exp_res: exp_utils.ExperimentResult = result['result']
                    f.write(f'{map_to_string(result["p_init"])},{int(exp_res.solution_found)},'
                            f'{int(exp_res.error)},{exp_res.speed},{exp_res.points_number}\n')
        else:
            print(global_results[model.id])


def evaluate_model(model: models.Model, p_init: Map, target_outputs: Map, null_threshold: float, *,
                   free_param: str = None, max_steps: int = DEFAULT_MAX_STEPS_NB, seed: int = None,
                   output_dir: pathlib.Path = None, logger: logging.Logger = None, logging_level: int = logging.INFO) \
        -> exp_utils.ExperimentResult:
    logger.info(f'Starting from {map_to_string(p_init)}')

    model.reset()
    system = calicoba.Calicoba(calicoba.CalicobaConfig(
        dump_directory=output_dir / model.id / map_to_string(p_init),
        seed=seed,
        logging_level=logging_level,
    ))

    param_files = {}
    for param_name in model.parameters_names:
        param_files[param_name] = (output_dir / (param_name + '.csv')).open('w', encoding='utf8')
        param_files[param_name].write('cycle,value,selected objective,objective criticality,decider,step,steps,'
                                      'decision\n')
        model.set_parameter(param_name, p_init[param_name])
        inf, sup = model.get_parameter_domain(param_name)
        if not free_param or free_param == param_name:
            system.add_parameter(param_name, inf, sup)

    obj_functions = {}
    for output_name in model.outputs_names:
        inf, sup = model.get_output_domain(output_name)
        objective_name = 'obj_' + output_name
        obj_functions[objective_name] = SimpleObjectiveFunction(target_outputs[output_name], output_name)
        inf = 0
        sup = max(obj_functions[objective_name](o1=inf), obj_functions[objective_name](o1=sup))
        system.add_objective(objective_name, inf, sup)

    system.setup()
    solution_found = False
    calibration_speed = 0
    points = []
    unique_points = []
    error = False
    for i in range(max_steps):
        calibration_speed = i
        model.update()
        params = {p_name: model.get_parameter(p_name) for p_name in model.parameters_names}
        p = sorted(params.items())
        points.append(p)
        if p not in unique_points:
            unique_points.append(p)
        objs = {
            obj_name: obj_function(**{
                out_name: model.get_output(out_name)
                for out_name in obj_function.parameter_names
            })
            for obj_name, obj_function in obj_functions.items()
        }

        if all([abs(obj) < null_threshold for obj in objs.values()]):
            solution_found = True
            break

        # noinspection PyBroadException
        try:
            logger.debug(params, objs)
            suggestions = system.suggest_new_point(params, objs)
        except BaseException as e:
            logger.exception(e)
            error = True
            break
        logger.debug(suggestions)

        for param_name, suggestion in suggestions.items():
            s = suggestion[0]
            param_files[param_name].write(f'{i},{model.get_parameter(param_name)},{s.selected_objective},'
                                          f'{s.criticality},{s.agent.parameter_value},{s.step},{s.steps_number},'
                                          f'"{s.reason}"\n')
            model.set_parameter(param_name, s.next_point)

        input('Paused')  # TEST

    return exp_utils.ExperimentResult(
        solution_found=solution_found,
        error=error,
        speed=calibration_speed,
        points_number=len(points),
        unique_points_number=len(unique_points),
    )


class SimpleObjectiveFunction(calicoba.agents.ObjectiveFunction):
    def __init__(self, target_value: float, *parameter_names):
        super().__init__(*parameter_names)
        self._target_value = target_value

    def __call__(self, **kwargs: float):
        return abs(kwargs[self.parameter_names[0]] - self._target_value)


if __name__ == '__main__':
    main()
