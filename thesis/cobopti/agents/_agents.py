from __future__ import annotations

import abc
import logging
import math
import typing as typ

from . import _normalizers, _internal_classes as ic, _suggestions
from .. import utils, config

DIR_INCREASE = 1
DIR_DECREASE = -1
DIR_NONE = 0


class Agent(abc.ABC):
    """Base class for agents."""

    def __init__(self, name: str, *, logger: logging.Logger = None):
        self.__name = name
        self.__dead = False
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
        return self.__name

    def die(self):
        """Flags this agent for deletion at the end of the current cycle."""
        self.__dead = True

    @property
    def dead(self) -> bool:
        """Whether this agent has been removed from the environment."""
        return self.__dead

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


class VariableAgent(Agent):
    """A variable agent represents a model variable to optimize."""

    def __init__(self, metadata: config.Variable, *, logger: logging.Logger = None):
        """Creates a variable agent.

        :param metadata: Variable’s metadata.
        :param logger: The logger instance to use to log things.
        """
        super().__init__(metadata.name, logger=logger)
        self._lower_bound = metadata.lower_bound
        self._upper_bound = metadata.upper_bound
        self._max_step_number = 2
        self._default_step = (self._upper_bound - self._lower_bound) / 100
        self.last_local_step = 0
        self.last_local_direction = DIR_NONE
        self.last_semi_local_step = 0
        self.last_semi_local_direction = DIR_NONE

        self._chains: typ.List[ChainAgent] = []
        self._minima: typ.List[PointAgent] = []
        self._last_point_id = 0
        self._last_chain_id = 0

        self._value = math.nan

    @property
    def lower_bound(self) -> float:
        """The variable’s lower bound."""
        return self._lower_bound

    @property
    def upper_bound(self) -> float:
        """The variable’s upper bound."""
        return self._upper_bound

    @property
    def value(self) -> float:
        """The variable’s current value."""
        return self._value

    @property
    def default_step(self) -> float:
        """The default variation step."""
        return self._default_step

    @property
    def max_step(self) -> int:
        """The maximum variation step."""
        return self._max_step_number

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

    def perceive(self, value: float, new_chain: bool, criticalities: typ.Dict[str, float],
                 previous_step: float, previous_direction: float, previous_search_phase: _suggestions.SearchPhase):
        """Updates this variable agent.

        :param value: The variable’s new value.
        :param new_chain: Whether to create a new chain agent.
        :param criticalities: The criticalities of each objective.
        :param previous_step: The previous variation amount.
        :param previous_direction: The previous variation direction.
        :param previous_search_phase: The previous search phase.
        """
        self._value = value
        if previous_search_phase != _suggestions.SearchPhase.SEMI_LOCAL:
            self.last_local_step = previous_step
            self.last_local_direction = previous_direction
        else:
            self.last_local_step = self.default_step
            self.last_local_direction = previous_direction
            self.last_semi_local_step = previous_step
            self.last_semi_local_direction = previous_direction
        prev_point = self._chains[-1].current_point if self._chains else None

        if not prev_point or prev_point.variable_value != value or prev_point.criticalities != criticalities:
            new_point = PointAgent(
                f'point_{self._last_point_id}',
                self,
                criticalities,
                logger=self._logger
            )
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
                        p.criticalities,
                        value=p.variable_value,
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
        return f'VariableAgent{{{self.name},value={self.value},lb={self.lower_bound},ub={self.upper_bound}}}'


