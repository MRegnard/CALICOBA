#!/usr/bin/python3
import argparse
import configparser
import logging
import math
import pathlib
import random
import time
import typing as typ

import numpy as np
import scipy.optimize as sp_opti

import calicoba
import experiments_utils as exp_utils
import models
import other_methods
import test_utils

DEFAULT_DIR = pathlib.Path('output/experiments')
DEFAULT_LOGGING_LEVEL = 'info'
DEFAULT_RUNS_NB = 200
DEFAULT_MAX_STEPS_NB = 1000
DEFAULT_NULL_THRESHOLD = 0.005
DEFAULT_NOISE_MEAN = 0
DEFAULT_NOISE_STDEV = 0.01


def get_config() -> exp_utils.ExperimentsConfig:
    def get_or_default(v, default):
        return v if v is not None else default

    arg_parser = argparse.ArgumentParser(description='Run experiments with selected method(s) on selected model(s).')
    arg_parser.add_argument('-c', '--config', dest='config_file', metavar='FILE', type=pathlib.Path,
                            help='path to config file')
    arg_parser.add_argument('--method', dest='method', metavar='METHOD', type=str, choices=(
        'calicoba', 'SA', 'GSA', 'BH', 'NM', 'DE', 'PSO',
    ), help='method to use to explore the model')
    arg_parser.add_argument('-m', '--model', dest='model_id', type=str, help='ID of the model to experiment on')
    arg_parser.add_argument('-p', '--params', metavar='VALUE', dest='param_values', type=float, nargs='+', default=[],
                            help='list of parameter values')
    arg_parser.add_argument('-t', '--threshold', dest='null_threshold', type=float,
                            help='seed for the random numbers generator')
    arg_parser.add_argument('--free-param', metavar='NAME', dest='free_param', type=str,
                            help='name of the parameter to let free while all others are fixed')
    arg_parser.add_argument('-r', '--runs', metavar='NB', dest='runs', type=int,
                            help=f'number of runs (default: {DEFAULT_RUNS_NB})')
    arg_parser.add_argument('--max-steps', metavar='NB', dest='max_steps', type=int,
                            help=f'maximum number of simulation steps (default: {DEFAULT_MAX_STEPS_NB})')
    arg_parser.add_argument('--step-by-step', dest='step_by_step', action='store_true',
                            help='enable step by step for CALICOBA')
    arg_parser.add_argument('-s', '--seed', dest='seed', type=int,
                            help='seed for the random numbers generator')
    arg_parser.add_argument('-o', '--output-dir', metavar='PATH', dest='output_dir', type=pathlib.Path,
                            help=f'output directory for dumped files (default: {DEFAULT_DIR})')
    arg_parser.add_argument('-d', '--dump', dest='dump', action='store_true', help='dump generated data to files')
    arg_parser.add_argument('-l', '--level', metavar='LEVEL', dest='logging_level', type=str,
                            choices=('debug', 'info', 'warning', 'error', 'critical'),
                            help='logging level among debug, info, warning, error and critical '
                                 f'(default: {DEFAULT_LOGGING_LEVEL})')
    arg_parser.add_argument('-n', '--noisy', dest='noisy', action='store_true', help='add noise to model outputs')
    arg_parser.add_argument('--mean', metavar='MEAN', dest='noise_mean', type=float, help='mean of gaussian noise')
    arg_parser.add_argument('--stdev', metavar='STDEV', dest='noise_stdev', type=float,
                            help='standard deviation of gaussian noise')
    args = arg_parser.parse_args()

    default_method = None
    default_model_id = None
    default_seed = None
    default_null_crit = DEFAULT_NULL_THRESHOLD
    default_params_values = []
    default_free_param = None
    default_runs_nb = DEFAULT_RUNS_NB
    default_max_steps = DEFAULT_MAX_STEPS_NB
    default_step_by_step = False
    default_output_dir = DEFAULT_DIR
    default_dump_data = False
    default_log_level = DEFAULT_LOGGING_LEVEL
    default_noisy = False
    default_noise_mean = DEFAULT_NOISE_MEAN
    default_noise_stdev = DEFAULT_NOISE_STDEV

    if args.config_file:
        config_parser = configparser.ConfigParser()
        if not args.config_file.exists():
            raise FileNotFoundError(args.config_file)
        config_parser.read(args.config_file, encoding='utf8')

        method = config_parser.get('Main', 'method')
        if not method:
            raise ValueError('missing method')

        params_values = config_parser.get('Parameters', 'parameters', fallback=default_params_values)
        if params_values:
            params_values = [float(v) for v in params_values.split(',')]

        default_method = method
        default_model_id = config_parser.get('Main', 'model', fallback=default_model_id)
        default_seed = config_parser.getint('Main', 'seed', fallback=default_seed)
        default_null_crit = config_parser.getfloat('Main', 'null_criticality_threshold', fallback=default_null_crit)
        default_params_values = params_values
        default_free_param = config_parser.get('Parameters', 'free_parameter', fallback=default_free_param)
        default_runs_nb = config_parser.getint('Run', 'runs_number', fallback=default_runs_nb)
        default_max_steps = config_parser.getint('Run', 'max_steps', fallback=default_max_steps)
        default_step_by_step = config_parser.getboolean('Run', 'step_by_step', fallback=default_step_by_step)
        default_output_dir = config_parser.get('Output', 'output_directory', fallback=default_output_dir)
        if isinstance(default_output_dir, str):
            default_output_dir = pathlib.Path(default_output_dir)
        default_dump_data = config_parser.getboolean('Output', 'dump_data', fallback=default_dump_data)
        default_log_level = config_parser.get('Output', 'log_level', fallback=default_log_level)
        default_noisy = config_parser.getboolean('Noise', 'noisy', fallback=default_noisy)
        default_noise_mean = config_parser.getfloat('Noise', 'noise_mean', fallback=default_noise_mean)
        default_noise_stdev = config_parser.getfloat('Noise', 'noise_stdev', fallback=default_noise_stdev)

    method = get_or_default(args.method, default_method)
    if not method:
        raise ValueError('missing method')
    dump_data = default_dump_data or args.dump

    return exp_utils.ExperimentsConfig(
        method=method,
        model_id=get_or_default(args.model_id, default_model_id),
        seed=get_or_default(args.seed, default_seed),
        null_crit_threshold=get_or_default(args.null_threshold, default_null_crit),
        parameters_values=args.param_values or default_params_values,
        free_parameter=get_or_default(args.free_param, default_free_param),
        runs_number=get_or_default(args.runs, default_runs_nb),
        max_steps=get_or_default(args.max_steps, default_max_steps),
        step_by_step=default_step_by_step or args.step_by_step,
        output_directory=get_or_default(args.output_dir, default_output_dir).absolute() if dump_data else None,
        dump_data=dump_data,
        log_level=vars(logging)[get_or_default(args.logging_level, default_log_level).upper()],
        noisy_functions=default_noisy or args.noisy,
        noise_mean=get_or_default(args.noise_mean, default_noise_mean),
        noise_stdev=get_or_default(args.noise_stdev, default_noise_stdev),
    )


