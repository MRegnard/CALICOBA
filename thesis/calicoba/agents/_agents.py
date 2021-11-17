from __future__ import annotations

import abc
import dataclasses
import math
import typing as typ

from . import _messages, _normalizers
from .. import data_sources, utils

DIR_INCREASE = 1
DIR_DECREASE = -1
DIR_NONE = 0

_T1 = typ.TypeVar('_T1', bound=_messages.Message)


class Agent(abc.ABC):
    def __init__(self, name: str):
        from .. import Calicoba
        self.__id = -1
        self.__name = name
        self.__world_set = False
        # noinspection PyTypeChecker
        self.__world: Calicoba = None
        self._messages: typ.List[_messages.Message] = []
        self._dead = False

    @property
    def id(self) -> int:
        return self.__id

    @id.setter
    def id(self, value: int):
        if self.__world_set:
            raise RuntimeError('Cannot set agent ID after setting world')
        self.__id = value

    @property
    def world(self):
        return self.__world

    @world.setter
    def world(self, value):
        if self.__world_set:
            raise RuntimeError(f'World already set for agent {self}')
        self.__world = value
        self.__world_set = True

    @property
    def name(self) -> str:
        return self.__name

    def on_message(self, message):
        self._messages.append(message)

    def get_messages_for_type(self, type_: typ.Type[_T1]) -> typ.List[_T1]:
        return list(filter(lambda m: isinstance(m, type_), self._messages))

    def perceive(self):
        pass

    def decide_and_act(self):
        pass

    def die(self):
        self._dead = True

    @property
    def dead(self):
        return self._dead

    def __repr__(self):
        return f'{self.name}'


_T2 = typ.TypeVar('_T2', bound=data_sources.DataOutput)


class AgentWithDataSource(Agent, abc.ABC, typ.Generic[_T2]):
    def __init__(self, source: _T2):
        super().__init__(source.name)
        self._data_source = source
        self.__cached_attribute: float = 0

    @property
    def inf(self):
        return self._data_source.inf

    @property
    def sup(self):
        return self._data_source.sup

    @property
    def value(self):
        return self.__cached_attribute

    def perceive(self):
        super().perceive()
        self.__cached_attribute = self._data_source.get_data()


class OutputAgent(AgentWithDataSource[data_sources.DataOutput]):
    def __init__(self, source: data_sources.DataOutput):
        super().__init__(source)


class ObjectiveAgent(Agent):
    def __init__(self, name: str, criticality_function: CriticalityFunction, function_inf: float, function_sup: float,
                 *output_agents: OutputAgent):
        super().__init__(name)
        self.__function = criticality_function
        self.__output_agents = output_agents
        self.__output_values: typ.Dict[str, float] = {}
        self.__parameters: typ.Sequence[ParameterAgent] = []
        self.__objective_value = 0
        self.__criticality = 0
        self.__normalizer = _normalizers.BoundNormalizer(function_inf, function_sup)
        self.__file = None

    @property
    def criticality(self) -> float:
        return self.__criticality

    def init(self):
        self.__parameters = self.world.get_agents_for_type(ParameterAgent)

    def perceive(self):
        super().perceive()
        self.__output_values = {a.name: a.value for a in self.__output_agents}

    def decide_and_act(self):
        super().decide_and_act()

        old_value = self.__objective_value
        self.__objective_value = self.__function(**self.__output_values)
        self.__criticality = self.__normalizer(self.__objective_value)
        if old_value < self.__objective_value:
            variation = DIR_INCREASE
        elif old_value > self.__objective_value:
            variation = DIR_DECREASE
        else:
            variation = DIR_NONE

        for a in self.__parameters:
            a.on_message(_messages.CriticalityMessage(self, self.criticality, variation))

        if self.world.config.dump_directory:
            if not self.__file:
                self.__file = (self.world.config.dump_directory / (self.name + '.csv')).open(mode='w', encoding='UTF-8')
                col_names = ','.join(a.name for a in self.__output_agents)
                self.__file.write('cycle,' + col_names + ',raw value,criticality\n')
            self.__file.write(','.join(map(str, [
                self.world.cycle, *self.__output_values.values(), self.__objective_value, self.__criticality
            ])) + '\n')
            self.__file.flush()

    def __del__(self):
        if self.__file:
            self.__file.close()


@dataclasses.dataclass(frozen=True)
class Action:
    target_name: str
    value: float


