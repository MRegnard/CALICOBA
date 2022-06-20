from __future__ import annotations

import abc
import logging
import math
import random
import typing as typ

from . import _normalizers, _internal_suggestions as ig, _suggestions
from .. import utils, config, _data_types as dt

DIR_INCREASE = 1
DIR_DECREASE = -1
DIR_NONE = 0


class Agent(abc.ABC):
    """Base class for agents."""

    def __init__(self, name: str, *, logger: logging.Logger = None):
        self._name = name
        self._dead = False
        self._logger = logger
        self._world = None

    @property
    def world(self):
        """The environment this agent lives in, i.e. the CoBOpti instances that manages it.

        :rtype: cobopti.CoBOpti
        """
        return self._world

    @world.setter
    def world(self, w):
        """Set the environment this agent lives in, i.e. the CoBOpti instances that manages it.

        :type w: cobopti.CoBOpti
        """
        if self._world:
            raise ValueError(f'world already set for agent {self.name}')
        self._world = w

    @property
    def name(self) -> str:
        """The name of this agent."""
        return self._name

    def die(self):
        """Flags this agent for deletion at the end of the current cycle."""
        self._dead = True

    @property
    def dead(self) -> bool:
        """Whether this agent has been removed from the environment."""
        return self._dead

    def log_exception(self, exception):
        """Logs an exception.

        :param exception: The exception to log.
        """
        if self._logger:
            self._logger.exception(exception)

    def log_error(self, message):
        """Logs a message with the error level.

        :param message: The message to log.
        """
        if self._logger:
            self._logger.error(f'{self._get_logging_name()}: {message}')

    def log_critical(self, message):
        """Logs a message with the critical level.

        :param message: The message to log.
        """
        if self._logger:
            self._logger.critical(f'{self._get_logging_name()}: {message}')

    def log_warning(self, message):
        """Logs a message with the warning level.

        :param message: The message to log.
        """
        if self._logger:
            self._logger.warning(f'{self._get_logging_name()}: {message}')

    def log_info(self, message):
        """Logs a message with the info level.

        :param message: The message to log.
        """
        if self._logger:
            self._logger.info(f'{self._get_logging_name()}: {message}')

    def log_debug(self, message):
        """Logs a message with the debug level.

        :param message: The message to log.
        """
        if self._logger:
            self._logger.debug(f'{self._get_logging_name()}: {message}')

    def _get_logging_name(self):
        """Returns the string to use as the name for this agent in log messages.
        Defaults to this agent’s name property.
        """
        return self.name

    def __repr__(self):
        return f'Agent{{name={self.name}}}'


class ObjectiveAgent(Agent):
    """Dummy agent that acts as a holder of the current value and criticality
    of the objective function it represents.
    """

    def __init__(self, function: config.ObjectiveFunction):
        """Creates an objective agent.

        :param function: The function to wrap.
        """
        super().__init__(function.name)
        self._evaluations = 0
        self._points = []
        self._function = function
        self._objective_value = math.nan
        self._criticality = math.nan
        self._normalizer = _normalizers.BoundNormalizer(function.lower_bound, function.upper_bound)

    @property
    def objective_value(self) -> float:
        """Returns the value of the objective function for the current cycle."""
        return self._objective_value

    @property
    def criticality(self) -> float:
        """The criticality of this objective for the current cycle."""
        return self._criticality

    @property
    def evaluations(self) -> int:
        """The number of times the associated function has been evaluated."""
        return self._evaluations

    @property
    def points(self) -> typ.List[typ.Dict[str, float]]:
        """The list of all points the associated function has been evaluated on. May contain duplicates."""
        return list(self._points)

    def evaluate_function(self, *, update: bool = True, **kwargs: float):
        """Evaluates the objective function wrapped by this agent.

        :param update: Whether to update this agent after function evaluation.
        :param kwargs: Arguments to pass to the objective function.
        """
        self._evaluations += 1
        self._points.append(kwargs)
        objective_value = self._function(**kwargs)
        criticality = self._normalizer(objective_value)
        if update:
            self._objective_value = objective_value
            self._criticality = criticality

    def __repr__(self):
        return f'ObjectiveAgent{{name={self.name},criticality={self._criticality}}}'


