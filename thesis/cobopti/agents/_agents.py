from __future__ import annotations

import abc
import dataclasses
import logging
import math
import pathlib
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


class ParameterAgent(Agent):
    def __init__(self, name: str, inf: float, sup: float, *, logger: logging.Logger = None):
        super().__init__(name, logger=logger)
        self._inf = inf
        self._sup = sup
        self._max_step_number = 2
        self._init_step = (sup - inf) / 100

        self._chains: typ.List[PointAgent] = []
        self._minima: typ.List[PointAgent] = []
        self._last_point_id = 0

        self._value = math.nan

        self.local_min_found = False

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
    def start_init_step(self) -> float:
        return self._init_step

    @property
    def step_max(self) -> int:
        return self._max_step_number

    @property
    def minima(self) -> typ.Sequence[PointAgent]:
        return self._minima

    def add_minimum(self, point: PointAgent):
        self._minima.append(point)

    def perceive(self, value: float, new_chain: bool, criticalities: typ.Dict[str, float]) \
            -> PointAgent:
        self._value = value
        prev_point = self._chains[-1] if self._chains else None

        if not prev_point or prev_point.parameter_value != value or prev_point.objective_criticalities != criticalities:
            new_point = PointAgent(
                f'point_{self._last_point_id}',
                self,
                prev_point if not new_chain else None,
                criticalities,
                logger=self._logger
            )
            self._last_point_id += 1
            if self._chains and not new_chain:
                prev_point.next_point = new_point
                if prev_point.create_new_chain_from_me:
                    self._chains[-1] = prev_point.previous_point
                    prev_point.previous_point.next_point = None
                    prev_point.previous_point = None
                    prev_point.create_new_chain_from_me = False
                    self._chains.append(new_point)
                else:
                    self._chains[-1] = new_point
            else:
                self._chains.append(new_point)
            return new_point

        return prev_point

    def __repr__(self):
        return f'{self.name}={self.value}'


