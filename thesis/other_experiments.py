#!/usr/bin/python3
import argparse
import logging
import math
import pathlib
import random
import typing as typ

import numpy as np
import scipy.optimize as sp_opti

import models
import other_methods
import test_utils

Map = typ.Dict[str, float]


def desired_parameters(*values: float) -> Map:
    return {f'p{i + 1}': v for i, v in enumerate(values)}


def desired_outputs(*values: float) -> Map:
    return {f'o{i + 1}': v for i, v in enumerate(values)}


DEFAULT_DIR = pathlib.Path('output/experiments')
DEFAULT_RUNS_NB = 200
DEFAULT_MAX_STEPS_NB = 1000
DEFAULT_LOGGING_LEVEL = 'info'


def sobol_to_param(v: float, mini: float, maxi: float) -> float:
    return v * (maxi - mini) + mini


def map_to_string(m: Map, sep: str = ';') -> str:
    return sep.join(f'{k}={v}' for k, v in m.items())


def main():
    arg_parser = argparse.ArgumentParser(description='Run experiments with Simulated Annealing approach.')
    arg_parser.add_argument('method', type=str, help='method to use to explore the model', choices=[
        'SA', 'DA', 'HC', 'BH',
    ])
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
    arg_parser.add_argument('-s', '--seed', dest='seed', type=int,
                            help='seed for the random numbers generator')
    arg_parser.add_argument('-o', '--output-dir', metavar='PATH', dest='output_dir', type=pathlib.Path,
                            default=DEFAULT_DIR, help=f'output directory for dumped files (default: {DEFAULT_DIR})')
    arg_parser.add_argument('--no-dump', dest='dump', action='store_false',
                            help='prevent dumping of generated data to files')
    args = arg_parser.parse_args()
    p_method: str = args.method
    p_model_id: typ.Optional[str] = args.model_id
    p_param_values: typ.Tuple[str] = args.param_values
    p_free_param: typ.Optional[str] = args.free_param
    p_runs_nb: int = args.runs if not p_param_values else 1
    p_max_steps: int = args.max_steps
    p_seed: int = args.seed
    p_dump: bool = args.dump
    p_output_dir: typ.Optional[pathlib.Path] = args.output_dir.absolute() if p_dump else None

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

    p_output_dir /= p_method

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
        'square': ([desired_parameters(0)], desired_outputs(0)),
        'gramacy_and_lee_2012': ([desired_parameters(0.548563)], desired_outputs(-0.869011)),
        'ackley_function': ([desired_parameters(0)], desired_outputs(0)),
        'levy_function': ([desired_parameters(1)], desired_outputs(0)),
        'rastrigin_function': ([desired_parameters(0)], desired_outputs(0)),
        'styblinski_tang_function': ([desired_parameters(-39.16599)], desired_outputs(-2.903534))
    }
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
            global_optimum_found, found_solution, error, calibration_speed = evaluate_model(
                p_method,
                model,
                p_init,
                target_parameters,
                target_outputs,
                free_param=p_free_param,
                max_steps=p_max_steps,
                seed=p_seed,
                output_dir=p_output_dir,
                logger=logger
            )
            global_results[model.id].append({
                'p_init': p_init,
                'found': global_optimum_found,
                'solution': found_solution,
                'error': error,
                'speed': calibration_speed,
            })

            if p_dump and p_runs_nb > 1:
                logger.info('Saving results')
                with (p_output_dir / (model.id + '.csv')).open(mode='w', encoding='UTF-8') as f:
                    f.write('P(0),solution found,solution,error,speed\n')
                    for result in global_results[model.id]:
                        values = [
                            map_to_string(result['p_init']),
                            str(int(result['found'])),
                            str(result['solution']),
                            str(int(result['error'])),
                            str(result['speed']),
                        ]
                        f.write(','.join(values) + '\n')


def evaluate_model(method: str, model: models.Model, p_init: Map, solutions: typ.Sequence[Map], target_outputs: Map, *,
                   free_param: str = None, max_steps: int = DEFAULT_MAX_STEPS_NB,
                   seed: int = None, output_dir: pathlib.Path = None,
                   logger: logging.Logger = None) \
        -> typ.Tuple[bool, float, bool, int]:
    for param_name in model.parameters_names:
        if free_param and free_param != param_name:
            p_init[param_name] = solutions[1][param_name]

    logger.info(f'Starting from {map_to_string(p_init)}')

    target_out = target_outputs['o1']
    ε = 0.01
    if output_dir:
        output = output_dir / model.id / map_to_string(p_init)
    else:
        output = None

    def function(x):
        return model.evaluate(p1=x[0])['o1']

    if seed is not None:
        random.seed(seed)

    model.reset()

    if method == 'SA':  # Simulated Annealing
        best, best_eval, speed = other_methods.simulated_annealing(
            init_state=np.asarray([p_init['p1']]),
            objective_function=function,
            bounds=np.asarray([model.get_parameter_domain('p1')]),
            n_iterations=max_steps,
            step_size=0.1,
            init_temp=10,
            output_dir=output
        )
        return abs(best_eval - target_out) < ε, best[0], False, speed

    elif method == 'DA':  # Dual Annealing
        res = sp_opti.dual_annealing(
            func=function,
            bounds=np.asarray([model.get_parameter_domain('p1')]),
            maxiter=max_steps,
            seed=seed,
            x0=np.asarray([p_init['p1']])
        )
        return abs(res.fun - target_out) < ε, res.x[0], False, res.nit

    elif method == 'HC':  # Hill Climbing
        def get_neighbors(x):
            out_inf, out_sup = model.get_output_domain('o1')
            step = (out_sup - out_inf) / 1000
            return [[x[0] - step], [x[0] + step]]

        best, best_eval, speed = other_methods.descrete_hill_climbing(
            init_state=[p_init['p1']],
            i_max=max_steps,
            get_neighbors=get_neighbors,
            objective_function=function,
            output_dir=output
        )
        return abs(best_eval - target_out) < ε, best[0], False, speed

    elif method == 'BH':  # Basin-hopping
        res = sp_opti.basinhopping(
            func=function,
            x0=np.asarray([p_init['p1']]),
            niter=max_steps,
            seed=seed
        )
        return abs(res.fun - target_out) < ε, res.x[0], False, res.nit

    raise ValueError(f'unknown method "{method}"')


if __name__ == '__main__':
    main()
