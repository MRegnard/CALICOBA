#!/usr/bin/python3
import argparse
import configparser
import json
import logging
import pathlib
import random
import time
import typing as typ

import jmetal.algorithm.multiobjective.omopso as jm_omopso
import jmetal.core.problem as jm_pb
import jmetal.operator as jm_op
import jmetal.operator.mutation as jm_mut
import jmetal.util.archive as jm_arch
import jmetal.util.termination_criterion as jm_term
import numpy as np
import pymoo.algorithms.moo.nsga2 as pymoo_nsga2
import pymoo.algorithms.moo.nsga3 as pymoo_nsga3
import pymoo.core.problem as pymoo_pb
import pymoo.factory as pymoo_f
import pymoo.optimize as pymoo_opti
from jmetal.core.problem import S

import cobopti
import cobopti.config
import experiments_utils as exp_utils
import models
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
        'calicoba', 'PSO', 'NSGA-II', 'NSGA-III',
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
    arg_parser.add_argument('--no-dump', dest='dump', action='store_false', default=True,
                            help='data files will not be generated')
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
    default_dump_data = True
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

    noisy = default_noisy or args.noisy
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
        noisy_functions=noisy,
        noise_mean=get_or_default(args.noise_mean, default_noise_mean) if noisy else None,
        noise_stdev=get_or_default(args.noise_stdev, default_noise_stdev) if noisy else None,
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

    # Model instanciation
    model_factory = models.get_model_factory(models.FACTORY_SIMPLE)
    models_ = {
        k: (model_factory.generate_model(k), v)
        for k, v in test_utils.MODEL_SOLUTIONS.items()
        if not config.model_id or k == config.model_id
    }

    # Execution of each model
    global_results = {}
    for model, target_parameters in models_.values():
        logger.info(f'Testing model "{model.id}"')
        global_results[model.id] = []
        param_names = list(model.parameters_names)
        # Generation of initial values
        if config.parameters_values:
            params_iterator = [config.parameters_values]
        else:
            params_iterator = (tuple(
                test_utils.sobol_to_param(v, *model.get_parameter_domain(param_names[i])) for i, v in enumerate(point))
                for point in test_utils.SobolSequence(len(model.parameters_names), config.runs_number)
            )

        # Runs
        tested_params = []
        for run, p in enumerate(params_iterator):
            p_init = {param_names[i]: v for i, v in enumerate(p)}
            logger.info(f'Model "{model.id}": run {run + 1}/{config.runs_number}')
            if p in tested_params:
                logger.info('Already tested, skipped')
                continue
            tested_params.append(p)

            run_config = exp_utils.RunConfig(
                method=config.method,
                model=model,
                p_init=p_init,
                target_parameters=target_parameters,
                max_steps=config.max_steps,
                step_by_step=config.step_by_step,
                seed=config.seed,
                free_parameter=config.free_parameter,
                noisy=config.noisy_functions,
                noise_mean=config.noise_mean,
                noise_stdev=config.noise_stdev,
                output_dir=output_dir / model.id / test_utils.map_to_string(p_init) if output_dir else None,
                logger=logger,
                logging_level=config.log_level
            )
            if config.method == 'calicoba':
                result = evaluate_model_calicoba(run_config)
            else:
                result = evaluate_model_other(run_config)

            global_results[model.id].append({
                'p_init': p_init,
                'result': result,
            })
            if result.error_message:
                logger.info(f'Error: {result.error_message}')

        if config.dump_data and output_dir and config.runs_number > 1:
            logger.info('Saving results')
            with (output_dir / (model.id + '.json')).open(mode='w', encoding='utf8') as f:
                data = []
                for result in global_results[model.id]:
                    exp_res: exp_utils.RunResult = result['result']
                    data.append({
                        'x0': result["p_init"],
                        'solution_found': exp_res.solution_found,
                        'error': exp_res.error,
                        'cycles': exp_res.cycles_number,
                        'solution_cycle': exp_res.solution_cycle,
                        'speed': exp_res.time,
                        'points_number': exp_res.points_number,
                        'unique_points_number': exp_res.unique_points_number,
                        'chains_number': exp_res.created_chains,
                        'error_message': exp_res.error_message,
                    })
                json.dump(data, f)