class PointAgent(Agent):
    LOCAL_MIN_THRESHOLD = 1e-4
    STUCK_THRESHOLD = 1e-4
    SAME_POINT_THRESHOLD = 0.01
    NULL_THRESHOLD = 5e-5

    def __init__(self, name: str, parameter_agent: ParameterAgent, previous_point: typ.Optional[PointAgent],
                 objective_criticalities: typ.Dict[str, float], *, logger: logging.Logger = None):
        super().__init__(name, logger=logger)
        self._param_agent = parameter_agent
        self._param_value = parameter_agent.value

        self._criticalities = objective_criticalities

        # noinspection PyTypeChecker
        self._step: float = None
        self._last_direction: int = DIR_NONE
        self._last_checked_direction: int = DIR_NONE

        self.steps_mult = 1
        self.local_min_already_visited = False
        self.prev_suggestion_out_of_bounds = False

        self.previous_point = previous_point
        self.next_point: typ.Optional[PointAgent] = None

        self._is_current = False
        self._current_point = None
        self._is_current_in_chain = False

        self._left_point = None
        self._left_value = None
        self._left_crit = None

        self._right_point = None
        self._right_value = None
        self._right_crit = None

        self._sorted_minima = []
        self._min_of_chain: typ.Optional[PointAgent] = None
        self._is_current_min_of_chain = False
        self.is_local_minimum = False
        self.is_global_minimum = False
        self.is_extremum = False
        self.go_up_mode = False
        self.already_went_up = False
        self.first_point = False  # Is this point the first when going up the slope?

        self.best_local_minimum = False

        self.create_new_chain_from_me = False

        self._all_points: typ.List[PointAgent] = []

    def _get_logging_name(self):
        return f'{self._param_agent.name}:{self.name}'

    def update_neighbors(self, points: typ.Iterable[PointAgent], threshold: float = 0):
        def value_in_list(v, vs):
            return any(abs(v - e) <= threshold for e in vs)

        sorted_points = sorted(points, key=lambda p: p.parameter_value)
        # Remove duplicates
        values = set()
        sorted_points_ = []
        for point in sorted_points:
            if not value_in_list(point.parameter_value, values) or point is self:  # Keep self in list
                # If another point with the same value as self is in the list, remove it and append self instead
                if (point is self and sorted_points_
                        and abs(sorted_points_[-1].parameter_value - self.parameter_value) <= threshold):
                    del sorted_points_[-1]
                sorted_points_.append(point)
                values.add(point.parameter_value)
        sorted_points = sorted_points_
        i = sorted_points.index(self)
        if i > 0:
            self._left_point = sorted_points[i - 1]
            self._left_value = self._left_point.parameter_value
            self._left_crit = self._left_point.criticality
        else:
            self._left_point = None
            self._left_value = None
            self._left_crit = None
        if i < len(sorted_points) - 1:
            self._right_point = sorted_points[i + 1]
            self._right_value = self._right_point.parameter_value
            self._right_crit = self._right_point.criticality
        else:
            self._right_point = None
            self._right_value = None
            self._right_crit = None

    def perceive(self, current_point: PointAgent, last_direction: int, last_step: float):
        self._is_current = self is current_point
        self._current_point = current_point
        self._step = last_step
        self._last_direction = last_direction
        self._all_points = self._get_all_points_in_chain()
        if not self.is_local_minimum:
            self._min_of_chain = min(self._all_points, key=lambda p: p.criticality)
        else:
            self._min_of_chain = self
        self._is_current_min_of_chain = self._min_of_chain is self
        in_chain = current_point in self._all_points
        self._is_current_in_chain = in_chain
        wait = False

        self.update_neighbors(self._all_points)

        if (self._is_current_min_of_chain and not self.is_local_minimum and (self._left_point or self._right_point)
                and (not self._left_point
                     or (self._left_point and abs(self._left_value - self.parameter_value) < self.LOCAL_MIN_THRESHOLD))
                and (not self._right_point
                     or self._right_point and abs(
                            self._right_value - self.parameter_value) < self.LOCAL_MIN_THRESHOLD)):
            self.log_debug('local min found')
            self._param_agent.local_min_found = True
            self.is_local_minimum = True
            if self.criticality < self.NULL_THRESHOLD:
                self.is_global_minimum = True
            similar_minima = [mini for mini in self._param_agent.minima
                              if abs(mini.parameter_value - self.parameter_value) <= self.SAME_POINT_THRESHOLD]
            already_went_up = any(mini.already_went_up for mini in similar_minima)
            max_steps_mult = max((mini.steps_mult for mini in similar_minima), default=1)
            other_minima = set(self._param_agent.minima) - set(similar_minima)

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
            self.go_up_mode = (len(self._param_agent.minima) == 0
                               or similar_minima and (not already_went_up or not other_minima))
            self._param_agent.add_minimum(self)

            if self.go_up_mode:
                self.log_debug('-> go up slope')
                self.first_point = True
                wait = True

        if self.is_local_minimum and not self.go_up_mode:
            self.update_neighbors(self._param_agent.minima, threshold=self.SAME_POINT_THRESHOLD)
            if self._param_agent.minima:
                filtered = filter(lambda mini: mini is self or abs(
                    mini.parameter_value - self.parameter_value) > self.SAME_POINT_THRESHOLD, self._param_agent.minima)
                self._sorted_minima: typ.List[PointAgent] = sorted(filtered, key=lambda mini: abs(
                    mini.parameter_value - self._current_point.parameter_value))
            else:
                self._sorted_minima = []
            if self._is_current_in_chain and len(self._sorted_minima) > 1 and self is self._sorted_minima[0]:
                self.best_local_minimum = True

        if self.go_up_mode:
            if not wait:
                self.first_point = False
            extremum_min = None
            extremum_max = None
            for point in self._all_points:
                if not extremum_min or point.parameter_value < extremum_min.parameter_value:
                    if extremum_min:
                        extremum_min.is_extremum = False
                    point.is_extremum = True
                    extremum_min = point
                elif not extremum_max or point.parameter_value > extremum_max.parameter_value:
                    if extremum_max:
                        extremum_max.is_extremum = False
                    point.is_extremum = True
                    extremum_max = point

    def decide(self) -> typ.Optional[typ.Union[Suggestion, GlobalMinimumFound]]:
        if self.is_global_minimum:
            return GlobalMinimumFound()
        if not self._is_current_in_chain and not self.best_local_minimum:
            return None

        direction = DIR_NONE
        suggested_step = None
        suggested_point = None
        new_chain_next = False
        check_for_out_of_bounds = False
        decision = ''
        from_value = self.parameter_value

        if self.is_extremum and self._min_of_chain.go_up_mode:
            decision, direction, new_chain_next, suggested_point, suggested_step = self._hill_climb()
        elif self._is_current_min_of_chain:
            if not self.is_local_minimum:
                decision, direction, suggested_point, suggested_step = self._local_search()
            elif self.best_local_minimum:
                (check_for_out_of_bounds, decision, direction, from_value,
                 new_chain_next, suggested_point, suggested_step) = self._semi_local_search()

        if decision:
            self.log_debug('Decision: ' + decision)

        if suggested_step is not None:
            # Cap jump length
            # suggested_step = min(self._param_agent.step_max, suggested_step)
            suggested_point = from_value + suggested_step * direction
            if (check_for_out_of_bounds
                    and (suggested_point < self._param_agent.inf or suggested_point > self._param_agent.sup)):
                self.prev_suggestion_out_of_bounds = True

        self._is_current = False

        if suggested_point is not None:
            return Suggestion(
                agent=self,
                next_point=min(self._param_agent.sup, max(self._param_agent.inf, suggested_point)),
                decision=decision,
                selected_objective='',
                criticality=self._current_point.criticality,
                local_min_found=self._param_agent.local_min_found,
                direction=direction,
                step=suggested_step,
                new_chain_next=new_chain_next,
            )
        return None

    def _local_search(self):
        direction = DIR_NONE
        suggested_point = None
        suggested_step = self._step
        self_value = self.parameter_value
        self_crit = self.criticality

        if self.previous_point is None and self.next_point is None:
            decision = 'first point in chain -> explore'
            if self_value == self._param_agent.inf:
                direction = DIR_INCREASE
            elif self_value == self._param_agent.sup:
                direction = DIR_DECREASE
            else:
                direction = self._last_direction or DIR_INCREASE  # TODO random if _last_direction is undefined

        elif self_value in (self._param_agent.inf, self._param_agent.sup):
            decision = 'point on bound -> go to middle'
            if self_value == self._param_agent.inf:
                other_value = self._right_value
            else:
                other_value = self._left_value
            suggested_point = (self_value + other_value) / 2

        elif (self._left_point is not None) != (self._right_point is not None):
            decision = '1 neighbor -> follow slope'
            if self._left_point:
                top_point = (self._left_value, self._left_crit)
            else:
                top_point = (self._right_value, self._right_crit)
            x = utils.get_xc(top_point, intermediate_point=(self_value, self_crit), yc=0)
            direction = DIR_INCREASE if x > self_value else DIR_DECREASE
            if abs(x - self_value) < self.STUCK_THRESHOLD:
                suggested_step = self._step * direction
            else:
                if self._last_direction == direction:
                    suggested_step = self._step * 2
                else:
                    suggested_step = self._step / 3
                # if self.parameter_name == 'p2':  # TEST
                #     suggested_steps_number /= 2

        else:
            decision = '2 neighbors -> go to middle point'
            if self._last_checked_direction == DIR_INCREASE and self._right_crit > self_crit:
                other_value = self._left_value
                self._last_checked_direction = DIR_DECREASE
            elif self._last_checked_direction == DIR_DECREASE and self._left_crit > self_crit:
                other_value = self._right_value
                self._last_checked_direction = DIR_INCREASE
            elif self.next_point and self.next_point.is_current:
                other_value = self.previous_point.parameter_value
            elif self._right_crit < self._left_crit:
                other_value = self._right_value
                self._last_checked_direction = DIR_INCREASE
            else:
                other_value = self._left_value
                self._last_checked_direction = DIR_DECREASE
            suggested_point = (self_value + other_value) / 2

        return decision, direction, suggested_point, suggested_step

    def _semi_local_search(self):
        self.best_local_minimum = False
        self_value = self.parameter_value
        self_crit = self.criticality
        from_value = self_value
        suggested_point = None
        suggested_step = None
        check_for_out_of_bounds = False
        direction = DIR_NONE
        other_min: typ.Optional[PointAgent] = None

        if (self._left_point is not None) != (self._right_point is not None):
            decision = '1 minimum neighbor -> follow slope'
            if self._left_point:
                top_point = (self._left_value, self._left_crit)
                other_min = self._left_point
            else:
                top_point = (self._right_value, self._right_crit)
                other_min = self._right_point
            if top_point[1] < self_crit:
                interm_point = top_point
                top_point = (self_value, self_crit)
                from_value = interm_point[0]
            else:
                interm_point = (self_value, self_crit)
            x = utils.get_xc(top_point, intermediate_point=interm_point, yc=0)
            direction = DIR_INCREASE if x > self_value else DIR_DECREASE
            suggested_step = abs(x - interm_point[0])
            check_for_out_of_bounds = True

        else:
            decision = '2 minima neighbors: '
            if self._left_crit < self_crit < self._right_crit:
                decision += 'left lower, right higher -> follow left slope'
                from_value = self._left_value
                x = utils.get_xc(top_point=(self_value, self_crit),
                                 intermediate_point=(self._left_value, self._left_crit), yc=0)
                direction = DIR_DECREASE
                other_min = self._left_point
                suggested_step = abs(x - self._left_value)
                check_for_out_of_bounds = True

            elif self._left_crit > self_crit > self._right_crit:
                decision += 'left higher, right lower -> follow right slope'
                from_value = self._right_value
                x = utils.get_xc(top_point=(self_value, self_crit),
                                 intermediate_point=(self._right_value, self._right_crit), yc=0)
                direction = DIR_INCREASE
                other_min = self._right_point
                suggested_step = abs(x - self._right_value)
                check_for_out_of_bounds = True

            elif self._left_crit < self_crit > self._right_crit:
                decision += 'both lower -> follow slope on lowest’s side'
                if self._right_crit < self._left_crit:
                    from_value = self._right_value
                    crit = self._right_crit
                    other_min = self._right_point
                else:
                    from_value = self._left_value
                    crit = self._left_crit
                    other_min = self._left_point
                x = utils.get_xc(top_point=(self_value, self_crit),
                                 intermediate_point=(from_value, crit), yc=0)
                direction = DIR_INCREASE
                suggested_step = abs(x - from_value)
                check_for_out_of_bounds = True

            else:
                decision += 'both higher -> go to middle point'
                if self._last_checked_direction == DIR_INCREASE and self._right_crit > self_crit:
                    other_value = self._left_value
                    self._last_checked_direction = DIR_DECREASE
                elif self._last_checked_direction == DIR_DECREASE and self._left_crit > self_crit:
                    other_value = self._right_value
                    self._last_checked_direction = DIR_INCREASE
                elif self.next_point and self.next_point.is_current:
                    other_value = self.previous_point.parameter_value
                elif self._right_crit < self._left_crit:
                    other_value = self._right_value
                    self._last_checked_direction = DIR_INCREASE
                else:
                    other_value = self._left_value
                    self._last_checked_direction = DIR_DECREASE
                suggested_point = (self_value + other_value) / 2

        if suggested_step is not None and self.local_min_already_visited:
            prev_min = self._param_agent.minima[-2]  # -1 is current minimum
            if (abs(self.parameter_value - prev_min.parameter_value) <= self.SAME_POINT_THRESHOLD
                    and prev_min.prev_suggestion_out_of_bounds):
                decision += ' -> previous was OOB -> cancel jump and go to middle'
                suggested_step = None
                suggested_point = (self_value + other_min.parameter_value) / 2
                check_for_out_of_bounds = False
            else:
                # We came back to an already visited local minimum, go twice as far than previously
                decision += ' and jump twice as far'
                self.steps_mult *= 2
                suggested_step *= self.steps_mult

        new_chain_next = True

        return (check_for_out_of_bounds, decision, direction, from_value,
                new_chain_next, suggested_point, suggested_step)

    def _hill_climb(self):
        decision = ''
        new_chain_next = False
        self_value = self.parameter_value
        self_crit = self.criticality
        suggested_point = None
        suggested_step = self._step
        direction = DIR_NONE

        other_extremum = [p for p in self._all_points if p.is_extremum and p is not self][0]
        other_extremum_value = other_extremum.parameter_value
        other_extremum_crit = other_extremum.criticality
        self_on_bound = self_value in [self._param_agent.inf, self._param_agent.sup]
        other_on_bound = other_extremum_value in [self._param_agent.inf, self._param_agent.sup]

        if other_extremum_value < self_value:
            prev_value = self._left_value
            prev_crit = self._left_crit
            prev = self._left_point
        else:
            prev_value = self._right_value
            prev_crit = self._right_crit
            prev = self._right_point

        if self_crit < prev_crit and not self_on_bound:
            decision = 'opposite slope found -> stop climbing; create new chain; explore'
            if prev is self._left_point or self_value == self._param_agent.inf:
                direction = DIR_INCREASE
            elif prev is self._right_point or self_value == self._param_agent.sup:
                direction = DIR_DECREASE
            else:
                direction = self._last_direction or DIR_INCREASE
            self._min_of_chain.go_up_mode = False
            self._min_of_chain.already_went_up = True
            if self._min_of_chain.first_point or self_on_bound:
                new_chain_next = True
            else:
                self.create_new_chain_from_me = True

        elif self_on_bound and other_on_bound:
            if self_crit < other_extremum_crit \
                    or (self_crit == other_extremum_crit and self_value > other_extremum_value):
                decision = 'both extrema on bounds -> go to middle; create new chain; explore'
                suggested_point = (self._param_agent.inf + self._param_agent.sup) / 2
                self._min_of_chain.go_up_mode = False
                self._min_of_chain.already_went_up = True
                if self._min_of_chain.first_point or self_on_bound:
                    new_chain_next = True
                else:
                    self.create_new_chain_from_me = True

        elif (self_crit < other_extremum_crit
              or (self_crit == other_extremum_crit and self_value > other_extremum_value)
              or other_on_bound) and not self_on_bound:
            direction = DIR_DECREASE if self_value < prev_value else DIR_INCREASE
            if (abs(self_value - prev_value) <= self.STUCK_THRESHOLD
                    or self_crit == other_extremum_crit or other_on_bound):
                decision = 'stuck -> move a bit' if not other_on_bound else 'other on bound -> move a bit'
                suggested_point = self_value + self._step * direction

            else:
                decision = 'go up slope'
                suggested_point = utils.get_xc(top_point=(prev_value, prev_crit),
                                               intermediate_point=(self_value, self_crit),
                                               yc=other_extremum_crit)

        return decision, direction, new_chain_next, suggested_point, suggested_step

    def get_minimum(self) -> typ.Optional[PointAgent]:
        if self.is_local_minimum:
            return self
        elif self.previous_point:
            return self.previous_point.get_minimum()
        else:
            return None

    @property
    def parameter_name(self) -> str:
        return self._param_agent.name

    @property
    def parameter_value(self) -> float:
        return self._param_value

    @property
    def objective_criticalities(self) -> typ.Dict[str, float]:
        return dict(self._criticalities)

    @property
    def criticality(self) -> float:
        return max(self._criticalities.values())

    @property
    def is_current(self) -> bool:
        return self._is_current

    def _get_all_points_in_chain(self) -> typ.List[PointAgent]:
        """Return all points in this point’s chain, including itself."""
        points = [self]
        p = self.previous_point
        while p:
            points.insert(0, p)
            p = p.previous_point

        p = self.next_point
        while p:
            points.append(p)
            p = p.next_point

        return points

    @staticmethod
    def _get_extrema(*points: PointAgent) -> typ.Tuple[PointAgent, PointAgent]:
        """Return the two most extreme points (along parameter value axis) in the given list.

        :param points: List of points to search into.
        :return: A tuple containing the minimum and maximum points from the list.
        """
        extremum_min = min(points, key=lambda p: p.parameter_value)
        extremum_max = max(points, key=lambda p: p.parameter_value)
        return extremum_min, extremum_max

    def __repr__(self):
        return (
            f'Point{{{self.name}={self.parameter_value},minimum={self.is_local_minimum},extremum={self.is_extremum},'
            f'min of chain={self._is_current_min_of_chain},current={self.is_current}}}'
        )


@dataclasses.dataclass(frozen=True)
class Suggestion:
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