class RegistryAgent(Agent):
    """A dummy “agent” used to gather global data."""
    MAX_STEPS_NUMBER = 2

    def __init__(self, *variables_metadata: config.Variable, logger: logging.Logger = None):
        """Creates a variable agent.

        :param variables_metadata: Metadata of variables to optimize.
        :param logger: The logger instance to use to log things.
        """
        super().__init__('registry', logger=logger)
        self._variables: typ.Dict[str, config.Variable] = {v.name: v for v in variables_metadata}
        self._variables_default_steps = dt.Vector(**{
            v.name: (v.upper_bound - v.lower_bound) / 100
            for v in variables_metadata
        })
        self._variables_names = sorted(self._variables.keys())
        self._variables_values = None
        self._variables_coefs = dt.Vector(**{v.name: abs(v.upper_bound - v.lower_bound) for v in variables_metadata})

        self.last_local_steps = dt.Vector.zero(*[var.name for var in variables_metadata])
        self.last_local_directions = dt.Vector.filled(DIR_NONE, *[var.name for var in variables_metadata])
        self.last_semi_local_steps = dt.Vector.zero(*[var.name for var in variables_metadata])
        self.last_semi_local_directions = dt.Vector.filled(DIR_NONE, *[var.name for var in variables_metadata])

        self._chains: typ.List[ChainAgent] = []
        self._minima: typ.List[PointAgent] = []
        self._last_point_id = 0
        self._last_chain_id = 0

    def get_variable_lower_bound(self, var_name: str) -> float:
        """The lower bound of a variable.

        :param var_name: Variable’s name.
        """
        return self._variables[var_name].lower_bound

    def get_variable_upper_bound(self, var_name: str) -> float:
        """The upper bound of a variable.

        :param var_name: Variable’s name.
        """
        return self._variables[var_name].upper_bound

    def is_within_bounds(self, var_name: str, value: float) -> bool:
        """Checks whether the given value is within the specified variable’s bounds.

        :param var_name: Variable’s name.
        :param value: A value.
        :returns: True if value is within bounds, False otherwise.
        """
        return self.get_variable_lower_bound(var_name) <= value <= self.get_variable_upper_bound(var_name)

    def clamp_value(self, var_name: str, value: float) -> float:
        """Clamps the given value within the bound of the specified variable.

        :param var_name: Variable’s name.
        :param value: A value to clamp.
        :returns: The clamped value.
        """
        return min(self.get_variable_upper_bound(var_name), max(self.get_variable_lower_bound(var_name), value))

    def get_variable_default_step(self, var_name: str) -> float:
        """The default variation step."""
        return self._variables_default_steps[var_name]

    @property
    def variables_names(self) -> typ.List[str]:
        return self._variables_names

    @property
    def variables_values(self) -> typ.Optional[dt.Vector[float]]:
        return self._variables_values

    @property
    def variables_coefficients(self) -> dt.Vector[float]:
        return self._variables_coefs

    @property
    def chains(self) -> typ.List[ChainAgent]:
        """The list of all chains managed by this agent. Returns a new list."""
        return list(self._chains)

    @property
    def minima(self) -> typ.Sequence[PointAgent]:
        """The list of all point agents that represent a local minimum. Returns a new list."""
        return list(self._minima)

    def add_minimum(self, point: PointAgent):
        """Adds a new point agent to the local minima list.

        :param point: The agent to add.
        """
        self._minima.append(point)

    def perceive(self, values: dt.Vector[float], new_chain: bool, criticalities: dt.Vector[float],
                 previous_suggestion: _suggestions.VariationSuggestion = None):
        """Updates this variable agent.

        :param values: Values of each variable.
        :param new_chain: Whether to create a new chain agent.
        :param criticalities: The criticalities of each objective.
        :param previous_suggestion: The variation suggestion accepted during the last cycle. May be null.
        """
        sampler = None
        if previous_suggestion is not None:
            if previous_suggestion.search_phase != _suggestions.SearchPhase.SEMI_LOCAL:
                self.last_local_steps = previous_suggestion.steps
                self.last_local_directions = previous_suggestion.directions
                if previous_suggestion.search_phase == _suggestions.SearchPhase.LOCAL_SAMPLING:
                    sampler = previous_suggestion.agent
            else:
                self.last_local_steps = dt.Vector(**{v.name: self.get_variable_default_step(v.name)
                                                     for v in self._variables.values()})
                self.last_semi_local_steps = previous_suggestion.steps
                self.last_local_directions = previous_suggestion.directions
                self.last_semi_local_directions = previous_suggestion.directions
        else:
            self.last_local_steps = dt.Vector.zero(*self._variables.keys())
            self.last_local_directions = dt.Vector.filled(DIR_NONE, *self._variables.keys())
        prev_point = self._chains[-1].current_point if self._chains else None

        if not prev_point or prev_point.variable_values != values or prev_point.criticalities != criticalities:
            new_point = PointAgent(
                f'point_{self._last_point_id}',
                self,
                values,
                criticalities,
                sampler=sampler,
                logger=self._logger
            )
            if sampler:
                sampler.sampled_points.append(new_point)
            self.world.add_agent(new_point)
            self._last_point_id += 1
            if self._chains and not new_chain:
                if p := [p for p in self._chains[-1].points if p.create_new_chain_from_me]:
                    p = p[0]
                    p.create_new_chain_from_me = False
                    # Duplicate the point that created a new chain and
                    # add it to this new chain to avoid breaking the previous one
                    duplicate = PointAgent(
                        p.name + '_dup',
                        self,
                        p.variable_values,
                        p.criticalities,
                        logger=self._logger
                    )
                    self.world.add_agent(duplicate)
                    self.__create_new_chain(duplicate)
                self._chains[-1].add_point(new_point)
            else:
                self.__create_new_chain(new_point)

    def __create_new_chain(self, first_point: PointAgent):
        if self._chains:
            self._chains[-1].is_active = False
        self._chains.append(ChainAgent(f'c{self._last_chain_id}', self, first_point, logger=self._logger))
        self.world.add_agent(self._chains[-1])
        self._last_chain_id += 1

    def __repr__(self):
        return f'RegistryAgent{{{self.name}}}'