def evaluate_model_calicoba(config: exp_utils.RunConfig) \
        -> exp_utils.RunResult:
    class VariableWrapper(cobopti.config.Variable):
        def __init__(self, name: str, bounds: typ.Tuple[float, float]):
            super().__init__(name, bounds[0], bounds[1])

    class ObjectiveFunctionWrapper(cobopti.config.ObjectiveFunction):
        def __init__(self, name: str, bounds: typ.Tuple[float, float], model_output: str):
            super().__init__(name, bounds[0], bounds[1])
            self.model_output = model_output

        def __call__(self, **model_inputs: float):
            return (model.evaluate(**model_inputs)[self.model_output]
                    + (test_utils.gaussian_noise(mean=config.noise_mean,
                                                 stdev=config.noise_stdev) if config.noisy else 0))

    logger = config.logger
    model = config.model

    logger.info(f'Starting from {test_utils.map_to_string(config.p_init)}')

    model.reset()  # Just in case

    result = cobopti.minimize(cobopti.config.CoBOptiConfig(
        variables_metadata=[
            VariableWrapper(var_name, model.get_parameter_domain(var_name))
            for var_name in model.parameters_names
        ],
        objective_functions=[
            ObjectiveFunctionWrapper(output_name, model.get_output_domain(output_name=output_name), output_name)
            for output_name in model.outputs_names
        ],
        x0=config.p_init,
        max_cycles=config.max_steps,
        step_by_step=config.step_by_step,
        expected_solutions=config.target_parameters,
        output_directory=config.output_dir,
        seed=config.seed,
        logging_level=config.logging_level,
    ))
    threshold = cobopti.agents.PointAgent.SAME_POINT_THRESHOLD
    solution_found = any(
        all(abs(expected_solution[pname] - result.solution[pname]) < threshold for pname in
            expected_solution)
        for expected_solution in config.target_parameters
    )

    return exp_utils.RunResult(
        solution_found=solution_found,
        error=result.error,
        cycles_number=result.cycles,
        solution_cycle=-1,
        time=result.time,
        points_number=result.points,
        unique_points_number=result.unique_points,
        created_chains=result.chains,
        error_message=result.error_message,
    )


