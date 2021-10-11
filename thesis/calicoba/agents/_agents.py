from __future__ import annotations

import abc
import dataclasses
import itertools
import typing as typ

from . import _messages, _normalizers
from .. import data_sources

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

    def __str__(self):
        return f'agent {self.id}'


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
    def __init__(self, name: str, criticality_function: CriticalityFunction, *output_agents: OutputAgent):
        super().__init__(name)
        self.__function = criticality_function
        self.__output_agents = output_agents
        self.__output_values: typ.Dict[str, float] = {}
        self.__parameters: typ.Sequence[ParameterAgent] = []
        self.__objective_value = 0
        self.__criticality = 0
        self.__normalizer = _normalizers.AllTimeAbsoluteNormalizer()
        self.__file = None

    @property
    def criticality(self) -> float:
        return self.__criticality

    def perceive(self):
        super().perceive()
        if not self.__parameters:
            self.__parameters = self.world.get_agents_for_type(ParameterAgent)
        self.__output_values = {a.name: a.value for a in self.__output_agents}

    def decide_and_act(self):
        from . import _messages
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
    direction: int


class ParameterAgent(AgentWithDataSource[data_sources.DataInput]):
    def __init__(self, source: data_sources.DataInput):
        super().__init__(source)
        self.__last_direction: typ.Optional[int] = None
        self.__current_action: typ.Optional[Action] = None
        self.__delta = 0
        source_range = source.sup - source.inf
        self.__delta_min = 1e-5 * source_range
        self.__delta_max = 1e-2 * source_range
        self.__influences = {}
        self.__file = None

    @property
    def last_direction(self) -> typ.Optional[int]:
        return self.__last_direction

    def perceive(self):
        super().perceive()
        if not self.__influences:
            self.__influences = {a.name: 0.5 for a in self.world.get_agents_for_type(ObjectiveAgent)}

    def decide_and_act(self):
        super().decide_and_act()
        self.world.logger.debug(self.name)
        old_value = self.value
        self.__current_action = None
        obj_to_help = ''

        self.__update_influences()

        messages = self.get_messages_for_type(_messages.CriticalityMessage)
        if self.world.config.free_parameter and self.world.config.free_parameter != self.name:
            messages.clear()

        if messages:
            message = self.__select_message(messages)
            influence = self.__influences[message.sender.name]

            if message.criticality != 0 and influence != 0:
                direction = DIR_DECREASE if influence > 0 else DIR_INCREASE
            else:
                direction = DIR_NONE

            if self.world.config.manual_actions:
                direction = int(input(f'Action for {self.name}: '))
            self.__current_action = Action(message.sender.name, direction)
            obj_to_help = message.sender.name
            if not self.world.config.manual_actions:
                self.world.logger.debug(f'{self.name} chose to help {obj_to_help}')

        if self.__current_action:
            self.__update_value(self.__current_action.direction)

        self._messages.clear()

        if self.world.config.dump_directory:
            if not self.__file:
                self.__file = (self.world.config.dump_directory / (self.name + '.csv')).open(mode='w', encoding='UTF-8')
                self.__file.write('cycle,value,action,helped obj\n')
            action = self.__current_action.direction if self.__current_action else 0
            self.__file.write(
                ','.join(map(str, [self.world.cycle, old_value, action, obj_to_help])) + '\n')
            self.__file.flush()

    def __select_message(self, messages: typ.List[_messages.CriticalityMessage]) -> _messages.CriticalityMessage:
        """Selects a request from all received requests using the following algorithm:

        - group each request by their criticality
        - sort the generated pairs (criticality, requests) by descending criticality
        - for each pair (criticality, requests):
        -   if there is only one request:
        -     return this request
        - return one request from the most critical ones

        :param messages: The list of messages to choose from.
        :return: The selected message.
        """
        crits_messages = itertools.groupby(sorted(messages, key=lambda m: abs(m.criticality), reverse=True),
                                           key=lambda m: abs(m.criticality))
        max_crit_messages = []
        for crit, messages_ in crits_messages:
            messages_ = [m for m in messages_]
            if not max_crit_messages:
                max_crit_messages = messages_
            if len(messages_) == 0:
                return messages_[0]
        return max_crit_messages[self.world.rng.randint(0, len(max_crit_messages) - 1)]

    def __update_influences(self):
        for objective_name, influence in self.__influences.items():
            # noinspection PyTypeChecker
            obj_agent: ObjectiveAgent = self.world.get_agent(
                lambda a: isinstance(a, ObjectiveAgent) and a.name == objective_name)
            if self.world.config.learn_influences:
                raise NotImplementedError('Influences learning is not available yet')
            else:
                crit = obj_agent.criticality
                new_influence = self.world.config.influence_function(self.name, self.value,
                                                                     objective_name, crit)
            self.__influences[objective_name] = new_influence

        self.world.logger.debug(self.__influences)

    def __update_value(self, new_direction: int):
        if self.world.config.manual_actions:
            self.__delta = -1.1 if new_direction == DIR_DECREASE else 1
        else:
            delta = self.__delta
            last_direction = self.__last_direction
            if last_direction and new_direction != DIR_NONE and last_direction != DIR_NONE:
                if new_direction == last_direction:
                    delta = 2 * self.__delta
                elif new_direction != last_direction:
                    delta = self.__delta / 3
            self.__delta = max(self.__delta_min, min(self.__delta_max, delta))
        self._data_source.set_data(self.value + self.__delta * new_direction)
        self.__last_direction = new_direction
        self.world.logger.debug(f'δ = {self.__delta}')

    def __del__(self):
        if self.__file:
            self.__file.close()


class CriticalityFunction(abc.ABC):
    def __init__(self, *parameter_names: str):
        self.__param_names = parameter_names

    @property
    def parameter_names(self) -> typ.Tuple[str]:
        return self.__param_names

    @abc.abstractmethod
    def __call__(self, **kwargs: float) -> float:
        pass