class ParameterAgent(AgentWithDataSource[data_sources.DataInput]):
    def __init__(self, source: data_sources.DataInput):
        super().__init__(source)
        self._direction = DIR_NONE
        source_range = source.sup - source.inf
        self._max_step_number = 5
        self._init_step = source_range / 100
        self._step = self._init_step
        # FIXME interdépendence
        self._local_minimum_threshold = 1E-4
        self._null_criticality_threshold = 0.005

        self._helped_obj = None
        self._last_point_id = 0
        self._chains: typ.List[typ.Optional[PointAgent]] = [None]
        # noinspection PyTypeChecker
        self._current_point: PointAgent = None
        self._climbing = False
        self._current_min = None
        self._expected_criticality = -1
        self._last_criticality_above_min = -1
        self._last_criticality_below_min = -1
        self._wait_for_response = False
        self._stuck = False

        self._file = None

    @property
    def last_direction(self) -> int:
        return self._direction

    @property
    def is_climbing_slope(self) -> bool:
        return self._climbing

    @is_climbing_slope.setter
    def is_climbing_slope(self, value: bool):
        self._climbing = value

    @property
    def current_chain(self) -> PointAgent:
        return self._chains[-1]

    @property
    def step_max(self) -> int:
        return self._max_step_number

    def perceive(self):
        super().perceive()
        crits = {a.name: a.criticality for a in self.world.get_agents_for_type(ObjectiveAgent)}
        if (not self._current_point or (self._current_point.objective_criticalities != crits
                                        and self.value != self._current_point.parameter_value)):
            prev_point = self._chains[-1]
            if self._climbing:
                prev_crit = prev_point.objective_criticalities[self._helped_obj]
                if prev_point.parameter_value <= self._current_min.parameter_value:
                    prev = self._last_criticality_below_min
                    self._last_criticality_below_min = prev_crit
                    if (prev_point is not self._current_min
                            and abs(prev - self._last_criticality_below_min) < self._null_criticality_threshold):
                        self._stuck = True
                else:
                    prev = self._last_criticality_above_min
                    self._last_criticality_above_min = prev_crit
                    if (prev_point is not self._current_min
                            and abs(prev - self._last_criticality_above_min) < self._null_criticality_threshold):
                        self._stuck = True
                print('set last crit')
            self._current_point = PointAgent(f'point_{self._last_point_id}', self, prev_point, crits)
            self._chains[-1] = self._current_point
            self._last_point_id += 1
            self.world.add_agent(self._current_point)

    def decide_and_act(self):
        super().decide_and_act()
        self.world.logger.debug(self.name)
        old_value = self.value
        action = Action('', 0)
        obj_to_help = ''
        listened_point: typ.Optional[PointAgent] = None
        wait = False

        if not self.world.config.free_parameter or self.world.config.free_parameter == self.name:
            crit_messages = self.get_messages_for_type(_messages.CriticalityMessage)
            obj_to_help = crit_messages[0].sender.name
            crit = self._current_point.objective_criticalities[obj_to_help]

            if self._climbing:
                minimum = self._chains[-1].get_minimum()
                if not self._wait_for_response:
                    minimum.active = True
                    if crit > self._expected_criticality:
                        self.world.logger.debug('crit higher, sending request for other side')
                        minimum.on_message(_messages.RequestOtherSideMessage(self))
                        self._wait_for_response = True
                        wait = True
                if not wait and (
                        self.value < self._current_min.parameter_value and crit <= self._last_criticality_below_min
                        or self.value > self._current_min.parameter_value and crit <= self._last_criticality_above_min):
                    self.world.logger.debug('new valley found, stop climbing')
                    self._climbing = False
                    self._stuck = False
                    self._current_min = None
                    # Create new chain with current point
                    self._chains[-1] = self._current_point.previous_point
                    self._current_point.previous_point = None
                    self._chains.append(self._current_point)
                    minimum.active = False

            if (not self._climbing and self._current_point.previous_point is not None
                    and abs(crit - self._current_point.previous_point.objective_criticalities[obj_to_help])
                    < self._local_minimum_threshold
                    and not self._current_point.local_minimum):
                self.world.logger.debug('local minimum found')
                self._current_point.local_minimum = True
                self._current_point.active = True
                self._current_min = self._current_point
                self._last_criticality_below_min = self._current_min.objective_criticalities[obj_to_help]
                self._last_criticality_above_min = self._last_criticality_below_min
                self._step = self._init_step
                if crit < self._null_criticality_threshold:
                    self.world.logger.info(
                        f'Found global minimum for objective {obj_to_help}: {self.name} = {self.value}')
                    self.world.stop()
                elif len(self._chains) > 1 and self._chains[-1] is not None:
                    minima = list(filter(lambda p: p is not None,
                                         [p.get_minimum() for p in self._chains if p is not None]))
                    if len(minima) >= 2:
                        self.world.logger.debug('follow minima slope')
                        self.world.logger.debug(minima)
                        m1 = min(minima, key=lambda p: p.objective_criticalities[obj_to_help])
                        # TEST tester d’autres variantes
                        m2 = max(minima, key=lambda p: p.objective_criticalities[obj_to_help])
                        self._direction = DIR_INCREASE if m1.parameter_value > m2.parameter_value else DIR_DECREASE
                        action = Action(
                            obj_to_help,
                            m1.parameter_value + abs(m1.parameter_value - m2.parameter_value) * self._direction
                        )

            elif not wait:
                self.world.logger.debug(self._messages)
                variation_messages = self.get_messages_for_type(_messages.VariationSuggestionMessage)
                if self._stuck:
                    self.world.logger.debug('stuck, moving by δ')
                    action = Action(obj_to_help, self.value + self._init_step * self._direction)
                    self._wait_for_response = True
                    self._current_min.active = True
                    self._stuck = False
                elif not self._climbing and variation_messages:
                    self.world.logger.debug('move by k * δ')
                    m = [m for m in variation_messages if m.sender == self._current_point][0]
                    listened_point = m.sender
                    k = min(m.steps_number, self._max_step_number)
                    action = Action(obj_to_help, self.value + self._step * k * m.direction)
                    self._direction = m.direction
                elif new_value_messages := self.get_messages_for_type(_messages.NewValueSuggestionMessage):
                    self.world.logger.debug('move to new point')
                    m = [m for m in new_value_messages
                         if not self._climbing and m.sender == self._current_point
                         or self._climbing and m.sender.local_minimum][0]
                    listened_point = m.sender
                    action = Action(obj_to_help, m.new_parameter_value)
                    if m.climbing:
                        self._climbing = True
                        self._expected_criticality = m.expected_criticality
                        self._step = self._init_step
                        if (self.value == self.inf and m.new_parameter_value < self.inf
                                or self.value == self.sup and m.new_parameter_value > self.sup):
                            self.world.logger.debug('climbing outside domain, revert to exploration')
                            self._climbing = False
                            self._current_point.active = True
                        if self._wait_for_response:
                            self._wait_for_response = False
                    else:
                        self._step /= 4
                    self._direction = math.copysign(DIR_INCREASE, action.value - self.value)

        self._helped_obj = obj_to_help
        v = action.value
        if v:
            self._data_source.set_data(v)
            self.world.logger.debug(f'new value: {v}')

        self._messages.clear()

        if self.world.config.dump_directory:
            if not self._file:
                self._file = (self.world.config.dump_directory / (self.name + '.csv')).open(mode='w', encoding='utf8')
                self._file.write('cycle,value,action,listened point,minimum,helped obj\n')
            action = action.value if action else 0
            lp_value = listened_point.parameter_value if listened_point else ''
            self._file.write(','.join(map(str, [self.world.cycle, old_value, action, lp_value,
                                                int(self._current_point.local_minimum), obj_to_help])) + '\n')
            self._file.flush()

    def __del__(self):
        if self._file:
            self._file.close()

    def __repr__(self):
        return f'{self.name}={self.value}'


