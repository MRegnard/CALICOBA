from __future__ import annotations

import abc
import dataclasses
import logging
import math
import pathlib
import random
import typing as typ

from . import _normalizers
from .. import utils

DIR_INCREASE = 1
DIR_DECREASE = -1
DIR_NONE = 0


class Agent(abc.ABC):
    def __init__(self, name: str, *, logger: logging.Logger = None):
        self.__name = name
        self.__dead = False
        self._logger = logger
        self._world = None

    @property
    def world(self):
        """The world this agent lives in.

        :rtype: cobopti.CoBOpti
        """
        return self._world

    @world.setter
    def world(self, w):
        """Set the world this agent lives in.

        :type w: cobopti.CoBOpti
        """
        if self._world:
            raise ValueError(f'world already set for agent {self.name}')
        self._world = w

    @property
    def name(self) -> str:
        return self.__name

    def die(self):
        self.__dead = True

    @property
    def dead(self) -> bool:
        return self.__dead

    def log_exception(self, exception):
        if self._logger:
            self._logger.exception(exception)

    def log_error(self, message):
        if self._logger:
            self._logger.error(f'{self._get_logging_name()}: {message}')

    def log_critical(self, message):
        if self._logger:
            self._logger.critical(f'{self._get_logging_name()}: {message}')

    def log_warning(self, message):
        if self._logger:
            self._logger.warning(f'{self._get_logging_name()}: {message}')

    def log_info(self, message):
        if self._logger:
            self._logger.info(f'{self._get_logging_name()}: {message}')

    def log_debug(self, message):
        if self._logger:
            self._logger.debug(f'{self._get_logging_name()}: {message}')

    def _get_logging_name(self):
        return self.name

    def __repr__(self):
        return f'{self.name}'


class ObjectiveAgent(Agent):
    def __init__(self, name: str, inf: float, sup: float):
        super().__init__(name)
        self._criticality = 0
        self._normalizer = _normalizers.BoundNormalizer(inf, sup)
        self._file = None

    @property
    def criticality(self) -> float:
        return self._criticality

    def perceive(self, cycle: int, objective_value: float, *, dump_dir: pathlib.Path = None):
        self._criticality = self._normalizer(objective_value)

        if dump_dir:
            if not self._file:
                self._file = (dump_dir / (self.name + '.csv')).open(mode='w', encoding='UTF-8')
                self._file.write('cycle,raw value,criticality\n')
            self._file.write(f'{cycle},{objective_value},{self._criticality}\n')
            self._file.flush()

    def __del__(self):
        if self._file:
            self._file.close()


class VariableAgent(Agent):
    def __init__(self, name: str, inf: float, sup: float, *, logger: logging.Logger = None):
        super().__init__(name, logger=logger)
        self._inf = inf
        self._sup = sup
        self._max_step_number = 2
        self._default_step = (sup - inf) / 100
        self.last_step = 0
        self.last_direction = DIR_NONE

        self._chains: typ.List[ChainAgent] = []
        self._minima: typ.List[PointAgent] = []
        self._last_point_id = 0
        self._last_chain_id = 0

        self._value = math.nan

    @property
    def inf(self) -> float:
        return self._inf

    @property
    def sup(self) -> float:
        return self._sup

    @property
    def value(self) -> float:
        return self._value

    @property
    def default_step(self) -> float:
        return self._default_step

    @property
    def max_step(self) -> int:
        return self._max_step_number

    @property
    def minima(self) -> typ.Sequence[PointAgent]:
        return self._minima

    def add_minimum(self, point: PointAgent):
        self._minima.append(point)

    def perceive(self, value: float, new_chain: bool, criticalities: typ.Dict[str, float],
                 previous_step: float, previous_direction: float):
        self._value = value
        self.last_step = previous_step
        self.last_direction = previous_direction
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
                if prev_point.create_new_chain_from_me:
                    self._chains[-1].remove_last_point()
                    prev_point.create_new_chain_from_me = False
                    self.__create_new_chain(prev_point)
                self._chains[-1].add_point(new_point)
            else:
                self.__create_new_chain(new_point)

    def __create_new_chain(self, first_point: PointAgent):
        self._chains[-1].is_active = False
        self._chains.append(ChainAgent(f'c{self._last_chain_id}', self, first_point, logger=self._logger))
        self.world.add_agent(self._chains[-1])
        self._last_chain_id += 1

    def __repr__(self):
        return f'{self.name}={self.value}'


