"""This module defines functions to find the global optimum of a system."""
import math
import typing as typ

import numpy as np
import scipy.optimize


def simulated_annealing(init_state, objective_function, bounds, n_iterations: int, step_size: float, init_temp: float) \
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


# noinspection PyDefaultArgument
def pso(func: typ.Callable[[typ.Sequence[float], typ.Any], float],
        lb: typ.Sequence[float], ub: typ.Sequence[float], ieqcons=(), f_ieqcons=None, args=(), kwargs={},
        swarmsize=100, omega=0.5, phip=0.5, phig=0.5, maxiter=100, minstep=1e-8, minfunc=1e-8, debug=False):
    """Perform a particle swarm optimization (PSO). Adapted from: https://pypi.org/project/pyswarm/

    :param func: The function to be minimized.
    :param lb: The lower bounds of the design variable(s).
    :param ub: The upper bounds of the design variable(s).
    :param ieqcons: A list of functions of length n such that ieqcons[j](x, *args) ≥ 0 in
        a successfully optimized problem (Default: []).
    :param f_ieqcons: Returns a 1-D array in which each element must be greater or equal to 0.0 in
        a successfully optimized problem. If f_ieqcons is specified, ieqcons is ignored (Default: None).
    :param args: Additional arguments passed to objective and constraint functions (Default: empty tuple).
    :param kwargs: Additional keyword arguments passed to objective and constraint functions (Default: empty dict).
    :param swarmsize: The number of particles in the swarm (Default: 100).
    :param omega: Particle velocity scaling factor (Default: 0.5).
    :param phip: Scaling factor to search away from the particle's best known position (Default: 0.5).
    :param phig: Scaling factor to search away from the swarm's best known position (Default: 0.5).
    :param maxiter: The maximum number of iterations for the swarm to search (Default: 100).
    :param minstep: The minimum stepsize of swarm's best position before the search terminates (Default: 1e-8).
    :param minfunc: The minimum change of swarm's best objective value before the search terminates (Default: 1e-8).
    :param debug: If True, progress statements will be displayed every iteration (Default: False).
    """

    assert len(lb) == len(ub), 'Lower- and upper-bounds must be the same length'
    assert hasattr(func, '__call__'), 'Invalid function handle'
    lb = np.array(lb)
    ub = np.array(ub)
    assert np.all(ub > lb), 'All upper-bound values must be greater than lower-bound values'

    vhigh = np.abs(ub - lb)
    vlow = -vhigh

    def obj(x_):
        return func(x_, *args, **kwargs)

    # Check for constraint function(s) #########################################
    if f_ieqcons is None:
        if not len(ieqcons):
            if debug:
                print('No constraints given.')
            cons = lambda _: np.array([0])
        else:
            if debug:
                print('Converting ieqcons to a single constraint function')
            cons = lambda p: np.array([y(p, *args, **kwargs) for y in ieqcons])
    else:
        if debug:
            print('Single constraint function given in f_ieqcons')
        cons = lambda p: np.array(f_ieqcons(p, *args, **kwargs))

    def is_feasible(x_):
        return np.all(cons(x_) >= 0)

    # Initialize the particle swarm ############################################
    d = len(lb)  # the number of dimensions each particle has
    x = np.random.rand(swarmsize, d)  # particle positions
    v = np.zeros_like(x)  # particle velocities
    best_p = np.zeros_like(x)  # best particle positions
    best_fp = np.zeros(swarmsize)  # best particle function values
    g = []  # best swarm position
    fg = 1e100  # artificial best swarm position starting value
    nb_eval = 0

    for i in range(swarmsize):
        # Initialize the particle's position
        x[i, :] = lb + x[i, :] * (ub - lb)

        # Initialize the particle's best known position
        best_p[i, :] = x[i, :]

        # Calculate the objective's value at the current particle's
        best_fp[i] = obj(best_p[i, :])
        nb_eval += 1

        # At the start, there may not be any feasible starting point, so just
        # give it a temporary "best" point since it's likely to change
        if i == 0:
            g = best_p[0, :].copy()

        # If the current particle's position is better than the swarm's,
        # update the best swarm position
        if best_fp[i] < fg and is_feasible(best_p[i, :]):
            fg = best_fp[i]
            g = best_p[i, :].copy()

        # Initialize the particle's velocity
        v[i, :] = vlow + np.random.rand(d) * (vhigh - vlow)

    # Iterate until termination criterion met ##################################
    it = 1
    stop = False
    while not stop and it <= maxiter:
        rp = np.random.uniform(size=(swarmsize, d))
        rg = np.random.uniform(size=(swarmsize, d))
        for i in range(swarmsize):

            # Update the particle's velocity
            v[i, :] = omega * v[i, :] + phip * rp[i, :] * (best_p[i, :] - x[i, :]) + \
                      phig * rg[i, :] * (g - x[i, :])

            # Update the particle's position, correcting lower and upper bound
            # violations, then update the objective function value
            x[i, :] = x[i, :] + v[i, :]
            mark1 = x[i, :] < lb
            mark2 = x[i, :] > ub
            x[i, mark1] = lb[mark1]
            x[i, mark2] = ub[mark2]
            fx = obj(x[i, :])
            nb_eval += 1

            # Compare particle's best position (if constraints are satisfied)
            if fx < best_fp[i] and is_feasible(x[i, :]):
                best_p[i, :] = x[i, :].copy()
                best_fp[i] = fx

                # Compare swarm's best position to current particle's position
                # (Can only get here if constraints are satisfied)
                if fx < fg:
                    if debug:
                        print('New best for swarm at iteration {:}: {:} {:}'.format(it, x[i, :], fx))

                    tmp = x[i, :].copy()
                    stepsize = np.sqrt(np.sum((g - tmp) ** 2))
                    if np.abs(fg - fx) <= minfunc:
                        if debug:
                            print('Stopping search: Swarm best objective change less than {:}'.format(minfunc))
                        g = tmp
                        fg = fx
                        stop = True
                    elif stepsize <= minstep:
                        if debug:
                            print('Stopping search: Swarm best position change less than {:}'.format(minstep))
                        g = tmp
                        fg = fx
                        stop = True
                    else:
                        g = tmp.copy()
                        fg = fx
            if stop:
                break

        if debug:
            print('Best after iteration {:}: {:} {:}'.format(it, g, fg))
        it += 1

    if debug:
        print('Stopping search: maximum iterations reached --> {:}'.format(maxiter))

    return scipy.optimize.OptimizeResult(
        x=g,
        success=is_feasible(g),
        fun=np.array([fg]),
        nfev=nb_eval,
        nit=it,
    )