class PointAgent(Agent):
    def __init__(self, name: str, parameter_agent: ParameterAgent, previous_point: typ.Optional[PointAgent],
                 objective_criticalities: typ.Dict[str, float]):
        super().__init__(name)
        self._previous_point = previous_point
        self._param_value = parameter_agent.value
        self._param_agent = parameter_agent
        self._criticalities = objective_criticalities
        self._helped_obj = ''
        self.local_minimum = False
        self.active = True
        self._chosen_side = DIR_NONE

    def distance_to(self, other_point: PointAgent) -> float:
        s = sum(map(lambda e: (e[0] - e[1]) ** 2,
                    zip(self.objective_criticalities.values(), other_point.objective_criticalities.values())))
        return math.sqrt(s)

    def decide_and_act(self):
        if not self.active:
            return

        direction = DIR_NONE
        steps_number = 0
        suggested_point = 0
        climbing = False
        expected_crit = -1

        self._helped_obj = max(self._criticalities.items(), key=lambda item: item[1])[0]
        self_crit = self._criticalities[self._helped_obj]
        self_value = self._param_value

        if self._previous_point is None:  # No neighbors, i.e. first point in chain
            self.world.logger.debug('first point in chain, explore')
            if self_value == self._param_agent.inf:
                direction = DIR_INCREASE
            elif self_value == self._param_agent.sup:
                direction = DIR_DECREASE
            else:
                direction = self._param_agent.last_direction or DIR_INCREASE
            steps_number = 1

        else:
            if not self.local_minimum:
                previous_crit = self._previous_point.objective_criticalities[self._helped_obj]
                if self_crit < previous_crit:
                    self.world.logger.debug('follow local slope')
                    if ((self_value > self._previous_point.parameter_value or self_value == self._param_agent.inf)
                            and self_value != self._param_agent.sup):
                        direction = DIR_INCREASE
                    else:
                        direction = DIR_DECREASE
                    x = utils.get_xc(a=(self._previous_point.parameter_value, previous_crit),
                                     e=(self_value, self_crit), yc=0)
                    steps_number = abs(x - self_value) / abs(self_value - self._previous_point.parameter_value)
                    expected_crit = 0
                elif self._param_agent.is_climbing_slope:
                    self.world.logger.debug('go up slope')
                    if self_value < self._previous_point.parameter_value:
                        direction = DIR_DECREASE
                    else:
                        direction = DIR_INCREASE
                    steps_number = 1
                    # Is this the second point of the current chain?
                elif self._previous_point.previous_point is None:
                    self.world.logger.debug('wrong way, turn around')
                    if self_value < self._previous_point.parameter_value:
                        direction = DIR_INCREASE
                    else:
                        direction = DIR_DECREASE
                    x = utils.get_xc(a=(self_value, self_crit),
                                     e=(self._previous_point.parameter_value, previous_crit), yc=0)
                    steps_number = abs(x - self_value) / abs(
                        self_value - self._previous_point.parameter_value)
                    expected_crit = 0
                else:
                    self.world.logger.debug('go to middle point')
                    suggested_point = (self_value + self._previous_point.parameter_value) / 2

            else:
                self.world.logger.debug('go up slope')
                p = self._param_agent.current_chain
                chain = [p]
                while p:
                    p = p.previous_point
                    if p and p != self:
                        chain.append(p)
                # Point with smallest parameter value in chain
                p1 = min(chain, key=lambda point: point.parameter_value)
                # Point with greatest parameter value in chain
                p2 = max(chain, key=lambda point: point.parameter_value)

                crit_p1 = p1.objective_criticalities[self._helped_obj]
                crit_p2 = p2.objective_criticalities[self._helped_obj]

                if self._chosen_side == DIR_NONE:
                    if (crit_p1 < crit_p2 and p1.parameter_value < p2.parameter_value
                            or crit_p1 < crit_p2 and p2.parameter_value < p1.parameter_value):
                        self._chosen_side = DIR_DECREASE
                    else:
                        self._chosen_side = DIR_INCREASE
                elif self.get_messages_for_type(_messages.RequestOtherSideMessage):
                    self._chosen_side = -self._chosen_side

                if (self._chosen_side == DIR_INCREASE and p1.parameter_value > p2.parameter_value
                        or self._chosen_side == DIR_DECREASE and p1.parameter_value < p2.parameter_value):
                    p_ = (p1.parameter_value, crit_p1)
                    expected_crit = crit_p2
                else:
                    p_ = (p2.parameter_value, crit_p2)
                    expected_crit = crit_p1
                print((self_value, self_crit))
                print(p_)
                suggested_point = utils.get_xc(a=(self_value, self_crit), e=p_, yc=expected_crit)
                print((suggested_point, expected_crit))

                climbing = True

        if direction:
            self._param_agent.on_message(
                _messages.VariationSuggestionMessage(self, direction, steps_number))
        elif suggested_point:
            self._param_agent.on_message(
                _messages.NewValueSuggestionMessage(self, suggested_point, climbing, expected_crit))

        self._messages.clear()

        self.active = False

    def get_minimum(self) -> typ.Optional[PointAgent]:
        if self.local_minimum:
            return self
        elif self._previous_point:
            return self._previous_point.get_minimum()
        else:
            return None

    @property
    def parameter_value(self) -> float:
        return self._param_value

    @property
    def previous_point(self) -> typ.Optional[PointAgent]:
        return self._previous_point

    @previous_point.setter
    def previous_point(self, value: typ.Optional[PointAgent]):
        self._previous_point = value

    @property
    def objective_criticalities(self) -> typ.Dict[str, float]:
        return dict(self._criticalities)

    def __repr__(self):
        return f'{self.name}={self.parameter_value}'


class CriticalityFunction(abc.ABC):
    def __init__(self, *parameter_names: str):
        self.__param_names = parameter_names

    @property
    def parameter_names(self) -> typ.Tuple[str]:
        return self.__param_names

    @abc.abstractmethod
    def __call__(self, **kwargs: float) -> float:
        pass