class ChainAgent(Agent):
    def __init__(self, name: str, variable_agent: VariableAgent, first_point: PointAgent,
                 *, logger: logging.Logger = None):
        super().__init__(name, logger=logger)
        self._var_agent = variable_agent
        self._points = [first_point]
        self._minimum = first_point
        self._lower_extremum = first_point
        self._upper_extremum = first_point
        self._current_point = first_point
        self.is_active = True
        self.local_min_found = False
        self.go_up_mode = False

    @property
    def points(self) -> typ.List[PointAgent]:
        return list(self._points)

    @property
    def current_point(self):
        return self._current_point

    @property
    def minimum(self) -> PointAgent:
        return self._minimum

    @property
    def lower_extremum(self) -> PointAgent:
        return self._lower_extremum

    @property
    def upper_extremum(self) -> PointAgent:
        return self._upper_extremum

    def add_point(self, p: PointAgent):
        if p in self._points:
            raise ValueError(f'point {p} already in chain {self}')
        self._current_point.is_current = False
        prev_point = self._points[-1]
        prev_point.next_point = p
        p.is_current = True
        p.next_point = None
        p.previous_point = prev_point
        self._points.append(p)

    def remove_last_point(self):
        if len(self._points) == 1:
            raise ValueError(f'cannot remove point from chain {self}')
        self._points.pop()

    def perceive(self):
        if not self.is_active:
            return
        self._detect_minimum()
        self._detect_extrema()

    def _detect_minimum(self):
        self._minimum = min(self._points, key=lambda p: p.criticality)

    def _detect_extrema(self):
        extremum_min = None
        extremum_max = None
        for point in self._points:
            if not extremum_min or point.variable_value < extremum_min.variable_value:
                if extremum_min:
                    extremum_min.is_extremum = False
                point.is_extremum = True
                extremum_min = point
            elif not extremum_max or point.variable_value > extremum_max.variable_value:
                if extremum_max:
                    extremum_max.is_extremum = False
                point.is_extremum = True
                extremum_max = point

    def __repr__(self):
        return f'Chain{{name={self.name},variable={self._var_agent.name},points={self._points}}}'


@dataclasses.dataclass(frozen=True)
class _Suggestion:
    decision: str
    next_point: typ.Optional[float] = None
    direction: typ.Optional[float] = None
    step: typ.Optional[float] = None


@dataclasses.dataclass(frozen=True)
class LocalSearchSuggestion(_Suggestion):
    pass


@dataclasses.dataclass(frozen=True)
class SemiLocalSearchSuggestion(_Suggestion):
    from_value: typ.Optional[float] = None
    new_chain_next: bool = False
    check_for_out_of_bounds: bool = False


@dataclasses.dataclass(frozen=True)
class HillClimbSuggestion(_Suggestion):
    new_chain_next: bool = False