def main():
    config = get_config()

    logging.basicConfig()
    logger = logging.getLogger(__name__)
    logger.setLevel(config.log_level)

    log_message = 'Running experiments'
    if config.model_id:
        log_message += f' model "{config.model_id}"'
    else:
        log_message += ' all models'
    if config.runs_number:
        log_message += f' for {config.runs_number} run(s)'
        if not config.model_id:
            log_message += ' each'
    if config.parameters_values:
        log_message += ' with parameter values ' + ', '.join(map(str, config.parameters_values))
    logger.info(log_message)

    output_dir = config.output_directory
    if output_dir:
        output_dir /= config.method
        if config.noisy_functions:
            output_dir /= 'noisy'

        if config.dump_data and not output_dir.exists():
            output_dir.mkdir(parents=True)
    model_factory = models.get_model_factory(models.FACTORY_SIMPLE)
    models_ = {
        k: (model_factory.generate_model(k), v)
        for k, v in test_utils.MODEL_SOLUTIONS.items()
        if not config.model_id or k == config.model_id
    }
    global_results = {}
    for model, target_parameters in models_.values():
        logger.info(f'Testing model "{model.id}"')
        global_results[model.id] = []
        param_names = list(model.parameters_names)
        if config.parameters_values:
            params_iterator = [config.parameters_values]
        else:
            params_iterator = (tuple(
                test_utils.sobol_to_param(v, *model.get_parameter_domain(param_names[i])) for i, v in enumerate(point))
                for point in test_utils.SobolSequence(len(model.parameters_names), config.runs_number)
            )
        tested_params = []
        for run, p in enumerate(params_iterator):
            p_init = {param_names[i]: v for i, v in enumerate(p)}
            logger.info(f'Model "{model.id}": run {run + 1}/{config.runs_number}')
            if p in tested_params:
                logger.info('Already tested, skipped')
                continue
            tested_params.append(p)
            if config.method == 'calicoba':
                result = evaluate_model_calicoba(model, p_init, target_parameters, free_param=config.free_parameter,
                                                 step_by_step=config.step_by_step, max_steps=config.max_steps,
                                                 seed=config.seed, noisy=config.noisy_functions,
                                                 noise_mean=config.noise_mean, noise_stdev=config.noise_stdev,
                                                 output_dir=output_dir / model.id / test_utils.map_to_string(
                                                     p_init) if output_dir else None, logger=logger,
                                                 logging_level=config.log_level)
            else:
                result = evaluate_model_other(config.method, model, p_init, target_parameters,
                                              noisy=config.noisy_functions, noise_mean=config.noise_mean,
                                              noise_stdev=config.noise_stdev, free_param=config.free_parameter,
                                              max_steps=config.max_steps, seed=config.seed, logger=logger)
            global_results[model.id].append({
                'p_init': p_init,
                'result': result,
            })
            if result.error_message:
                logger.info(f'Error: {result.error_message}')

        if config.dump_data and output_dir and config.runs_number > 1:
            logger.info('Saving results')
            with (output_dir / (model.id + '.csv')).open(mode='w', encoding='utf8') as f:
                f.write('P(0),solution found,error,cycles_number,solution_cycle,speed,# of visited points,'
                        '# of unique visited points,error message\n')
                for result in global_results[model.id]:
                    exp_res: exp_utils.ExperimentResult = result['result']
                    f.write(f'{test_utils.map_to_string(result["p_init"])},{int(exp_res.solution_found)},'
                            f'{int(exp_res.error)},{exp_res.cycles_number},{exp_res.solution_cycle},{exp_res.time},'
                            f'{exp_res.points_number},{exp_res.unique_points_number},"{exp_res.error_message or ""}"\n')


