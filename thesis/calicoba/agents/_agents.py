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
            self._logger.error(message)

    def log_critical(self, message):
        if self._logger:
            self._logger.critical(message)

    def log_warning(self, message):
        if self._logger:
            self._logger.warning(message)

    def log_info(self, message):
        if self._logger:
            self._logger.info(message)

    def log_debug(self, message):
        if self._logger:
            self._logger.debug(message)

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
        self._max_step_number = 5
        self._init_step = (sup - inf) / 100

        self._chains: typ.List[PointAgent] = []
        self._minima = set()
        self._last_point_id = 0

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
    def start_init_step(self) -> float:
        return self._init_step

    @property
    def step_max(self) -> int:
        return self._max_step_number

    @property
    def minima(self) -> typ.Set[PointAgent]:
        return self._minima

    def add_minima(self, point: PointAgent):
        self._minima.add(point)

    def perceive(self, value: float, new_chain: bool, init_step: typ.Optional[float],
                 criticalities: typ.Dict[str, float]) -> PointAgent:
        self._value = value
        prev_point = self._chains[-1] if self._chains else None

        if not prev_point or prev_point.parameter_value != value or prev_point.objective_criticalities != criticalities:
            new_point = PointAgent(
                f'point_{self._last_point_id}',
                self,
                prev_point if not new_chain else None,
                init_step or self.start_init_step,
                criticalities,
                logger=self._logger
            )
            self._last_point_id += 1
            if self._chains and not new_chain:
                prev_point.next_point = new_point
                if prev_point.create_new_chain_from_me:
                    self._chains[-1] = prev_point.previous_point
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

    def __init__(self, name: str, parameter_agent: ParameterAgent, previous_point: typ.Optional[PointAgent],
                 init_step: float, objective_criticalities: typ.Dict[str, float], *, logger: logging.Logger = None):
        super().__init__(name, logger=logger)
        self.previous_point = previous_point
        self.next_point: typ.Optional[PointAgent] = None
        self._param_value = parameter_agent.value
        self._param_agent = parameter_agent
        self._criticalities = objective_criticalities
        self._last_direction = DIR_NONE
        self._helped_obj = None
        self._is_current = True
        self._current_point = None
        self._is_current_in_chain = False
        self._left_point = None
        self._left_value = None
        self._left_crit = None
        self._right_point = None
        self._right_value = None
        self._right_crit = None
        self._min_of_chain = None
        self._is_current_min_of_chain = False
        self.is_local_minimum = False
        self.is_extremum = False
        self.go_up_mode = False
        self.create_new_chain_from_me = False
        self._step = init_step
        self._all_points: typ.List[PointAgent] = []
        self._last_checked_direction = DIR_NONE

    def perceive(self, current_point: PointAgent, last_direction: int, criticalities: typ.Dict[str, float]):
        self._is_current = self is current_point
        self._current_point = current_point
        self._last_direction = last_direction
        self._helped_obj = max(criticalities.items(), key=lambda item: item[1])[0]
        self._all_points = self._get_all_points_in_chain()
        if not self.is_local_minimum:
            self._min_of_chain = min(self._all_points, key=lambda p: p.objective_criticalities[self._helped_obj])
        else:
            self._min_of_chain = self
        self._is_current_min_of_chain = self._min_of_chain is self
        self._is_current_in_chain = current_point in self._all_points

        sorted_points = sorted(self._all_points, key=lambda p: p.parameter_value)
        i = sorted_points.index(self)
        if i > 0:
            self._left_point = sorted_points[i - 1]
            self._left_value = self._left_point.parameter_value
            self._left_crit = self._left_point.objective_criticalities[self._helped_obj]
        else:
            self._left_point = None
            self._left_value = None
            self._left_crit = None
        if i < len(sorted_points) - 1:
            self._right_point = sorted_points[i + 1]
            self._right_value = self._right_point.parameter_value
            self._right_crit = self._right_point.objective_criticalities[self._helped_obj]
        else:
            self._right_point = None
            self._right_value = None
            self._right_crit = None

        self_crit = self.objective_criticalities[self._helped_obj]
        if (self._left_point and self._right_point and self._is_current_min_of_chain and not self.is_local_minimum
                and abs(self._left_crit - self_crit) < self.LOCAL_MIN_THRESHOLD
                and abs(self._right_crit - self_crit) < self.LOCAL_MIN_THRESHOLD):
            self.log_debug('local min found')
            self.is_local_minimum = True
            self.go_up_mode = len(self._param_agent.minima) == 0
            self._param_agent.add_minima(self)
            if self.go_up_mode:
                self.log_debug('-> go up slope')

        if self.go_up_mode:
            extremum_min = self
            extremum_max = self
            for point in self._all_points:
                if point.parameter_value < extremum_min.parameter_value:
                    extremum_min.is_extremum = False
                    point.is_extremum = True
                    extremum_min = point
                if point.parameter_value > extremum_max.parameter_value:
                    extremum_max.is_extremum = False
                    point.is_extremum = True
                    extremum_max = point

    def decide(self) -> typ.Optional[Suggestion]:
        if not self._is_current_in_chain:
            return None

        direction = DIR_NONE
        suggested_steps_number = None
        suggested_point = None
        next_init_step = self._step
        new_chain_next = False
        decision = ''

        self_crit = self._criticalities[self._helped_obj]
        self_value = self.parameter_value

        if self._param_agent.minima:
            sorted_minima: typ.List[PointAgent] = \
                sorted(self._param_agent.minima,
                       key=lambda mini: abs(mini.parameter_value - self._current_point.parameter_value))
        else:
            sorted_minima = []

        if self._is_current_min_of_chain:
            if not self.is_local_minimum:
                if self.previous_point is None and self.next_point is None:
                    decision = 'first point in chain -> explore'
                    if self_value == self._param_agent.inf:
                        direction = DIR_INCREASE
                    elif self_value == self._param_agent.sup:
                        direction = DIR_DECREASE
                    else:
                        direction = self._last_direction or DIR_INCREASE
                    suggested_steps_number = 1
                    next_init_step = self._param_agent.start_init_step

                elif (self._left_point is not None) != (self._right_point is not None):
                    decision = '1 neighbor -> follow slope'
                    if self._left_point:
                        a = (self._left_value, self._left_crit)
                    else:
                        a = (self._right_value, self._right_crit)
                    self._step = abs(self_value - a[0])
                    x = utils.get_xc(a, e=(self_value, self_crit), yc=0)
                    if ((x > self_value or self_value == self._param_agent.inf)
                            and self_value != self._param_agent.sup):
                        direction = DIR_INCREASE
                    else:
                        direction = DIR_DECREASE
                    suggested_steps_number = abs(x - self_value) / self._step

                else:
                    decision = '2 neighbors -> go to middle on side of lowest neighor'
                    if self._last_checked_direction == DIR_INCREASE and self._right_crit > self_crit:
                        other_value = self._left_value
                        self._last_checked_direction = DIR_DECREASE
                    elif self._last_checked_direction == DIR_DECREASE and self._left_crit > self_crit:
                        other_value = self._right_value
                        self._last_checked_direction = DIR_INCREASE
                    else:
                        if self.next_point and self.next_point.is_current:
                            other_value = self.previous_point.parameter_value
                        elif self._right_crit < self._left_crit:
                            other_value = self._right_value
                            self._last_checked_direction = DIR_INCREASE
                        else:
                            other_value = self._left_value
                            self._last_checked_direction = DIR_DECREASE
                    suggested_point = (self_value + other_value) / 2

            elif len(sorted_minima) > 1 and sorted_minima[0] is self:
                decision = '-> follow local minima slope'
                other_minimum = sorted_minima[1]
                other_value = other_minimum.parameter_value
                other_crit = other_minimum.objective_criticalities[self._helped_obj]
                if self_crit < other_crit:
                    a = (other_value, other_crit)
                    e = (self_value, self_crit)
                    direction = DIR_DECREASE if self_value < other_value else DIR_INCREASE
                else:
                    a = (self_value, self_crit)
                    e = (other_value, other_crit)
                    direction = DIR_DECREASE if self_value > other_value else DIR_INCREASE
                x = utils.get_xc(a, e, yc=0)
                self._step = abs(self_value - other_value)
                suggested_steps_number = abs(x - self_value) / self._step
                next_init_step = self._param_agent.start_init_step
                new_chain_next = True

        elif self.is_extremum and self._min_of_chain.go_up_mode:
            other_extremum = [p for p in self._all_points if p.is_extremum and p is not self][0]
            other_extremum_value = other_extremum.parameter_value
            other_extremum_crit = other_extremum.objective_criticalities[self._helped_obj]

            sorted_points = sorted(self._all_points, key=lambda p: p.parameter_value)
            i = sorted_points.index(self)
            previous_on_slope = sorted_points[i + (1 if self.parameter_value < other_extremum_value else -1)]
            prev_value = previous_on_slope.parameter_value
            prev_crit = previous_on_slope.objective_criticalities[self._helped_obj]

            if self_crit < prev_crit:
                decision = 'opposite slope found -> stop climbing; create new chain; explore'
                if self_value == self._param_agent.inf:
                    direction = DIR_INCREASE
                elif self_value == self._param_agent.sup:
                    direction = DIR_DECREASE
                else:
                    direction = self._last_direction or DIR_INCREASE
                suggested_steps_number = 1
                next_init_step = self._param_agent.start_init_step
                self._min_of_chain.go_up_mode = False
                self.create_new_chain_from_me = True

            elif self_crit < other_extremum_crit:
                if abs(self_value - prev_value) <= self.STUCK_THRESHOLD:
                    decision = 'stuck -> move a bit'
                    if self_value < prev_value:
                        direction = DIR_DECREASE
                    else:
                        direction = DIR_INCREASE
                    suggested_point = self_value + self._step * direction

                else:
                    decision = 'go up slope'
                    if self_value < prev_value:
                        direction = DIR_DECREASE
                    else:
                        direction = DIR_INCREASE
                    suggested_point = utils.get_xc(a=(prev_value, prev_crit), e=(self_value, self_crit),
                                                   yc=other_extremum_crit)

        if suggested_steps_number is not None:
            suggested_point = self_value + self._step * suggested_steps_number * direction

        max_jump_distance = (self._param_agent.sup - self._param_agent.inf) / 10
        if suggested_point is not None and abs(suggested_point - self_value) > max_jump_distance:
            old_point = suggested_point
            suggested_point = self_value + max_jump_distance * direction
            self.log_debug(f'capped jump distance: {old_point} -> {suggested_point}')

        self._is_current = False
        if decision:
            self.log_debug(decision)

        if suggested_point:
            return Suggestion(
                agent=self,
                next_point=suggested_point,
                decision=decision,
                selected_objective=self._helped_obj,
                criticality=self._current_point.objective_criticalities[self._helped_obj],
                direction=direction,
                step=self._step,
                steps_number=suggested_steps_number,
                next_init_step=next_init_step,
                new_chain_next=new_chain_next,
            )
        return None

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
    next_init_step: float
    new_chain_next: bool = False
    step: float = None
    steps_number: float = None
    direction: int = None


class ObjectiveFunction(abc.ABC):
    def __init__(self, *parameter_names: str):
        self._param_names = parameter_names

    @property
    def parameter_names(self) -> typ.Tuple[str]:
        return self._param_names

    @abc.abstractmethod
    def __call__(self, **kwargs: float) -> float:
        pass