class ChainAgent(Agent):
    """A chain agent manages a set of point agents and is managed by a variable agent."""

    def __init__(self, name: str, variable_agent: RegistryAgent, first_point: PointAgent,
                 *, logger: logging.Logger = None):
        """Creates a chain agent.

        :param name: Chain’s name.
        :param variable_agent: The variable agent that manages this chain.
        :param first_point: The first point agent that this chain will manage.
        :param logger: The logger to use to log things.
        """
        super().__init__(name, logger=logger)
        self._var_agent = variable_agent
        self._points = []
        self._minimum = first_point
        self._lower_extremum = first_point
        self._upper_extremum = first_point
        self._current_point = None  # Set by add_point below
        self.add_point(first_point)
        self.is_active = True
        self.local_min_found = False
        self.go_up_mode = False

    @property
    def variable(self) -> RegistryAgent:
        return self._var_agent

    @property
    def points(self) -> typ.Sequence[PointAgent]:
        """The list of all point agents managed by this chain. The returned value is a copy of internal data."""
        return list(self._points)

    @property
    def current_point(self) -> PointAgent:
        """The point agent that represents the current model state."""
        return self._current_point

    @property
    def minimum(self) -> PointAgent:
        """The point agent that has the lowest associated criticality."""
        return self._minimum

    @property
    def lower_extremum(self) -> PointAgent:
        """The agent that has the lowest variable value."""
        return self._lower_extremum

    @property
    def upper_extremum(self) -> PointAgent:
        """The agent that has the highest variable value."""
        return self._upper_extremum

    def add_point(self, p: PointAgent):
        """Adds a point agent to this chain.

        :param p: The point to add.
        """
        if p in self._points:
            raise ValueError(f'point {p} already in chain {self}')
        if self._current_point:
            self._current_point.is_current = False
            prev_point = self._points[-1]
            prev_point.next_point = p
        else:
            prev_point = None
        p.is_current = True
        p.next_point = None
        p.previous_point = prev_point
        p.chain = self
        self._current_point = p
        self._points.append(p)

    def remove_last_point(self):
        """Removes the last point from this chain.

        :raise ValueError: If this chain contains a single point.
        """
        if len(self._points) == 1:
            raise ValueError(f'cannot remove point from chain {self}')
        self._points.pop()

    def perceive(self):
        """Updates this chain."""
        if not self.is_active:
            return
        self._detect_minimum()
        # self._detect_extrema()  # TEMP disabled until hill-climbing is updated

    def _detect_minimum(self):
        """Detects the point agent with the lowest criticality of this chain."""
        self._minimum = min(self._points, key=lambda p: p.criticality)

    def _detect_extrema(self):
        """Detects the points with the lowest and highest variable values of this chain."""
        extremum_min = None
        extremum_max = None
        for point in self._points:
            point.is_extremum = False
            if not extremum_min or point.variable_value < extremum_min.variable_value:
                extremum_min = point
            if not extremum_max or point.variable_value > extremum_max.variable_value:
                extremum_max = point
        extremum_min.is_extremum = True
        extremum_max.is_extremum = True
        self._lower_extremum = extremum_min
        self._upper_extremum = extremum_max

    def _get_logging_name(self):
        return f'{self.variable.name}:{self.name}'

    def __repr__(self):
        return f'ChainAgent{{name={self.name},variable={self.variable.name},points={self._points}}}'