class PointAgent(Agent):
    LOCAL_MIN_THRESHOLD = 1e-4
    STUCK_THRESHOLD = 1e-4
    SAME_POINT_THRESHOLD = 0.01
    NULL_THRESHOLD = 5e-5

    def __init__(self, name: str, variable_agent: VariableAgent, objective_criticalities: typ.Dict[str, float],
                 *, logger: logging.Logger = None):
        super().__init__(name, logger=logger)
        self._var_agent = variable_agent
        self._var_value = variable_agent.value
        self._criticalities = objective_criticalities
        # noinspection PyTypeChecker
        self._chain: ChainAgent = None

        # noinspection PyTypeChecker
        self._suggested_step: float = None
        self._last_direction: int = DIR_NONE
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
        return f'{self._var_agent.name}:{self.name}'

    def set_chain(self, chain: ChainAgent):
        if self._chain:
            raise ValueError(f'point {self} already has a chain')
        self._chain = chain

    def update_neighbors(self, points: typ.Iterable[PointAgent], threshold: float = 0):
        def value_in_list(v, vs):
            return any(abs(v - e) <= threshold for e in vs)

        sorted_points = sorted(points, key=lambda p: p.variable_value)
        # Remove duplicates
        values = set()
        sorted_points_ = []
        for point in sorted_points:
            if not value_in_list(point.variable_value, values) or point is self:  # Keep self in list
                # If another point with the same value as self is in the list, remove it and append self instead
                if (point is self and sorted_points_
                        and abs(sorted_points_[-1].variable_value - self.variable_value) <= threshold):
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
        self._suggested_step = self._var_agent.last_step
        self._last_direction = self._var_agent.last_direction
        all_points = self._chain.points
        self._is_current_min_of_chain = self._chain.minimum is self

        self.update_neighbors(all_points)

        # FIXME dans le cas où il existe plusieurs agents variables,
        #  tous ne vont pas nécessairement converger en même temps
        if (self._is_current_min_of_chain and not self.is_local_minimum and (self._left_point or self._right_point)
                and (not self._left_point
                     or (self._left_point and abs(self._left_value - self.variable_value) < self.LOCAL_MIN_THRESHOLD))
                and (not self._right_point
                     or self._right_point and abs(
                            self._right_value - self.variable_value) < self.LOCAL_MIN_THRESHOLD)):
            self.log_debug('local min found')
            self._chain.local_min_found = True
            self.is_local_minimum = True
            if self.criticality < self.NULL_THRESHOLD:
                self.is_global_minimum = True
            similar_minima = [mini for mini in self._var_agent.minima
                              if abs(mini.variable_value - self.variable_value) <= self.SAME_POINT_THRESHOLD]
            already_went_up = any(mini.already_went_up for mini in similar_minima)
            max_steps_mult = max((mini.steps_mult for mini in similar_minima), default=1)
            other_minima = set(self._var_agent.minima) - set(similar_minima)

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
            self._chain.go_up_mode = (len(self._var_agent.minima) == 0
                                      or similar_minima and (not already_went_up or not other_minima))
            self._var_agent.add_minimum(self)

            if self._chain.go_up_mode:
                self.log_debug('-> go up slope')

        if self.is_local_minimum and not self._chain.go_up_mode:
            self.update_neighbors(self._var_agent.minima, threshold=self.SAME_POINT_THRESHOLD)
            if self._var_agent.minima:
                filtered = filter(lambda mini: mini is self or abs(
                    mini.variable_value - self.variable_value) > self.SAME_POINT_THRESHOLD, self._var_agent.minima)
                self._sorted_minima: typ.List[PointAgent] = sorted(filtered, key=lambda mini: abs(
                    mini.variable_value - self._chain.current_point.variable_value))
            else:
                self._sorted_minima = []
            if self._chain.is_active and len(self._sorted_minima) > 1 and self is self._sorted_minima[0]:
                self.best_local_minimum = True

    def decide(self) -> typ.Optional[typ.Union[VariationSuggestion, GlobalMinimumFound]]:
        if self.is_global_minimum:
            return GlobalMinimumFound()
        if not self._chain.is_active or not self.best_local_minimum:
            return None

        decision = ''
        suggested_direction = DIR_NONE
        suggested_step = None
        from_value = self.variable_value
        suggested_point = None
        new_chain_next = False
        check_for_out_of_bounds = False

        if self.is_extremum and self._chain.go_up_mode:
            suggestion = self._hill_climb()
            decision = suggestion.decision
            suggested_direction = suggestion.direction
            suggested_step = suggestion.step
            suggested_point = suggestion.next_point
            new_chain_next = suggestion.new_chain_next
        elif self._is_current_min_of_chain:
            if not self.is_local_minimum:
                suggestion = self._local_search()
                decision = suggestion.decision
                suggested_direction = suggestion.direction
                suggested_step = suggestion.step
                suggested_point = suggestion.next_point
            elif self.best_local_minimum:
                suggestion = self._semi_local_search()
                decision = suggestion.decision
                suggested_direction = suggestion.direction
                suggested_step = suggestion.step
                from_value = suggestion.from_value
                suggested_point = suggestion.next_point
                new_chain_next = suggestion.new_chain_next
                check_for_out_of_bounds = suggestion.check_for_out_of_bounds

        if decision:
            self.log_debug('Decision: ' + decision)

        if suggested_step is not None:
            # Cap jump length
            # suggested_step = min(self._param_agent.max_step, suggested_step)
            suggested_point = from_value + suggested_step * suggested_direction
            if (check_for_out_of_bounds
                    and (suggested_point < self._var_agent.inf or suggested_point > self._var_agent.sup)):
                self.prev_suggestion_out_of_bounds = True

        if suggested_point is not None:
            return VariationSuggestion(
                agent=self,
                next_point=min(self._var_agent.sup, max(self._var_agent.inf, suggested_point)),
                decision=decision,
                selected_objective='',
                criticality=self.criticality,
                local_min_found=self._chain.local_min_found,
                direction=suggested_direction,
                step=suggested_step,
                new_chain_next=new_chain_next,
            )
        return None

    def _local_search(self) -> LocalSearchSuggestion:
        if self.previous_point is None and self.next_point is None:
            return self._local_search__explore_from_first_point()
        elif self.variable_value in (self._var_agent.inf, self._var_agent.sup):
            return self._local_search__point_on_bound_go_to_middle()
        elif (self._left_point is not None) != (self._right_point is not None):
            return self._local_search__follow_slope()
        else:
            return self._local_search__go_to_middle()

    def _local_search__explore_from_first_point(self) -> LocalSearchSuggestion:
        decision = 'first point in chain -> explore'
        if self.variable_value == self._var_agent.inf:
            direction = DIR_INCREASE
        elif self.variable_value == self._var_agent.sup:
            direction = DIR_DECREASE
        else:
            direction = self._last_direction or random.choice([DIR_DECREASE, DIR_INCREASE])
        return LocalSearchSuggestion(decision=decision, direction=direction, step=self._suggested_step)

    def _local_search__point_on_bound_go_to_middle(self) -> LocalSearchSuggestion:
        decision = 'point on bound -> go to middle'
        if self.variable_value == self._var_agent.inf:
            other_value = self._right_value
        else:
            other_value = self._left_value
        suggested_point = (self.variable_value + other_value) / 2
        return LocalSearchSuggestion(decision=decision, next_point=suggested_point)

    def _local_search__follow_slope(self) -> LocalSearchSuggestion:
        decision = '1 neighbor -> follow slope'
        if self._left_point:
            top_point = (self._left_value, self._left_crit)
        else:
            top_point = (self._right_value, self._right_crit)
        x = utils.get_xc(top_point, intermediate_point=(self.variable_value, self.criticality), yc=0)
        direction = DIR_INCREASE if x > self.variable_value else DIR_DECREASE
        if abs(x - self.variable_value) < self.STUCK_THRESHOLD:
            suggested_step = self._suggested_step * direction
        else:
            # TODO go back to behavior before AVT
            if self._last_direction == direction:
                suggested_step = self._suggested_step * 2
            else:
                suggested_step = self._suggested_step / 3
            # if self.parameter_name == 'p2':  # TEST
            #     suggested_steps_number /= 2
        return LocalSearchSuggestion(decision=decision, direction=direction, step=suggested_step)

    def _local_search__go_to_middle(self) -> LocalSearchSuggestion:
        decision = '2 neighbors -> go to middle point'
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
        return LocalSearchSuggestion(decision=decision, next_point=suggested_point)

    def _semi_local_search(self) -> SemiLocalSearchSuggestion:
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
        from_value = suggestion.from_value
        suggested_point = suggestion.next_point
        check_for_out_of_bounds = suggestion.check_for_out_of_bounds

        if suggested_step is not None and self.local_min_already_visited:
            prev_min = self._var_agent.minima[-2]  # -1 is current minimum
            if (abs(self.variable_value - prev_min.variable_value) <= self.SAME_POINT_THRESHOLD
                    and prev_min.prev_suggestion_out_of_bounds):
                decision += ' -> previous was OOB -> cancel jump and go to middle'
                suggested_step = None
                suggested_point = (self_value + other_min.variable_value) / 2
                check_for_out_of_bounds = False
            else:
                # We came back to an already visited local minimum, go twice as far than previously
                decision += ' and jump twice as far'
                self.steps_mult *= 2
                suggested_step *= self.steps_mult

        new_chain_next = True

        return SemiLocalSearchSuggestion(
            decision=decision,
            direction=direction,
            step=suggested_step,
            from_value=from_value,
            next_point=suggested_point,
            check_for_out_of_bounds=check_for_out_of_bounds,
            new_chain_next=new_chain_next,
        )

    def _semi_local_search__follow_only_slope(self) -> typ.Tuple[PointAgent, SemiLocalSearchSuggestion]:
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
        suggested_step = abs(x - interm_point[0])
        return other_min, SemiLocalSearchSuggestion(
            decision='1 minimum neighbor -> follow slope',
            direction=direction,
            step=suggested_step,
            from_value=from_value,
            check_for_out_of_bounds=True
        )

    def _semi_local_search__follow_left_slope(self) -> typ.Tuple[PointAgent, SemiLocalSearchSuggestion]:
        x = utils.get_xc(top_point=(self.variable_value, self.criticality),
                         intermediate_point=(self._left_value, self._left_crit), yc=0)
        suggested_step = abs(x - self._left_value)
        return self._left_point, SemiLocalSearchSuggestion(
            decision='2 minima neighbors: left lower, right higher -> follow left slope',
            direction=DIR_DECREASE,
            step=suggested_step,
            from_value=self._left_value,
            check_for_out_of_bounds=True
        )

    def _semi_local_search__follow_right_slope(self) -> typ.Tuple[PointAgent, SemiLocalSearchSuggestion]:
        x = utils.get_xc(top_point=(self.variable_value, self.criticality),
                         intermediate_point=(self._right_value, self._right_crit), yc=0)
        suggested_step = abs(x - self._right_value)
        return self._right_point, SemiLocalSearchSuggestion(
            decision='2 minima neighbors: left higher, right lower -> follow right slope',
            direction=DIR_INCREASE,
            step=suggested_step,
            from_value=self._right_value,
            check_for_out_of_bounds=True
        )

    def _semi_local_search__follow_slope_on_lowest_side(self) -> typ.Tuple[PointAgent, SemiLocalSearchSuggestion]:
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
        suggested_step = abs(x - from_value)
        return other_min, SemiLocalSearchSuggestion(
            decision='2 minima neighbors: both lower -> follow slope on lowest’s side',
            direction=direction,
            step=suggested_step,
            from_value=self._right_value,
            check_for_out_of_bounds=True
        )

    def _semi_local_search__go_to_middle(self) -> SemiLocalSearchSuggestion:
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
        return SemiLocalSearchSuggestion(
            decision='2 minima neighbors: both higher -> go to middle point',
            next_point=suggested_point
        )

    def _hill_climb(self):
        suggestion = None

        lower_extremum = self._chain.lower_extremum
        upper_extremum = self._chain.upper_extremum
        other_extremum = lower_extremum if self is upper_extremum else upper_extremum
        other_extremum_value = other_extremum.variable_value
        other_extremum_crit = other_extremum.criticality
        self_on_bound = self.variable_value in [self._var_agent.inf, self._var_agent.sup]
        other_on_bound = other_extremum_value in [self._var_agent.inf, self._var_agent.sup]

        if other_extremum_value < self.variable_value:
            prev_value = self._left_value
            prev_crit = self._left_crit
            prev = self._left_point
        else:
            prev_value = self._right_value
            prev_crit = self._right_crit
            prev = self._right_point

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
            -> HillClimbSuggestion:
        if prev_point is self._left_point or self._var_value == self._var_agent.inf:
            direction = DIR_INCREASE
        elif prev_point is self._right_point or self._var_value == self._var_agent.sup:
            direction = DIR_DECREASE
        else:
            direction = self._last_direction or random.choice([DIR_DECREASE, DIR_INCREASE])
        self._chain.go_up_mode = False
        self._chain.minimum.already_went_up = True
        new_chain_next = self is self._chain.minimum or self_on_bound
        if not new_chain_next:
            self.create_new_chain_from_me = True
        return HillClimbSuggestion(
            decision='opposite slope found -> stop climbing; create new chain; explore',
            direction=direction,
            step=self._suggested_step,
            new_chain_next=new_chain_next,
        )

    def _hill_climbing__both_extrema_on_bounds_stop_climbing(self, self_on_bound: bool) -> HillClimbSuggestion:
        suggested_point = (self._var_agent.inf + self._var_agent.sup) / 2
        self._chain.go_up_mode = False
        self._chain.minimum.already_went_up = True
        new_chain_next = self is self._chain.minimum or self_on_bound
        if not new_chain_next:
            self.create_new_chain_from_me = True
        return HillClimbSuggestion(
            decision='both extrema on bounds -> go to middle; create new chain; explore',
            next_point=suggested_point,
            new_chain_next=new_chain_next,
        )

    def _hill_climbing__climb(self, prev_value: float, prev_crit: float, other_extremum_criticality: float,
                              other_on_bound: bool):
        direction = DIR_DECREASE if self.variable_value < prev_value else DIR_INCREASE
        if (abs(self.variable_value - prev_value) <= self.STUCK_THRESHOLD
                or self.criticality == other_extremum_criticality or other_on_bound):
            decision = 'stuck -> move a bit' if not other_on_bound else 'other on bound -> move a bit'
            suggested_point = self.variable_value + self._suggested_step * direction
        else:
            decision = 'go up slope'
            suggested_point = utils.get_xc(top_point=(prev_value, prev_crit),
                                           intermediate_point=(self.variable_value, self.criticality),
                                           yc=other_extremum_criticality)
        return HillClimbSuggestion(
            decision=decision,
            next_point=suggested_point,
        )

    @property
    def variable_name(self) -> str:
        return self._var_agent.name

    @property
    def variable_value(self) -> float:
        return self._var_value

    @property
    def criticalities(self) -> typ.Dict[str, float]:
        return dict(self._criticalities)

    @property
    def criticality(self) -> float:
        return max(self._criticalities.values())

    def __repr__(self):
        return (
            f'Point{{{self.name}={self.variable_value},minimum={self.is_local_minimum},extremum={self.is_extremum},'
            f'min of chain={self._is_current_min_of_chain},current={self.is_current}}}'
        )


@dataclasses.dataclass(frozen=True)
class VariationSuggestion:
    agent: PointAgent
    next_point: float
    decision: str
    selected_objective: str
    criticality: float
    local_min_found: bool
    new_chain_next: bool = False
    step: float = None
    direction: int = None


class GlobalMinimumFound:
    pass


class ObjectiveFunction(abc.ABC):
    def __init__(self, *outputs_names: str):
        self._outputs_names = outputs_names

    @property
    def outputs_names(self) -> typ.Tuple[str]:
        return self._outputs_names

    @abc.abstractmethod
    def __call__(self, **outputs_values: float) -> float:
        pass
