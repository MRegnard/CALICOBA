#!/usr/bin/python3
import argparse
import logging
import pathlib
import random
import typing as typ

import numpy as np
import scipy.optimize as sp_opti

import experiments_utils as exp_utils
import models
import other_methods
import test_utils

DEFAULT_DIR = pathlib.Path('output/experiments')


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
    arg_parser.add_argument('-r', '--runs', metavar='NB', dest='runs', type=int, default=test_utils.DEFAULT_RUNS_NB,
                            help=f'number of runs (default: {test_utils.DEFAULT_RUNS_NB})')
    arg_parser.add_argument('--max-steps', metavar='NB', dest='max_steps', type=int,
                            default=test_utils.DEFAULT_MAX_STEPS_NB,
                            help=f'maximum number of simulation steps (default: {test_utils.DEFAULT_MAX_STEPS_NB})')
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
    models_ = {k: (model_factory.generate_model(k), v) for k, v in test_utils.MODEL_SOLUTIONS.items()}
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
                test_utils.sobol_to_param(v, *model.get_parameter_domain(param_names[i])) for i, v in enumerate(point))
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
                p_method,
                model,
                p_init,
                target_parameters,
                target_outputs,
                free_param=p_free_param,
                max_steps=p_max_steps,
                seed=p_seed,
                logger=logger
            )
            global_results[model.id].append({
                'p_init': p_init,
                'result': result,
            })

        if p_dump and p_runs_nb > 1:
            logger.info('Saving results')
            with (p_output_dir / (model.id + '.csv')).open(mode='w', encoding='UTF-8') as f:
                f.write(
                    'P(0),solution found,error,speed,# of visited points,# of unique visited points,error message\n')
                for result in global_results[model.id]:
                    exp_res: exp_utils.ExperimentResult = result['result']
                    f.write(f'{test_utils.map_to_string(result["p_init"])},{int(exp_res.solution_found)},'
                            f'{int(exp_res.error)},{exp_res.speed},{exp_res.points_number},'
                            f'{exp_res.unique_points_number},"{exp_res.error_message or ""}"\n')


def evaluate_model(method: str, model: models.Model, p_init: test_utils.Map, solutions: typ.Sequence[test_utils.Map],
                   target_outputs: test_utils.Map, *, free_param: str = None,
                   max_steps: int = test_utils.DEFAULT_MAX_STEPS_NB, seed: int = None, logger: logging.Logger = None) \
        -> exp_utils.ExperimentResult:
    for param_name in model.parameters_names:
        if free_param and free_param != param_name:
            p_init[param_name] = solutions[1][param_name]

    logger.info(f'Starting from {test_utils.map_to_string(p_init)}')

    target_out = target_outputs['o1']
    ε = 0.01

    def function(x):
        return model.evaluate(p1=x[0])['o1']

    if seed is not None:
        random.seed(seed)

    model.reset()

    if method == 'SA':  # Simulated Annealing
        res = other_methods.simulated_annealing(
            init_state=np.asarray([p_init['p1']]),
            objective_function=function,
            bounds=np.asarray([model.get_parameter_domain('p1')]),
            n_iterations=max_steps,
            step_size=0.1,
            init_temp=10,
        )
        return exp_utils.ExperimentResult(
            solution_found=abs(res.fun - target_out) < ε,
            error=False,
            speed=res.nit,
            points_number=res.nfev,
            unique_points_number=res.nfev,
        )

    elif method == 'DA':  # Dual Annealing
        res = sp_opti.dual_annealing(
            func=function,
            bounds=np.asarray([model.get_parameter_domain('p1')]),
            maxiter=max_steps,
            seed=seed,
            x0=np.asarray([p_init['p1']])
        )
        return exp_utils.ExperimentResult(
            solution_found=abs(res.fun - target_out) < ε,
            error=False,
            speed=res.nit,
            points_number=res.nfev,
            unique_points_number=res.nfev,
        )

    elif method == 'HC':  # Hill Climbing
        def get_neighbors(x):
            out_inf, out_sup = model.get_output_domain('o1')
            step = (out_sup - out_inf) / 1000
            return [[x[0] - step], [x[0] + step]]

        res = other_methods.descrete_hill_climbing(
            init_state=[p_init['p1']],
            i_max=max_steps,
            get_neighbors=get_neighbors,
            objective_function=function,
        )
        return exp_utils.ExperimentResult(
            solution_found=abs(res.fun - target_out) < ε,
            error=False,
            speed=res.nit,
            points_number=res.nfev,
            unique_points_number=res.nfev,
        )

    elif method == 'BH':  # Basin-hopping
        res = sp_opti.basinhopping(
            func=function,
            x0=np.asarray([p_init['p1']]),
            niter=max_steps,
            seed=seed
        )
        return exp_utils.ExperimentResult(
            solution_found=abs(res.fun - target_out) < ε,
            error=False,
            speed=res.nit,
            points_number=res.nfev,
            unique_points_number=res.nfev,
        )

    # TODO ajouter des algos G (-> scipy.optimize.differential_evolution())

    raise ValueError(f'unknown method "{method}"')


if __name__ == '__main__':
    main()