class PointAgent(Agent):
    """A point agent represents the values of all variables at a given time along with the states of all objectives."""
    LOCAL_MIN_THRESHOLD = 1e-4
    STUCK_THRESHOLD = 1e-4
    SAME_POINT_THRESHOLD = 0.01
    NULL_THRESHOLD = 5e-5

    def __init__(self, name: str, registry: RegistryAgent, values: dt.Vector[float],
                 objective_criticalities: dt.Vector[float],
                 *, sampler: PointAgent = None, logger: logging.Logger = None):
        """Creates a point agent.

        :param name: Point’s name.
        :param registry: The global registry.
        :param values: The current values of each variable.
        :param objective_criticalities: The current objectives criticalities.
        :param logger: The logger to use to log things.
        """
        super().__init__(name, logger=logger)
        self._registry = registry
        self._var_values = values
        self._criticalities = objective_criticalities
        # noinspection PyTypeChecker
        self._chain: ChainAgent = None

        # noinspection PyTypeChecker
        self._last_checked_direction: int = DIR_NONE

        self.steps_mult = 1
        self.local_min_already_visited = False
        self.prev_suggestion_out_of_bounds = False

        self.previous_point: typ.Optional[PointAgent] = None
        self.next_point: typ.Optional[PointAgent] = None

        self.is_current = False

        self._neighbors = []

        self._sorted_minima = []
        self._is_current_min_of_chain = False
        self.is_local_minimum = False
        self.is_global_minimum = False
        self.is_extremum = False
        self.already_went_up = False

        self.best_local_minimum = False

        self.create_new_chain_from_me = False

        self._should_sample = sampler is None
        self.sampled_points: typ.List[PointAgent] = []
        self._sample_steps = dt.Vector.zero(*self._registry.variables_names)
        self.finished_sampling = False
        self.sampling_cluster_acted = False
        self.selected_from_sampling = False
        self._sampled = sampler is not None
        self._sampler = sampler

    @property
    def chain(self) -> ChainAgent:
        """The chain that this point belongs to."""
        return self._chain

    @chain.setter
    def chain(self, chain: ChainAgent):
        """Set the chain this point belongs to."""
        self._chain = chain

    @property
    def variable_values(self) -> dt.Vector[float]:
        """Values of all variables when this agent was created."""
        return self._var_values

    @property
    def criticalities(self) -> dt.Vector[float]:
        """Objectives’ criticalities when this agent was created.
        The returned object is a copy of the internal data.
        """
        return self._criticalities

    @property
    def criticality(self) -> float:
        """Criticality of this agent."""
        return max(self.criticalities.values())

    def distance(self, p: PointAgent):
        """Returns the euclidian distance between this point and the specified one.

        :param p: Another point agent.
        :returns: The euclidian distance.
        """
        return self._var_values.distance(p._var_values, self._registry.variables_coefficients)

    def update_neighbors(self, points: typ.Iterable[PointAgent], min_distance: float = 0, max_distance: float = 0.01):
        """Updates the neighbors of this point from the given list of point agents.

        :param points: The point agents to gather the neighbors from.
        :param min_distance: The minimum distance between this point and its potential neighbors.
        :param max_distance: The maximum distance between this point and its potential neighbors.
        """
        self._neighbors = sorted(
            (p for p in points if p is not self),
            key=lambda p: min_distance <= self.distance(p) <= max_distance
        )

    def perceive(self):
        """Updates this point agent."""
        all_points = self.chain.points
        self._is_current_min_of_chain = self.chain.minimum is self

        # Only the current chain minimum should be set to sampler when local search phase just happened
        if self._should_sample:
            if len(self.sampled_points) == 0 and not self._is_current_min_of_chain:
                # Disable current sampler if not the chain minimum and has not yet sampled
                self._should_sample = False
            else:
                self.finished_sampling = len(self.sampled_points) >= 2
                if self.finished_sampling:
                    self._sample_steps = dt.Vector.zero(*self._registry.variables_names)
                    self._should_sample = False

        if self.finished_sampling or self._sampled and self._sampler.finished_sampling:
            self.update_neighbors(all_points)

            if (self._is_current_min_of_chain and not self.is_local_minimum and self._neighbors
                    # TODO all or a certain proportion?
                    and any(self.distance(other) < self.LOCAL_MIN_THRESHOLD for other in self._neighbors)):
                self.log_debug('local min found')
                self.chain.local_min_found = True
                self.is_local_minimum = True
                if self.criticality < self.NULL_THRESHOLD:
                    self.is_global_minimum = True
                    return
                similar_minima = [other_min for other_min in self._registry.minima
                                  if self.distance(other_min) <= self.SAME_POINT_THRESHOLD]
                already_went_up = any(other_min.already_went_up for other_min in similar_minima)
                max_steps_mult = max((other_min.steps_mult for other_min in similar_minima), default=1)
                other_minima = set(self._registry.minima) - set(similar_minima)

                if similar_minima:
                    self.log_debug('local min already visited')
                    self.local_min_already_visited = True
                    # Update all other similar minima
                    for sim_min in similar_minima:
                        sim_min.steps_mult = max_steps_mult
                        if already_went_up:
                            sim_min.already_went_up = True
                    if already_went_up:
                        self.log_debug('already went up')

                self.steps_mult = max_steps_mult
                # Toggle go up mode if this minimum is alone or has already been visited before
                self.chain.go_up_mode = (len(self._registry.minima) == 0
                                         or similar_minima and (not already_went_up or not other_minima))
                self._registry.add_minimum(self)

                if self.chain.go_up_mode:
                    self.log_debug('-> go up slope')

            if self.is_local_minimum and not self.chain.go_up_mode:
                self.update_neighbors(self._registry.minima, min_distance=self.SAME_POINT_THRESHOLD)
                if self._registry.minima:
                    filtered_minima = filter(
                        lambda mini: mini is self or self.distance(mini) > self.SAME_POINT_THRESHOLD,
                        self._registry.minima
                    )
                    self._sorted_minima: typ.List[PointAgent] = sorted(
                        filtered_minima,
                        key=lambda mini: self.chain.current_point.distance(mini)
                    )
                else:
                    self._sorted_minima = []
                if self.chain.is_active and len(self._sorted_minima) > 1 and self is self._sorted_minima[0]:
                    self.best_local_minimum = True

    def decide(self) -> typ.Optional[_suggestions.VariationSuggestion]:
        """Makes this point agent decide what to do.

        :return: A suggestion or None if this agent decided to suggest nothing.
        """
        decision = ''
        from_point: typ.Optional[dt.Vector[float]] = self._var_values
        distances_to_neighbor: typ.Optional[dt.Vector[float]] = None
        check_for_out_of_bounds = False
        search_phase = None
        suggested_directions: typ.Optional[dt.Vector[int]] = None
        suggested_steps: typ.Optional[dt.Vector[float]] = None
        suggested_point: typ.Optional[dt.Vector[float]] = None
        new_chain_next = False
        priority = 0
        custom_data = None

        if not self.chain.is_active:
            return None

        # Detection must be done here as requiered data may not be updated yet in perceive() method
        if (not self._should_sample
                and ((self.finished_sampling or self._sampled and self._sampler.finished_sampling)
                     and (self.sampling_cluster_acted or self._sampled and self._sampler.sampling_cluster_acted)
                     and self._is_current_min_of_chain)):
            # Set current chain minimum as the sampler
            self._should_sample = True

        if self._should_sample:
            decision = f'sample nearby point ({len(self.sampled_points) + 1})'
            search_phase = _suggestions.SearchPhase.LOCAL_SAMPLING
            first_sample = len(self.sampled_points) == 0
            if first_sample:
                if self._registry.last_local_steps.is_zero():
                    # Pick random steps
                    self._sample_steps = self._var_values.map(
                        lambda vname, _: random.random() * self._registry.get_variable_default_step(vname) / 2)
                    # Pick random directions
                    suggested_directions = dt.Vector(**{
                        vname: DIR_INCREASE if utils.randbool() else DIR_DECREASE
                        for vname in self._registry.variables_names
                    })
                else:
                    # Move slightly along the previous directions to sample the first point of the cluster
                    self._sample_steps = self._registry.last_local_steps / 2
                    suggested_directions = self._registry.last_local_directions
            else:
                diffs = (self.sampled_points[0]._var_values - self._var_values) \
                    .map(lambda vname, v: v / self._registry.variables_coefficients[vname])
                crit_diff = self.sampled_points[0].criticality - self.criticality
                # sensitivities[i] = crit_diff / diff[i] for all i
                sensitivities = (diffs ** -1) * crit_diff
                print('sensitivities', sensitivities)  # DEBUG
                max_sensitivity = max(abs(sensitivities).values())
                print('normalized sensitivities', sensitivities / max_sensitivity)  # DEBUG
                if self.world.config.sampling_mode == config.SamplingMode.WEIGHTED:
                    # Weight step using normalized sensitivities
                    print('last steps', self._registry.last_local_steps)
                    print(abs(sensitivities) / max_sensitivity)
                    self._sample_steps = self._registry.last_local_steps @ (abs(sensitivities) / max_sensitivity)
                    suggested_directions = -self._registry.last_local_directions
                elif self.world.config.sampling_mode == config.SamplingMode.N_BESTS:
                    # Take only best half variables
                    nb = math.ceil(len(self._var_values) / 2)
                    best_entries = {k for k, _ in sorted(sensitivities.entries(), key=lambda e: e[1])[:nb]}
                    self._sample_steps = dt.Vector(**{
                        vname: self._registry.last_local_steps[vname] if vname in best_entries else 0
                        for vname, v in dt.Vector.zero(*self._registry.variables_names)
                    })
                    suggested_directions = self._sample_steps.map(lambda _, v: DIR_DECREASE if v else DIR_NONE)
                else:
                    raise ValueError(f'invalid sensitivity mode {self.world.config.sampling_mode}')
                print('sample steps', self._sample_steps)  # DEBUG
                print('directions', suggested_directions)
            suggested_steps = self._sample_steps

        else:
            if self.is_global_minimum:
                return _suggestions.VariationSuggestion(
                    agent=self,
                    search_phase=_suggestions.SearchPhase.LOCAL,
                    decision='global min found -> do nothing',
                    criticality=self.criticality,
                    local_min_found=True,
                    steps=dt.Vector.zero(*self._registry.variables_names),
                    directions=dt.Vector.filled(DIR_NONE, *self._registry.variables_names),
                    next_point=self._var_values,
                )
            elif self.is_extremum and self.chain.go_up_mode:
                raise NotImplementedError('hill climbing not implemented yet')  # TEMP
                # noinspection PyUnreachableCode
                suggestion = self._hill_climb()
                if not suggestion:
                    return None
                decision = suggestion.decision
                suggested_directions = suggestion.directions
                suggested_steps = suggestion.steps
                from_point = suggestion.from_point
                distances_to_neighbor = suggestion.distances_to_neighbor
                suggested_point = suggestion.next_point
                new_chain_next = suggestion.new_chain_next
                search_phase = _suggestions.SearchPhase.HILL_CLIMBING
                priority = suggestion.priority
                custom_data = suggestion.custom_data
            elif not self.is_local_minimum:
                # Detection must be done here as sampling_cluster_acted property
                # might not be updated yet in perceive() method
                if ((self.finished_sampling or self._sampled and self._sampler.finished_sampling)
                        and not self.sampling_cluster_acted
                        and (not self._sampled or not self._sampler.sampling_cluster_acted)):
                    # Only the minimum from the cluster should act
                    suggestion = self._local_search()
                    if not suggestion:
                        return None
                    decision = suggestion.decision
                    suggested_directions = suggestion.directions
                    suggested_steps = suggestion.steps
                    from_point = suggestion.from_point
                    distances_to_neighbor = suggestion.distances_to_neighbor
                    suggested_point = suggestion.next_point
                    search_phase = _suggestions.SearchPhase.LOCAL
                    custom_data = suggestion.custom_data
                    if self._sampled:
                        self._sampler.sampling_cluster_acted = True
                    else:
                        self.sampling_cluster_acted = True
            elif self.best_local_minimum:
                suggestion = self._semi_local_search()
                decision = suggestion.decision
                suggested_directions = suggestion.directions
                suggested_steps = suggestion.steps
                from_point = suggestion.from_point
                distances_to_neighbor = suggestion.distances_to_neighbor
                suggested_point = suggestion.next_point
                new_chain_next = suggestion.new_chain_next
                check_for_out_of_bounds = suggestion.check_for_out_of_bounds
                search_phase = _suggestions.SearchPhase.SEMI_LOCAL
                custom_data = suggestion.custom_data

            if decision:
                self.log_debug('Decision: ' + decision)

        if suggested_steps is not None:
            # Cap jump lengths
            if distances_to_neighbor is not None:
                print(from_point, suggested_steps)  # DEBUG
                suggested_steps = suggested_steps.map(lambda vname, v: min(2 * distances_to_neighbor[vname], v))
                print(suggested_steps)  # DEBUG
            suggested_point = from_point + suggested_steps @ suggested_directions
            if (check_for_out_of_bounds and any(not self._registry.is_within_bounds(vname, value)
                                                for vname, value in suggested_point)):
                self.prev_suggestion_out_of_bounds = True

        if suggested_point is not None:
            return _suggestions.VariationSuggestion(
                agent=self,
                search_phase=search_phase,
                next_point=suggested_point.map(lambda vname, v: self._registry.clamp_value(vname, v)),
                decision=decision,
                criticality=self.criticality,
                local_min_found=self.chain.local_min_found,
                directions=suggested_directions,
                steps=suggested_steps,
                new_chain_next=new_chain_next,
                priority=priority,
                custom_data=custom_data,
            )
        return None

    def _local_search(self) -> typ.Optional[ig.LocalSearchSuggestion]:
        custom_data = {}
        # Select direction from side of triangle with the highest criticality variation
        if self._sampled:
            cluster_points = [self._sampler, *self._sampler.sampled_points]
        else:
            cluster_points = [self, *self.sampled_points]
        distances = {
            (p1, p2): abs(p1.criticality - p2.criticality)
            for p1, p2 in zip(cluster_points, cluster_points[1:] + [cluster_points[0]])
        }
        print(distances)  # DEBUG
        # Extract side with highest criticality variation
        best_side = max(distances.items(), key=lambda e: e[1])[0]
        print(self, best_side)  # DEBUG
        lowest_agent, highest_agent = sorted(best_side, key=lambda p: p.criticality)
        print(self is lowest_agent)  # DEBUG
        # Only lowest agent of the selected side can act
        if lowest_agent is not self:
            return None
        steps = {}
        directions = {}
        for vname in self._registry.variables_names:
            # noinspection PyProtectedMember
            top_point = (highest_agent._var_values[vname], highest_agent.criticality)
            x = utils.get_xc(top_point, intermediate_point=(self._var_values[vname], self.criticality), yc=0)
            steps[vname] = abs(x - self._var_values[vname])
            directions[vname] = DIR_INCREASE if x > self._var_values[vname] else DIR_DECREASE
        # noinspection PyTypeChecker
        print(steps)  # DEBUG
        # noinspection PyTypeChecker,PyProtectedMember
        return ig.LocalSearchSuggestion(
            decision='sampling done -> follow slope',
            directions=dt.Vector(**directions),
            steps=dt.Vector(**steps),
            from_point=self._var_values,
            distances_to_neighbor=abs(self._var_values - highest_agent._var_values),
            custom_data=custom_data,
        )

    def _semi_local_search(self) -> ig.SemiLocalSearchSuggestion:
        self.best_local_minimum = False
        self_value = self.variable_value
        self_crit = self.criticality
        other_min: typ.Optional[PointAgent] = None

        if (self._left_point is not None) != (self._right_point is not None):
            # 1 minimum neighbor -> follow slope
            other_min, suggestion = self._semi_local_search__follow_only_slope()
        else:
            # 2 minima neighbors
            if self._left_crit < self_crit < self._right_crit:
                # left lower, right higher -> follow left slope
                other_min, suggestion = self._semi_local_search__follow_left_slope()
            elif self._left_crit > self_crit > self._right_crit:
                # left higher, right lower -> follow right slope
                other_min, suggestion = self._semi_local_search__follow_right_slope()
            elif self._left_crit < self_crit > self._right_crit:
                # both lower -> follow slope on lowest’s side
                other_min, suggestion = self._semi_local_search__follow_slope_on_lowest_side()
            else:
                # both higher -> go to middle point
                suggestion = self._semi_local_search__go_to_middle()

        decision = suggestion.decision
        direction = suggestion.direction
        suggested_step = suggestion.step
        distance_to_neighbor = suggestion.distances_to_neighbor
        from_value = suggestion.from_value
        suggested_point = suggestion.next_point
        check_for_out_of_bounds = suggestion.check_for_out_of_bounds

        if suggested_step is not None and self.local_min_already_visited:
            prev_min = self.variable.minima[-2]  # -1 is current minimum
            if (abs(self.variable_value - prev_min.variable_value) <= self.SAME_POINT_THRESHOLD
                    and prev_min.prev_suggestion_out_of_bounds):
                decision += ' -> previous was OOB -> cancel jump and go to middle'
                suggested_step = None
                distance_to_neighbor = None
                suggested_point = (self_value + other_min.variable_value) / 2
                check_for_out_of_bounds = False
            else:
                # We came back to an already visited local minimum, go twice as far than previously
                decision += ' and jump twice as far'
                self.steps_mult *= 2
                suggested_step *= self.steps_mult
                distance_to_neighbor = None  # Disable capping

        return ig.SemiLocalSearchSuggestion(
            decision=decision,
            direction=direction,
            step=suggested_step,
            from_value=from_value,
            distance_to_neighbor=distance_to_neighbor,
            next_point=suggested_point,
            check_for_out_of_bounds=check_for_out_of_bounds,
            new_chain_next=True,
        )

    def _semi_local_search__follow_only_slope(self) -> typ.Tuple[PointAgent, ig.SemiLocalSearchSuggestion]:
        # FIXME div by 0 if both points have the same criticality
        if self._left_point:
            top_point = (self._left_value, self._left_crit)
            other_min = self._left_point
        else:
            top_point = (self._right_value, self._right_crit)
            other_min = self._right_point
        if top_point[1] < self.criticality:
            interm_point = top_point
            top_point = (self.variable_value, self.criticality)
            from_value = interm_point[0]
        else:
            interm_point = (self.variable_value, self.criticality)
            from_value = self.variable_value
        x = utils.get_xc(top_point, intermediate_point=interm_point, yc=0)
        direction = DIR_INCREASE if x > self.variable_value else DIR_DECREASE
        return other_min, ig.SemiLocalSearchSuggestion(
            decision='1 minimum neighbor -> follow slope',
            direction=direction,
            step=abs(x - interm_point[0]),
            from_value=from_value,
            distance_to_neighbor=abs(interm_point[0] - top_point[0]),
            check_for_out_of_bounds=True,
        )

    def _semi_local_search__follow_left_slope(self) -> typ.Tuple[PointAgent, ig.SemiLocalSearchSuggestion]:
        x = utils.get_xc(top_point=(self.variable_value, self.criticality),
                         intermediate_point=(self._left_value, self._left_crit), yc=0)
        return self._left_point, ig.SemiLocalSearchSuggestion(
            decision='2 minima neighbors: left lower, right higher -> follow left slope',
            direction=DIR_DECREASE,
            step=abs(x - self._left_value),
            from_value=self._left_value,
            distance_to_neighbor=abs(self.variable_value - self._left_value),
            check_for_out_of_bounds=True,
        )

    def _semi_local_search__follow_right_slope(self) -> typ.Tuple[PointAgent, ig.SemiLocalSearchSuggestion]:
        x = utils.get_xc(top_point=(self.variable_value, self.criticality),
                         intermediate_point=(self._right_value, self._right_crit), yc=0)
        return self._right_point, ig.SemiLocalSearchSuggestion(
            decision='2 minima neighbors: left higher, right lower -> follow right slope',
            direction=DIR_INCREASE,
            step=abs(x - self._right_value),
            from_value=self._right_value,
            distance_to_neighbor=abs(self.variable_value - self._right_value),
            check_for_out_of_bounds=True,
        )

    def _semi_local_search__follow_slope_on_lowest_side(self) -> typ.Tuple[PointAgent, ig.SemiLocalSearchSuggestion]:
        if self._right_crit < self._left_crit:
            from_value = self._right_value
            crit = self._right_crit
            other_min = self._right_point
        else:
            from_value = self._left_value
            crit = self._left_crit
            other_min = self._left_point
        x = utils.get_xc(top_point=(self.variable_value, self.criticality),
                         intermediate_point=(from_value, crit), yc=0)
        direction = DIR_INCREASE if x > self.variable_value else DIR_DECREASE
        return other_min, ig.SemiLocalSearchSuggestion(
            decision='2 minima neighbors: both lower -> follow slope on lowest’s side',
            direction=direction,
            step=abs(x - from_value),
            from_value=from_value,
            distance_to_neighbor=abs(self.variable_value - from_value),
            check_for_out_of_bounds=True,
        )

    def _semi_local_search__go_to_middle(self) -> ig.SemiLocalSearchSuggestion:
        if self._last_checked_direction == DIR_INCREASE and self._right_crit > self.criticality:
            other_value = self._left_value
            self._last_checked_direction = DIR_DECREASE
        elif self._last_checked_direction == DIR_DECREASE and self._left_crit > self.criticality:
            other_value = self._right_value
            self._last_checked_direction = DIR_INCREASE
        elif self.next_point and self.next_point.is_current:
            other_value = self.previous_point.variable_value
        elif self._right_crit < self._left_crit:
            other_value = self._right_value
            self._last_checked_direction = DIR_INCREASE
        else:
            other_value = self._left_value
            self._last_checked_direction = DIR_DECREASE
        suggested_point = (self.variable_value + other_value) / 2
        return ig.SemiLocalSearchSuggestion(
            decision='2 minima neighbors: both higher -> go to middle point',
            next_point=suggested_point
        )

    def _hill_climb(self) -> ig.HillClimbSuggestion:
        lower_extremum = self.chain.lower_extremum
        upper_extremum = self.chain.upper_extremum
        other_extremum = lower_extremum if self is upper_extremum else upper_extremum
        other_extremum_value = other_extremum.variable_value
        other_extremum_crit = other_extremum.criticality
        self_on_bound = self.variable_value in [self.variable.lower_bound, self.variable.upper_bound]
        other_on_bound = other_extremum_value in [self.variable.lower_bound, self.variable.upper_bound]

        if other_extremum_value < self.variable_value:
            prev_value = self._left_value
            prev_crit = self._left_crit
            prev = self._left_point
        else:
            prev_value = self._right_value
            prev_crit = self._right_crit
            prev = self._right_point

        suggestion = None
        if self.criticality < prev_crit and not self_on_bound:
            suggestion = self._hill_climb__new_slope_found_stop_climbing(prev, self_on_bound)
        elif self_on_bound and other_on_bound:
            if self.criticality < other_extremum_crit \
                    or (self.criticality == other_extremum_crit and self.variable_value > other_extremum_value):
                suggestion = self._hill_climbing__both_extrema_on_bounds_stop_climbing(self_on_bound)
        elif (self.criticality < other_extremum_crit
              or (self.criticality == other_extremum_crit and self.variable_value > other_extremum_value)
              or other_on_bound) and not self_on_bound:
            suggestion = self._hill_climbing__climb(prev_value, prev_crit, other_extremum_crit, other_on_bound)

        return suggestion

    def _hill_climb__new_slope_found_stop_climbing(self, prev_point: PointAgent, self_on_bound: bool) \
            -> ig.HillClimbSuggestion:
        if prev_point is self._left_point or self.variable_value == self.variable.lower_bound:
            direction = DIR_INCREASE
        elif prev_point is self._right_point or self.variable_value == self.variable.upper_bound:
            direction = DIR_DECREASE
        else:
            direction = self.variable.last_local_direction or self.world.rng.choice([DIR_DECREASE, DIR_INCREASE])
        self.chain.go_up_mode = False
        self.chain.minimum.already_went_up = True
        new_chain_next = self is self.chain.minimum or self_on_bound
        if not new_chain_next:
            self.create_new_chain_from_me = True
        return ig.HillClimbSuggestion(
            decision='opposite slope found -> stop climbing; create new chain; explore',
            direction=direction,
            step=self.variable.default_step,
            from_value=self.variable_value,
            new_chain_next=new_chain_next,
            priority=2
        )

    def _hill_climbing__both_extrema_on_bounds_stop_climbing(self, self_on_bound: bool) -> ig.HillClimbSuggestion:
        suggested_point = (self.variable.lower_bound + self.variable.upper_bound) / 2
        self.chain.go_up_mode = False
        self.chain.minimum.already_went_up = True
        new_chain_next = self is self.chain.minimum or self_on_bound
        if not new_chain_next:
            self.create_new_chain_from_me = True
        return ig.HillClimbSuggestion(
            decision='both extrema on bounds -> go to middle; create new chain; explore',
            next_point=suggested_point,
            new_chain_next=new_chain_next,
            priority=1
        )

    def _hill_climbing__climb(self, prev_value: float, prev_crit: float, other_extremum_criticality: float,
                              other_on_bound: bool) -> ig.HillClimbSuggestion:
        direction = DIR_DECREASE if self.variable_value < prev_value else DIR_INCREASE
        if (abs(self.variable_value - prev_value) <= self.STUCK_THRESHOLD
                or self.criticality == other_extremum_criticality or other_on_bound):
            decision = 'stuck -> move a bit' if not other_on_bound else 'other on bound -> move a bit'
            suggested_point = self.variable_value + self.variable.default_step * direction
        else:
            decision = 'go up slope'
            suggested_point = utils.get_xc(top_point=(prev_value, prev_crit),
                                           intermediate_point=(self.variable_value, self.criticality),
                                           yc=other_extremum_criticality)
        return ig.HillClimbSuggestion(
            decision=decision,
            next_point=suggested_point,
            priority=0
        )

    def __repr__(self):
        return (
            f'Point{{name={self.name},values={self._var_values},sampled={self._sampled},'
            f'local min={self.is_local_minimum},extremum={self.is_extremum},'
            f'min of chain={self._is_current_min_of_chain},current={self.is_current}}}'
        )