def evaluate_model_calicoba(model: models.Model, p_init: test_utils.Map, solutions: typ.Sequence[test_utils.Map], *,
                            free_param: str = None, step_by_step: bool = False, max_steps: int = DEFAULT_MAX_STEPS_NB,
                            seed: int = None, noisy: bool = False, noise_mean: float = DEFAULT_NOISE_MEAN,
                            noise_stdev: float = DEFAULT_NOISE_STDEV, output_dir: pathlib.Path = None,
                            logger: logging.Logger = None, logging_level: int = logging.INFO) \
        -> exp_utils.ExperimentResult:
    class SimpleObjectiveFunction(calicoba.agents.ObjectiveFunction):
        def __init__(self, *outputs_names, noise=False):
            super().__init__(*outputs_names)
            self.noisy = noise

        def __call__(self, **outputs_values: float):
            return (outputs_values[self.outputs_names[0]]
                    + (test_utils.gaussian_noise(mean=noise_mean, stdev=noise_stdev) if self.noisy else 0))

    logger.info(f'Starting from {test_utils.map_to_string(p_init)}')

    model.reset()
    system = calicoba.Calicoba(calicoba.CalicobaConfig(
        dump_directory=output_dir,
        seed=seed,
        logging_level=logging_level,
    ))

    param_files = {}
    for param_name in model.parameters_names:
        param_files[param_name] = (output_dir / (param_name + '.csv')).open(mode='w', encoding='utf8')
        param_files[param_name].write('cycle,value,objective,criticality,decider,is min,step,steps,decision\n')
        model.set_parameter(param_name, p_init[param_name])
        inf, sup = model.get_parameter_domain(param_name)
        if not free_param or free_param == param_name:
            system.add_parameter(param_name, inf, sup)

    obj_functions = {}
    for output_name in model.outputs_names:
        inf, sup = model.get_output_domain(output_name)
        objective_name = 'obj_' + output_name
        obj_functions[objective_name] = SimpleObjectiveFunction(output_name, noise=noisy)
        system.add_objective(objective_name, inf, sup)

    system.setup()
    solution_found = False
    cycles_number = 0
    start_time = time.time()
    points = []
    unique_points = []
    error_message = ''
    lowest_solution = None
    lowest_crit = math.inf
    solution_cycle = -1
    for i in range(max_steps):
        cycles_number = i + 1
        model.update()
        params = {p_name: model.get_parameter(p_name) for p_name in model.parameters_names}
        p = sorted(params.items())
        points.append(p)
        if p not in unique_points:
            unique_points.append(p)
        objs = {
            obj_name: obj_function(**{
                out_name: model.get_output(out_name)
                for out_name in obj_function.outputs_names
            })
            for obj_name, obj_function in obj_functions.items()
        }

        logger.debug('Objectives: ' + str(objs.items()))

        # noinspection PyBroadException
        try:
            logger.debug(params, objs)
            suggestions = system.suggest_new_point(params, objs)
        except BaseException as e:
            logger.exception(e)
            error_message = str(e)
            break
        logger.debug(suggestions)

        for param_name, suggestion in suggestions.items():
            if not suggestion:
                error_message = 'no suggestions for parameter ' + param_name
                break
            s = suggestion[0]
            if isinstance(s, calicoba.agents.GlobalMinimumFound):
                solution_found = True
                # threshold = calicoba.agents.PointAgent.NULL_THRESHOLD
                # solution_found = any(abs(solution['p1'] - model.get_parameter('p1')) < threshold
                #                      for solution in solutions)
                solution_cycle = i + 1
            else:
                param_files[param_name].write(
                    f'{i},{model.get_parameter(param_name)},{s.selected_objective},'
                    f'{s.criticality},{s.agent.parameter_value},{int(s.agent.is_local_minimum)},'
                    f'{s.step},{s.steps_number},{s.decision}\n'
                )
                model.set_parameter(param_name, s.next_point)
                if lowest_crit > s.agent.criticality:
                    lowest_crit = s.agent.criticality
                    lowest_solution = params
                    solution_cycle = i + 1

        if solution_found or error_message:
            break

        if step_by_step:
            input('Paused')

    # Check if the global minimum was found but not detected by the system
    if not solution_found and not error_message and lowest_solution is not None:
        threshold = 0.01
        solution_found = any(abs(solution['p1'] - lowest_solution['p1']) < threshold for solution in solutions)

    total_time = time.time() - start_time

    for param_name in model.parameters_names:
        param_files[param_name].write(
            f'{cycles_number + 1},{model.get_parameter(param_name)},,,,,1,,,\n')

    return exp_utils.ExperimentResult(
        solution_found=solution_found,
        error=error_message != '',
        cycles_number=cycles_number,
        solution_cycle=solution_cycle,
        time=total_time,
        points_number=len(points),
        unique_points_number=len(unique_points),
        error_message=error_message,
    )