class ChainAgent(Agent):
    """A chain agent manages a set of point agents and is managed by a variable agent."""

    def __init__(self, name: str, variable_agent: VariableAgent, first_point: PointAgent,
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
        self._current_point = None  # Set by add_point
        self.add_point(first_point)
        self.is_active = True
        self.local_min_found = False
        self.go_up_mode = False

    @property
    def variable(self) -> VariableAgent:
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
        self._detect_extrema()

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
    """A point agent represents the value of a variable at a given time along with the states of all objectives."""
    LOCAL_MIN_THRESHOLD = 1e-4
    STUCK_THRESHOLD = 1e-4
    SAME_POINT_THRESHOLD = 0.01
    NULL_THRESHOLD = 5e-5

    def __init__(self, name: str, variable_agent: VariableAgent, objective_criticalities: typ.Dict[str, float],
                 *, value: float = None, logger: logging.Logger = None):
        """Creates a point agent.

        :param name: Point’s name.
        :param variable_agent: Variable agent this point is be associated with.
         Used to gather the current variable value.
        :param objective_criticalities: The current objectives criticalities.
        :param value: If specified, will be used as the variable value instead of the variable agent’s.
        :param logger: The logger to use to log things.
        """
        super().__init__(name, logger=logger)
        self._var_agent = variable_agent
        self._var_value = variable_agent.value if value is None else value
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

        self._left_point = None
        self._left_value = None
        self._left_crit = None

        self._right_point = None
        self._right_value = None
        self._right_crit = None

        self._sorted_minima = []
        self._is_current_min_of_chain = False
        self.is_local_minimum = False
        self.is_global_minimum = False
        self.is_extremum = False
        self.already_went_up = False

        self.best_local_minimum = False

        self.create_new_chain_from_me = False

    def _get_logging_name(self):
        return f'{self.variable.name}:{self.name}'

    @property
    def chain(self) -> ChainAgent:
        """The chain that this point belongs to."""
        return self._chain

    @chain.setter
    def chain(self, chain: ChainAgent):
        """Set the chain this point belongs to."""
        self._chain = chain

    @property
    def variable(self) -> VariableAgent:
        """The variable agent this chain is managed by."""
        return self._var_agent

    @property
    def variable_name(self) -> str:
        """Name of the associated variable."""
        return self.variable.name

    @property
    def variable_value(self) -> float:
        """Value of the associated variable when this agent was created."""
        return self._var_value

    @property
    def criticalities(self) -> typ.Dict[str, float]:
        """Objectives’ criticalities when this agent was created.
        The returned object is a copy of the internal data.
        """
        return dict(self._criticalities)

    @property
    def criticality(self) -> float:
        """Criticality of this agent."""
        return max(self.criticalities.values())

    def update_neighbors(self, points: typ.Iterable[PointAgent], min_distance: float = 0):
        """Updates the neighbors of this point from the given list of point agents.

        :param points: The point agents to gather the neighbors from.
        :param min_distance: The minimum distance between this point and its potential neighbors.
        """

        def value_in_list(v, vs):
            return any(abs(v - e) <= min_distance for e in vs)

        sorted_points = sorted(points, key=lambda p: p.variable_value)
        # Remove duplicates
        values = set()
        sorted_points_ = []
        for point in sorted_points:
            if not value_in_list(point.variable_value, values) or point is self:  # Keep self in list
                # If another point with the same value as self is in the list, remove it and append self instead
                if (point is self and sorted_points_
                        and abs(sorted_points_[-1].variable_value - self.variable_value) <= min_distance):
                    del sorted_points_[-1]
                sorted_points_.append(point)
                values.add(point.variable_value)
        sorted_points = sorted_points_
        i = sorted_points.index(self)
        if i > 0:
            self._left_point = sorted_points[i - 1]
            self._left_value = self._left_point.variable_value
            self._left_crit = self._left_point.criticality
        else:
            self._left_point = None
            self._left_value = None
            self._left_crit = None
        if i < len(sorted_points) - 1:
            self._right_point = sorted_points[i + 1]
            self._right_value = self._right_point.variable_value
            self._right_crit = self._right_point.criticality
        else:
            self._right_point = None
            self._right_value = None
            self._right_crit = None

    def perceive(self):
        """Updates this point agent."""
        all_points = self.chain.points
        self._is_current_min_of_chain = self.chain.minimum is self

        self.update_neighbors(all_points)

        # TODO remonter à un niveau plus global pour vérifier sur l’ensemble des dimensions en même temps,
        #  pas une par une (distance euclidienne entre points)
        if (self._is_current_min_of_chain and not self.is_local_minimum and (self._left_point or self._right_point)
                and (not self._left_point
                     or (self._left_point and abs(self._left_value - self.variable_value) < self.LOCAL_MIN_THRESHOLD))
                and (not self._right_point
                     or self._right_point and abs(
                            self._right_value - self.variable_value) < self.LOCAL_MIN_THRESHOLD)):
            self.log_debug('local min found')
            self.chain.local_min_found = True
            self.is_local_minimum = True
            if self.criticality < self.NULL_THRESHOLD:
                self.is_global_minimum = True
                return
            similar_minima = [other_min for other_min in self.variable.minima
                              if abs(other_min.variable_value - self.variable_value) <= self.SAME_POINT_THRESHOLD]
            already_went_up = any(other_min.already_went_up for other_min in similar_minima)
            max_steps_mult = max((other_min.steps_mult for other_min in similar_minima), default=1)
            other_minima = set(self.variable.minima) - set(similar_minima)

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
            self.chain.go_up_mode = (len(self.variable.minima) == 0
                                     or similar_minima and (not already_went_up or not other_minima))
            self.variable.add_minimum(self)

            if self.chain.go_up_mode:
                self.log_debug('-> go up slope')

        if self.is_local_minimum and not self.chain.go_up_mode:
            self.update_neighbors(self.variable.minima, min_distance=self.SAME_POINT_THRESHOLD)
            if self.variable.minima:
                filtered = filter(lambda mini: mini is self or abs(
                    mini.variable_value - self.variable_value) > self.SAME_POINT_THRESHOLD, self.variable.minima)
                self._sorted_minima: typ.List[PointAgent] = sorted(filtered, key=lambda mini: abs(
                    mini.variable_value - self.chain.current_point.variable_value))
            else:
                self._sorted_minima = []
            if self.chain.is_active and len(self._sorted_minima) > 1 and self is self._sorted_minima[0]:
                self.best_local_minimum = True

    def decide(self) -> typ.Optional[_suggestions.VariationSuggestion]:
        """Makes this point agent decide what to do.

        :return: A suggestion or None if this agent decided to suggest nothing.
        """
        if self.is_global_minimum:
            return _suggestions.VariationSuggestion(
                agent=self,
                search_phase=_suggestions.SearchPhase.LOCAL,
                decision='global min found -> do nothing',
                criticality=self.criticality,
                local_min_found=True,
                step=0,
                direction=DIR_NONE,
                next_point=self.variable_value,
            )
        if not self.chain.is_active and not self.best_local_minimum:
            return None

        decision = ''
        search_phase = None
        suggested_direction = DIR_NONE
        suggested_step = None
        from_value = self.variable_value
        distance_to_neighbor = None
        suggested_point = None
        new_chain_next = False
        check_for_out_of_bounds = False
        priority = 0

        if self.is_extremum and self.chain.go_up_mode:
            suggestion = self._hill_climb()
            if not suggestion:
                return None
            decision = suggestion.decision
            suggested_direction = suggestion.direction
            suggested_step = suggestion.step
            from_value = suggestion.from_value
            distance_to_neighbor = suggestion.distance_to_neighbor
            suggested_point = suggestion.next_point
            new_chain_next = suggestion.new_chain_next
            search_phase = _suggestions.SearchPhase.HILL_CLIMBING
            priority = suggestion.priority
        elif self._is_current_min_of_chain:
            if not self.is_local_minimum:
                suggestion = self._local_search()
                decision = suggestion.decision
                suggested_direction = suggestion.direction
                suggested_step = suggestion.step
                from_value = suggestion.from_value
                distance_to_neighbor = suggestion.distance_to_neighbor
                suggested_point = suggestion.next_point
                search_phase = _suggestions.SearchPhase.LOCAL
            elif self.best_local_minimum:
                suggestion = self._semi_local_search()
                decision = suggestion.decision
                suggested_direction = suggestion.direction
                suggested_step = suggestion.step
                from_value = suggestion.from_value
                distance_to_neighbor = suggestion.distance_to_neighbor
                suggested_point = suggestion.next_point
                new_chain_next = suggestion.new_chain_next
                check_for_out_of_bounds = suggestion.check_for_out_of_bounds
                search_phase = _suggestions.SearchPhase.SEMI_LOCAL

        if decision:
            self.log_debug('Decision: ' + decision)

        if suggested_step is not None:
            # Cap jump length
            if from_value is not None and distance_to_neighbor is not None:
                suggested_step = min(2 * distance_to_neighbor, suggested_step)
            suggested_point = from_value + suggested_step * suggested_direction
            if (check_for_out_of_bounds
                    and (suggested_point < self.variable.lower_bound
                         or suggested_point > self.variable.upper_bound)):
                self.prev_suggestion_out_of_bounds = True

        if suggested_point is not None:
            return _suggestions.VariationSuggestion(
                agent=self,
                search_phase=search_phase,
                next_point=min(self.variable.upper_bound, max(self.variable.lower_bound, suggested_point)),
                decision=decision,
                criticality=self.criticality,
                local_min_found=self.chain.local_min_found,
                direction=suggested_direction,
                step=suggested_step,
                new_chain_next=new_chain_next,
                priority=priority,
            )
        return None

    def _local_search(self) -> ic.LocalSearchSuggestion:
        if self.previous_point is None and self.next_point is None:
            return self._local_search__explore_from_first_point()
        elif self.variable_value in (self.variable.lower_bound, self.variable.upper_bound):
            return self._local_search__go_to_middle_from_bound()
        elif (self._left_point is not None) != (self._right_point is not None):
            return self._local_search__follow_slope()
        else:
            return self._local_search__go_to_middle()

    def _local_search__explore_from_first_point(self) -> ic.LocalSearchSuggestion:
        if self.variable_value == self.variable.lower_bound:
            direction = DIR_INCREASE
        elif self.variable_value == self.variable.upper_bound:
            direction = DIR_DECREASE
        else:
            direction = self.variable.last_local_direction or self.world.rng.choice([DIR_DECREASE, DIR_INCREASE])
        return ic.LocalSearchSuggestion(
            decision='first point in chain -> explore',
            direction=direction,
            step=self.variable.last_local_step,
            from_value=self.variable_value,
        )

    def _local_search__go_to_middle_from_bound(self) -> ic.LocalSearchSuggestion:
        if self.variable_value == self.variable.lower_bound:
            other_value = self._right_value
        else:
            other_value = self._left_value
        suggested_point = (self.variable_value + other_value) / 2
        return ic.LocalSearchSuggestion(
            decision='point on bound -> go to middle',
            next_point=suggested_point,
        )

    def _local_search__follow_slope(self) -> ic.LocalSearchSuggestion:
        if self._left_point:
            top_point = (self._left_value, self._left_crit)
        else:
            top_point = (self._right_value, self._right_crit)
        x = utils.get_xc(top_point, intermediate_point=(self.variable_value, self.criticality), yc=0)
        direction = DIR_INCREASE if x > self.variable_value else DIR_DECREASE
        return ic.LocalSearchSuggestion(
            decision='1 neighbor -> follow slope',
            direction=direction,
            step=abs(x - self.variable_value),
            from_value=self.variable_value,
            distance_to_neighbor=abs(self.variable_value - top_point[0]),
        )

    def _local_search__go_to_middle(self) -> ic.LocalSearchSuggestion:
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
        return ic.LocalSearchSuggestion(
            decision='2 neighbors -> go to middle point',
            next_point=suggested_point,
        )

    def _semi_local_search(self) -> ic.SemiLocalSearchSuggestion:
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
        distance_to_neighbor = suggestion.distance_to_neighbor
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

        return ic.SemiLocalSearchSuggestion(
            decision=decision,
            direction=direction,
            step=suggested_step,
            from_value=from_value,
            distance_to_neighbor=distance_to_neighbor,
            next_point=suggested_point,
            check_for_out_of_bounds=check_for_out_of_bounds,
            new_chain_next=True,
        )

    def _semi_local_search__follow_only_slope(self) -> typ.Tuple[PointAgent, ic.SemiLocalSearchSuggestion]:
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
        return other_min, ic.SemiLocalSearchSuggestion(
            decision='1 minimum neighbor -> follow slope',
            direction=direction,
            step=abs(x - interm_point[0]),
            from_value=from_value,
            distance_to_neighbor=abs(interm_point[0] - top_point[0]),
            check_for_out_of_bounds=True,
        )

    def _semi_local_search__follow_left_slope(self) -> typ.Tuple[PointAgent, ic.SemiLocalSearchSuggestion]:
        x = utils.get_xc(top_point=(self.variable_value, self.criticality),
                         intermediate_point=(self._left_value, self._left_crit), yc=0)
        return self._left_point, ic.SemiLocalSearchSuggestion(
            decision='2 minima neighbors: left lower, right higher -> follow left slope',
            direction=DIR_DECREASE,
            step=abs(x - self._left_value),
            from_value=self._left_value,
            distance_to_neighbor=abs(self.variable_value - self._left_value),
            check_for_out_of_bounds=True,
        )

    def _semi_local_search__follow_right_slope(self) -> typ.Tuple[PointAgent, ic.SemiLocalSearchSuggestion]:
        x = utils.get_xc(top_point=(self.variable_value, self.criticality),
                         intermediate_point=(self._right_value, self._right_crit), yc=0)
        return self._right_point, ic.SemiLocalSearchSuggestion(
            decision='2 minima neighbors: left higher, right lower -> follow right slope',
            direction=DIR_INCREASE,
            step=abs(x - self._right_value),
            from_value=self._right_value,
            distance_to_neighbor=abs(self.variable_value - self._right_value),
            check_for_out_of_bounds=True,
        )

    def _semi_local_search__follow_slope_on_lowest_side(self) -> typ.Tuple[PointAgent, ic.SemiLocalSearchSuggestion]:
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
        return other_min, ic.SemiLocalSearchSuggestion(
            decision='2 minima neighbors: both lower -> follow slope on lowest’s side',
            direction=direction,
            step=abs(x - from_value),
            from_value=from_value,
            distance_to_neighbor=abs(self.variable_value - from_value),
            check_for_out_of_bounds=True,
        )

    def _semi_local_search__go_to_middle(self) -> ic.SemiLocalSearchSuggestion:
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
        return ic.SemiLocalSearchSuggestion(
            decision='2 minima neighbors: both higher -> go to middle point',
            next_point=suggested_point
        )

    def _hill_climb(self) -> ic.HillClimbSuggestion:
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
            -> ic.HillClimbSuggestion:
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
        return ic.HillClimbSuggestion(
            decision='opposite slope found -> stop climbing; create new chain; explore',
            direction=direction,
            step=self.variable.default_step,
            from_value=self.variable_value,
            new_chain_next=new_chain_next,
            priority=2
        )

    def _hill_climbing__both_extrema_on_bounds_stop_climbing(self, self_on_bound: bool) -> ic.HillClimbSuggestion:
        suggested_point = (self.variable.lower_bound + self.variable.upper_bound) / 2
        self.chain.go_up_mode = False
        self.chain.minimum.already_went_up = True
        new_chain_next = self is self.chain.minimum or self_on_bound
        if not new_chain_next:
            self.create_new_chain_from_me = True
        return ic.HillClimbSuggestion(
            decision='both extrema on bounds -> go to middle; create new chain; explore',
            next_point=suggested_point,
            new_chain_next=new_chain_next,
            priority=1
        )

    def _hill_climbing__climb(self, prev_value: float, prev_crit: float, other_extremum_criticality: float,
                              other_on_bound: bool) -> ic.HillClimbSuggestion:
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
        return ic.HillClimbSuggestion(
            decision=decision,
            next_point=suggested_point,
            priority=0
        )

    def __repr__(self):
        return (
            f'Point{{{self.name}={self.variable_value},local minimum={self.is_local_minimum},'
            f'extremum={self.is_extremum},min of chain={self._is_current_min_of_chain},current={self.is_current}}}'
        )

    @staticmethod
    def _get_variation_step(last_direction: float, direction: float, last_step: float) -> float:
        """Returns the suggested variation amount based on the selected and previous variation directions.

        :param last_direction: The previous variation direction.
        :param direction: The selected variation direction.
        :param last_step: The previous variation amount.
        :return: The suggested variation amount.
        """
        if last_direction == direction:
            return last_step * 2
        else:
            return last_step / 3
