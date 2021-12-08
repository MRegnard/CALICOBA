"""This module defines functions to find the global optimum of a system."""
import math
import typing as typ

import numpy as np
import scipy.optimize

State = typ.TypeVar('State')


def descrete_hill_climbing(init_state: typ.Sequence[float], i_max: int,
                           get_neighbors: typ.Callable[[typ.Sequence[float]], typ.Iterable[typ.Sequence[float]]],
                           objective_function: typ.Callable[[typ.Sequence[float]], float]) \
        -> scipy.optimize.OptimizeResult:
    """Performs the descrete space hill climbing algorithm on a system.
    Algorithm from: https://en.wikipedia.org/wiki/Hill_climbing

    :param init_state: Initial state.
    :param i_max: Maximum allowed number of steps.
    :param get_neighbors: A function that returns the neighbors for the given state.
    :param objective_function: A function that evaluates the fitness of the given state.
    :return: A locally optimal state. It cannot be guaranted that it will be the global optimum.
    """
    best_solution = init_state
    best_score = objective_function(best_solution)
    evaluations = 0
    i = 0
    stop = False

    while not stop and i < i_max:
        neighbors = get_neighbors(best_solution)
        better_neighbor_found = False
        for neighbor in neighbors:
            neighbor_score = objective_function(neighbor)
            evaluations += 1
            if neighbor_score < best_score:
                best_solution = neighbor
                best_score = neighbor_score
                better_neighbor_found = True
        if not better_neighbor_found:
            stop = True
        else:
            i += 1

    return scipy.optimize.OptimizeResult(
        x=np.array([best_solution]),
        success=True,
        fun=np.array([best_score]),
        nfev=evaluations,
        nit=i,
    )


def simulated_annealing(init_state, objective_function, bounds, n_iterations, step_size, init_temp) \
        -> scipy.optimize.OptimizeResult:
    """Performs the simulated annealing algorithm on a model.
    Code from: https://machinelearningmastery.com/simulated-annealing-from-scratch-in-python/

    :return: The best solution and its score.
    """
    best = init_state
    best_eval = objective_function(best)
    evaluations = 0
    curr, curr_eval = best, best_eval
    for i in range(n_iterations):
        candidate = curr + np.random.randn(len(bounds)) * step_size
        candidate_eval = objective_function(candidate)
        evaluations += 1
        if candidate_eval < best_eval:
            best, best_eval = candidate, candidate_eval
        diff = candidate_eval - curr_eval
        # calculate temperature for current epoch
        t = init_temp / (i + 1)
        # calculate metropolis acceptance criterion
        metropolis = math.exp(-diff / t)
        # check if we should keep the new point
        if diff < 0 or np.random.rand() < metropolis:
            curr, curr_eval = candidate, candidate_eval

    return scipy.optimize.OptimizeResult(
        x=np.array([best]),
        success=True,
        fun=np.array([best_eval]),
        nfev=evaluations,
        nit=n_iterations,
    )