def evaluate_model_other(method: str, model: models.Model, p_init: test_utils.Map,
                         solutions: typ.Sequence[test_utils.Map], *, noisy: bool = False,
                         noise_mean: float = DEFAULT_NOISE_MEAN, noise_stdev: float = DEFAULT_NOISE_STDEV,
                         free_param: str = None, max_steps: int = DEFAULT_MAX_STEPS_NB, seed: int = None,
                         logger: logging.Logger = None) \
        -> exp_utils.ExperimentResult:
    for param_name in model.parameters_names:
        if free_param and free_param != param_name:
            p_init[param_name] = solutions[1][param_name]

    logger.info(f'Starting from {test_utils.map_to_string(p_init)}')

    def function(x):
        return (model.evaluate(p1=x[0])['o1']
                + (test_utils.gaussian_noise(mean=noise_mean, stdev=noise_stdev) if noisy else 0))

    if seed is not None:
        random.seed(seed)

    x0 = np.asarray([p_init['p1']])
    bounds = np.asarray([model.get_parameter_domain('p1')])
    res = None

    model.reset()

    start_time = time.time()

    if method == 'SA':  # Simulated Annealing
        res = other_methods.simulated_annealing(
            init_state=x0,
            objective_function=function,
            bounds=bounds,
            n_iterations=max_steps,
            step_size=0.1,
            init_temp=10,
        )

    elif method == 'GSA':  # Generalized Simulated Annealing
        res = sp_opti.dual_annealing(
            func=function,
            bounds=bounds,
            maxiter=max_steps,
            seed=seed,
            x0=x0
        )

    elif method == 'BH':  # Basin-hopping
        res = sp_opti.basinhopping(
            func=function,
            x0=x0,
            niter=max_steps,
            seed=seed
        )

    elif method == 'NM':  # Nelder-Mead
        res = sp_opti.minimize(
            method='Nelder-Mead',
            fun=function,
            x0=x0,
            bounds=bounds,
            options={'max_iter': max_steps}
        )

    elif method == 'DE':  # Differential Evolution
        res = sp_opti.differential_evolution(
            func=function,
            x0=x0,
            bounds=bounds,
            maxiter=max_steps,
            seed=seed
        )

    elif method == 'PSO':  # Particle Swarm Optimization
        res = other_methods.pso(
            func=function,
            lb=[bounds[0][0]],
            ub=[bounds[0][1]],
            maxiter=max_steps,
        )

    if res:
        threshold = calicoba.agents.PointAgent.NULL_THRESHOLD
        return exp_utils.ExperimentResult(
            solution_found=any(abs(solution['p1'] - res.x[0]) < threshold for solution in solutions),
            error=False,
            cycles_number=res.nit,
            solution_cycle=-1,
            time=time.time() - start_time,
            points_number=res.nfev,
            unique_points_number=res.nfev,
        )
    raise ValueError(f'unknown method "{method}"')


if __name__ == '__main__':
    main()