def evaluate_model_other(config: exp_utils.RunConfig) -> exp_utils.RunResult:
    logger = config.logger
    method = config.method
    model = config.model
    p_init = dict(config.p_init)  # Copy to avoid eventual side effects

    for param_name in model.parameters_names:
        # Set value of other parameters if config.free_parameter is set
        if config.free_parameter and config.free_parameter != param_name:
            p_init[param_name] = config.target_parameters[0][param_name]

    logger.info(f'Starting from {test_utils.map_to_string(p_init)}')

    # def function(x: typ.Sequence[float]) -> typ.Sequence[float]:
    #     outputs = model.evaluate(**{input_name: x[i] for i, input_name in enumerate(model.parameters_names)})
    #     return [out_value + (test_utils.gaussian_noise(mean=noise_mean, stdev=noise_stdev) if noisy else 0)
    #             for out_name, out_value in outputs.items()]

    if config.seed is not None:
        random.seed(config.seed)

    # x0 = np.asarray([v for v in p_init.values()])
    # bounds = np.asarray(model.get_parameter_domain(param_name) for param_name in model.parameters_names)

    model.reset()

    pymoo_problem = PyMOOProblemWrapper(model, config.noise_mean, config.noise_stdev)
    pymoo_algorithm = None
    jmetal_algorithm = None
    jmetal_problem = JMetalProblemWrapper(model, config.noise_mean, config.noise_stdev)

    start_time = time.time()

    # if method == 'SA':  # Simulated Annealing
    #     res = other_methods.simulated_annealing(
    #         init_state=x0,
    #         objective_function=function,
    #         bounds=bounds,
    #         n_iterations=max_steps,
    #         step_size=0.1,
    #         init_temp=10,
    #     )

    # elif method == 'GSA':  # Generalized Simulated Annealing
    #     res = sp_opti.dual_annealing(
    #         func=function,
    #         bounds=bounds,
    #         maxiter=max_steps,
    #         seed=seed,
    #         x0=x0
    #     )

    # if method == 'DE':  # Differential Evolution
    #     pymoo_algorithm = pymoo_de.DE()
    #     res = sp_opti.differential_evolution(
    #         func=function,
    #         x0=x0,
    #         bounds=bounds,
    #         maxiter=max_steps,
    #         seed=seed
    #     )

    if method == 'PSO':  # Particle Swarm Optimization
        mutation_probability = 1 / len(model.parameters_names)
        max_evaluations = 10000
        swarm_size = 100
        jmetal_algorithm = ProbedOMOPSO(
            jmetal_problem,
            swarm_size=swarm_size,
            epsilon=0.0075,
            uniform_mutation=jm_op.UniformMutation(probability=mutation_probability, perturbation=0.5),
            non_uniform_mutation=jm_mut.NonUniformMutation(mutation_probability, perturbation=0.5,
                                                           max_iterations=max_evaluations // swarm_size),
            leaders=jm_arch.CrowdingDistanceArchive(100),
            termination_criterion=jm_term.StoppingByEvaluations(max_evaluations)
        )
        # pymoo_algorithm = pymoo_pso.PSO()
        # res = other_methods.pso(
        #     func=function,
        #     lb=[bounds[0][0]],
        #     ub=[bounds[0][1]],
        #     maxiter=max_steps,
        # )

    elif method == 'NSGA-II':
        pymoo_algorithm = pymoo_nsga2.NSGA2(
            # mutation=pymoo_pm.PolynomialMutation(prob=1 / len(model.parameters_names), eta=20)
        )

    elif method == 'NSGA-III':
        # Reference directions taken from doc’s example at https://pymoo.org/algorithms/moo/nsga3.html#Example
        ref_dirs = pymoo_f.get_reference_directions('das-dennis', len(model.outputs_names), n_partitions=12)
        pymoo_algorithm = pymoo_nsga3.NSGA3(ref_dirs=ref_dirs)

    def is_expected_solution(solution: typ.Sequence[float]) -> bool:
        """Checks whether the candidate solution is one of the expected solutions."""

        def solution_matches(s):
            return all(abs(s[pname] - solution[i]) < cobopti.agents.PointAgent.SAME_POINT_THRESHOLD
                       for i, pname in enumerate(s.keys()))

        return any(solution_matches(s) for s in config.target_parameters)

    if pymoo_algorithm:
        termination = pymoo_f.get_termination('n_eval', 10000)
        res = pymoo_opti.minimize(pymoo_problem, pymoo_algorithm, termination, seed=config.seed, verbose=False)
        return exp_utils.RunResult(
            solution_found=any(is_expected_solution(sol) for sol in res.X),
            error=False,
            cycles_number=termination.n_max_evals // pymoo_algorithm.pop_size,
            solution_cycle=-1,
            time=time.time() - start_time,
            points_number=len(pymoo_problem.points),
            unique_points_number=len(pymoo_problem.unique_points),
            created_chains=-1,
        )
    elif jmetal_algorithm:
        jmetal_algorithm.run()
        res = jmetal_algorithm.get_result()
        return exp_utils.RunResult(
            solution_found=any(is_expected_solution(sol.variables) for sol in res),
            error=False,
            cycles_number=jmetal_algorithm.cycles,
            solution_cycle=-1,
            time=time.time() - start_time,
            points_number=len(jmetal_problem.points),
            unique_points_number=len(jmetal_problem.unique_points),
            created_chains=-1,
        )
    raise ValueError(f'unknown method "{method}"')


class ProbedOMOPSO(jm_omopso.OMOPSO):
    """Adds a property for the number of cycles to the OMOPSO algorithm class."""

    cycles = 0

    def step(self):
        super().step()
        self.cycles += 1


class ProblemWrapper:
    def __init__(self, model: models.Model, noise_mean: float = None, noise_stdev: float = None):
        self.model = model
        self.noise_mean = noise_mean
        self.noise_stdev = noise_stdev
        self.noisy = noise_mean is not None and noise_stdev is not None
        self.points = []
        self.unique_points = []

    def on_solution_evaluation(self, solution):
        self.points.append(solution)
        if np.all([solution != s for s in self.unique_points]):
            self.unique_points.append(solution)


class PyMOOProblemWrapper(pymoo_pb.ElementwiseProblem, ProblemWrapper):
    def __init__(self, model: models.Model, noise_mean: float = None, noise_stdev: float = None):
        super().__init__(
            n_var=len(model.parameters_names),
            n_obj=len(model.outputs_names),
            xl=np.array([model.get_parameter_domain(param_name)[0] for param_name in model.parameters_names]),
            xu=np.array([model.get_parameter_domain(param_name)[1] for param_name in model.parameters_names]),
        )
        ProblemWrapper.__init__(self, model, noise_mean, noise_stdev)

    def _evaluate(self, x, out, *args, **kwargs):
        self.on_solution_evaluation(x)
        outputs = self.model.evaluate(**{input_name: x[i] for i, input_name in enumerate(self.model.parameters_names)})
        out['F'] = [
            out_value + (test_utils.gaussian_noise(mean=self.noise_mean, stdev=self.noise_stdev) if self.noisy else 0)
            for out_name, out_value in outputs.items()
        ]


class JMetalProblemWrapper(jm_pb.FloatProblem, ProblemWrapper):
    def __init__(self, model: models.Model, noise_mean: float = None, noise_stdev: float = None):
        super().__init__()
        ProblemWrapper.__init__(self, model, noise_mean, noise_stdev)
        self.number_of_variables = len(model.parameters_names)
        self.number_of_objectives = len(model.outputs_names)
        self.number_of_constraints = 0
        self.lower_bound = [model.get_parameter_domain(pname)[0] for pname in model.parameters_names]
        self.upper_bound = [model.get_parameter_domain(pname)[1] for pname in model.parameters_names]

    def evaluate(self, solution: S) -> S:
        x = solution.variables
        self.on_solution_evaluation(x)
        outputs = self.model.evaluate(**{input_name: x[i] for i, input_name in enumerate(self.model.parameters_names)})
        solution.objectives = [
            out_value + (test_utils.gaussian_noise(mean=self.noise_mean, stdev=self.noise_stdev) if self.noisy else 0)
            for out_name, out_value in outputs.items()
        ]
        return solution

    def get_name(self) -> str:
        return self.model.id


if __name__ == '__main__':
    main()
