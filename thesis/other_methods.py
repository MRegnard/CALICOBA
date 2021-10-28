"""This module defines functions to find the global optimum of a system."""
import math
import random
import typing as typ

State = typ.TypeVar('State')
SequenceState = typ.Sequence[float]


def descrete_hill_climbing(s0: State, i_max: int,
                           get_neighbors: typ.Callable[[State], typ.Iterable[State]],
                           evaluate: typ.Callable[[State], float]) -> State:
    """Performs the descrete space hill climbing algorithm on a system.
    Algorithm from: https://en.wikipedia.org/wiki/Hill_climbing

    :param s0: Initial state.
    :param i_max: Maximum allowed number of steps.
    :param get_neighbors: A function that returns the neighbors for the given state.
    :param evaluate: A function that evaluates the fitness of the given state.
    :return: A locally optimal state. It cannot be guaranted that it will be the global optimum.
    """
    state = s0
    i = 0
    stop = False

    while not stop and i < i_max:
        neighbors = get_neighbors(state)
        next_eval = -math.inf
        next_state = None
        for neighbor in neighbors:
            if evaluate(neighbor) > next_eval:
                next_state = neighbor
                next_eval = evaluate(neighbor)
        if next_eval <= evaluate(state):
            stop = True
        else:
            state = next_state
            i += 1

    return state


def continuous_hill_climbing(s0: SequenceState, initial_step_sizes: typ.Sequence[float], acceleration: float,
                             i_max: int, precision: float,
                             evaluate: typ.Callable[[SequenceState], float]) -> SequenceState:
    """Performs the continuous space hill climbing algorithm on a system.
    Algorithm from: https://en.wikipedia.org/wiki/Hill_climbing

    :param s0: Initial state.
    :param initial_step_sizes: Initial step sizes.
    :param acceleration: Base acceleration for step size variation.
    :param i_max: Maximum allowed number of steps.
    :param precision: Threshold to consider the best score to be equal to the previous score.
    :param evaluate: A function that returns a score for the given state.
    :return: A locally optimal state. It cannot be guaranted that it will be the global optimum.
    """
    state = list(s0)
    step_sizes = list(initial_step_sizes)
    if len(state) != len(step_sizes) or len(state) == 0 or len(step_sizes) == 0:
        raise ValueError('state and step_sizes must have the same length > 0')
    candidates = [-acceleration, -1 / acceleration, 1 / acceleration, acceleration]
    best_score = evaluate(state)
    i = 0
    stop = False

    while not stop and i < i_max:
        before_score = best_score
        for j, state_item in enumerate(state):
            best_step = 0
            for candidate in candidates:
                step = step_sizes[j] * candidate
                state[j] = state_item + step
                score = evaluate(state)
                if score > best_score:
                    best_score = score
                    best_step = step
            if best_step == 0:
                state[j] = state_item
                step_sizes[j] = step_sizes[j] / acceleration
            else:
                state[j] = state_item + best_step
                step_sizes[j] = best_step // acceleration
        if best_score - before_score < precision:
            stop = True
        else:
            i += 1

    return state


def simulated_annealing(s0: State, i_max: int, max_energy: float,
                        get_energy: typ.Callable[[State], float],
                        get_temperature: typ.Callable[[float], float],
                        get_neighbor: typ.Callable[[State], State],
                        get_probability: typ.Callable[[float, float], float]) -> State:
    """Performs the simulated annealing algorithm on a system.
    Algorithm from: https://fr.wikipedia.org/wiki/Recuit_simul%C3%A9

    :param s0: Initial state.
    :param i_max: Maximum allowed number of steps.
    :param max_energy: Maximum allowed energy for the solution state.
    :param get_energy: A function that returns the energy for the given state.
    :param get_temperature: A function that returns the temperature for the given proportion of elapsed iterations.
    :param get_neighbor: A function that returns a random neighbor for the given state.
    :param get_probability: A function that returns a probability for the given energy variation and temperature.
    :return: The global optimum.
    """
    state = s0
    optimal_state = s0
    energy = get_energy(state)
    optimal_energy = get_energy(optimal_state)
    i = 0

    while i < i_max and energy > max_energy:
        new_state = get_neighbor(state)
        new_energy = get_energy(new_state)
        if new_energy < energy or random.random() < get_probability(new_energy - energy, get_temperature(i / i_max)):
            state = new_state
            energy = new_energy
        if energy < optimal_energy:
            optimal_state = state
            optimal_energy = energy
        i += 1

    return optimal_state
