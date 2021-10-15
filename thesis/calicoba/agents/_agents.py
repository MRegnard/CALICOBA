from __future__ import annotations

import abc
import dataclasses
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
        # TEMP ne fonctionne que pour une seule sortie
        self.__normalizer = _normalizers.BoundNormalizer(0, max(output_agents[0].inf, output_agents[0].sup))
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
    value: float


class ParameterAgent(AgentWithDataSource[data_sources.DataInput]):
    def __init__(self, source: data_sources.DataInput):
        super().__init__(source)
        self.__direction: typ.Optional[int] = None
        source_range = source.sup - source.inf
        self.__delta = 0.01 * source_range
        self.__file = None
        self.__check_next = [DIR_INCREASE, DIR_DECREASE]
        self.__ref_crit = 0
        self.__crit_below = 0
        self.__crit_above = 0

    @property
    def last_direction(self) -> typ.Optional[int]:
        return self.__direction

    def decide_and_act(self):
        super().decide_and_act()
        self.world.logger.debug(self.name)
        old_value = self.value
        action = None
        obj_to_help = ''
        proportion = 0.5

        messages = self.get_messages_for_type(_messages.CriticalityMessage)
        if self.world.config.free_parameter and self.world.config.free_parameter != self.name:
            messages.clear()

        if messages:
            crit = messages[0].criticality
            obj_to_help = messages[0].sender.name

            if len(self.__check_next) == 2:  # Exploration d’un coté
                self.__ref_crit = abs(crit)
                self.__direction = self.__check_next.pop(0)
                action = Action(obj_to_help, self.__delta * self.__direction)
            elif len(self.__check_next) == 1:  # Exploration de l’autre coté
                if self.__direction == DIR_INCREASE:
                    self.__crit_above = abs(crit)
                else:
                    self.__crit_below = abs(crit)
                self.__direction = self.__check_next.pop(0)
                action = Action(obj_to_help, self.__delta * self.__direction * 2)
            else:  # Comparaison des criticités des deux cotés
                if self.__direction == DIR_INCREASE:
                    self.__crit_above = abs(crit)
                else:
                    self.__crit_below = abs(crit)
                # Déplacement du meilleur coté
                if self.__crit_below < self.__ref_crit or self.__crit_above < self.__ref_crit:
                    if self.__crit_above < self.__crit_below:
                        self.__direction = DIR_INCREASE
                        c = self.__crit_above
                    else:
                        self.__direction = DIR_DECREASE
                        c = self.__crit_below
                    action = Action(obj_to_help, (self.__direction * self.__delta / proportion) * (self.__ref_crit / c))
                    self.__check_next = [self.__direction, -self.__direction]
                else:  # Aucun coté n’améliore la situation, explorer
                    pass  # TODO explorer

        if action:
            v = action.value
            self._data_source.set_data(self.value + v)
            self.world.logger.debug(f'δ = {v}')

        self._messages.clear()

        if self.world.config.dump_directory:
            if not self.__file:
                self.__file = (self.world.config.dump_directory / (self.name + '.csv')).open(mode='w', encoding='UTF-8')
                self.__file.write('cycle,value,action,helped obj\n')
            action = action.value if action else 0
            self.__file.write(
                ','.join(map(str, [self.world.cycle, old_value, action, obj_to_help])) + '\n')
            self.__file.flush()

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
