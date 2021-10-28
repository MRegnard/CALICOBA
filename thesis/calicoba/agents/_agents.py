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
        self._direction: typ.Optional[int] = None
        source_range = source.sup - source.inf
        self._max_step = 0.01 * source_range
        self._file = None
        self._memory: typ.List[PointAgent] = []
        self._last_memory_id = 0

    @property
    def last_direction(self) -> typ.Optional[int]:
        return self._direction

    @property
    def max_step(self) -> float:
        return self._max_step

    @property
    def memory(self) -> typ.List[PointAgent]:
        return list(self._memory)

    def perceive(self):
        crits = {a.name: a.criticality for a in self.world.get_agents_for_type(ObjectiveAgent)}
        point = PointAgent(f'point_{self._last_memory_id}', self, crits, self.world.cycle)
        self._last_memory_id += 1
        self.world.add_agent(point)
        self._memory.append(point)

    def decide_and_act(self):
        super().decide_and_act()
        self.world.logger.debug(self.name)
        old_value = self.value
        action = None
        obj_to_help = ''

        crit_messages = self.get_messages_for_type(_messages.CriticalityMessage)
        variation_messages = self.get_messages_for_type(_messages.VariationSuggestionMessage)
        if self.world.config.free_parameter and self.world.config.free_parameter != self.name:
            crit_messages.clear()

        if crit_messages:
            crit = crit_messages[0].criticality
            obj_to_help = crit_messages[0].sender.name
            variation_message = max(variation_messages, key=lambda m: m.sender.creation_cycle)
            variation_message.sender.chosen = True
            action = Action(obj_to_help, min(variation_message.variation, self._max_step))

        if action:
            v = action.value
            self._data_source.set_data(self.value + v)
            self.world.logger.debug(f'δ = {v}')

        self._messages.clear()

        if self.world.config.dump_directory:
            if not self._file:
                self._file = (self.world.config.dump_directory / (self.name + '.csv')).open(mode='w', encoding='utf8')
                self._file.write('cycle,value,action,helped obj\n')
            action = action.value if action else 0
            self._file.write(','.join(map(str, [self.world.cycle, old_value, action, obj_to_help])) + '\n')
            self._file.flush()

    def __del__(self):
        if self._file:
            self._file.close()


class PointAgent(Agent):
    MAX_LIFESPAN = 20

    def __init__(self, name: str, parameter_agent: ParameterAgent, objective_criticalities: typ.Dict[str, float],
                 cycle: int):
        super().__init__(name)
        self._closest_above: typ.Optional[PointAgent] = None
        self._closest_below: typ.Optional[PointAgent] = None
        self._parameter_value = parameter_agent.value
        self._parameter_agent = parameter_agent
        self._objective_criticalities = objective_criticalities
        self._creation_cycle = cycle
        self._chosen = False
        self._variation_suggestion = 0
        self._helped_objective = ''
        self._expected_criticality = 0

    def perceive(self):
        for point in self._parameter_agent.memory:
            if point is not self:
                if (self._closest_above is None
                        or self._closest_above.parameter_value > point.parameter_value > self._parameter_value):
                    self._closest_above = point
                elif (self._closest_below is None
                      or self._parameter_value < point.parameter_value < self._closest_below.parameter_value):
                    self._closest_below = point

    def decide_and_act(self):
        if self.world.cycle - self._creation_cycle >= self.MAX_LIFESPAN:
            self.die()
        else:
            variation = 0
            if (self._chosen and ((self._variation_suggestion == DIR_INCREASE
                                   and self._closest_above.objective_criticalities[
                                       self._helped_objective] > self._expected_criticality)
                                  or (self._variation_suggestion == DIR_DECREASE
                                      and self._closest_below.objective_criticalities[
                                          self._helped_objective] > self._expected_criticality))):
                if self._variation_suggestion == DIR_INCREASE:
                    v = self._closest_above.parameter_value
                else:
                    v = self._closest_below.parameter_value
                variation = (v - self._parameter_value) / 2
                self._chosen = False
            else:
                crit_messages = self.get_messages_for_type(_messages.CriticalityMessage)
                crit = crit_messages[0].criticality
                obj_to_help = crit_messages[0].sender.name

                above = self._closest_above
                below = self._closest_below
                if above is None and below is None:
                    variation = self._parameter_agent.max_step
                elif (above is None) != (below is None):
                    if above is not None:
                        if above.objective_criticalities[obj_to_help] < self._objective_criticalities[obj_to_help]:
                            pass  # TODO suivre pente
                    elif below is not None:
                        if below.objective_criticalities[obj_to_help] < self._objective_criticalities[obj_to_help]:
                            pass  # TODO suivre pente

                    if not variation:
                        pass  # TODO explorer coté opposé

                self._helped_objective = obj_to_help

            if variation:
                self._parameter_agent.on_message(_messages.VariationSuggestionMessage(self, variation))
                self._expected_criticality = 0

    @property
    def parameter_value(self) -> float:
        return self._parameter_value

    @property
    def objective_criticalities(self) -> typ.Dict[str, float]:
        return dict(self._objective_criticalities)

    @property
    def creation_cycle(self) -> int:
        return self._creation_cycle


class CriticalityFunction(abc.ABC):
    def __init__(self, *parameter_names: str):
        self.__param_names = parameter_names

    @property
    def parameter_names(self) -> typ.Tuple[str]:
        return self.__param_names

    @abc.abstractmethod
    def __call__(self, **kwargs: float) -> float:
        pass
