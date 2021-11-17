import dataclasses
import logging
import pathlib
import random
import typing as typ

from . import agents, data_sources

_T = typ.TypeVar('_T', bound=agents.Agent)


@dataclasses.dataclass(frozen=True)
class CalicobaConfig:
    dump_directory: pathlib.Path = None
    free_parameter: typ.Optional[str] = None
    learn_influences: bool = False
    alpha: float = 0.5
    manual_actions: bool = False
    influence_function: typ.Callable[[str, float, str, float], float] = None
    seed: typ.Optional[int] = None
    logging_level: int = logging.INFO

    def __post_init__(self):
        if not (0 <= self.alpha <= 1):
            raise ValueError('alpha must be in [0, 1]')


class Calicoba:
    def __init__(self, config: CalicobaConfig):
        self.__config = config
        self.__logger = logging.getLogger('CALICOBA')
        self.__logger.setLevel(self.__config.logging_level)
        self.__rng = random.Random(self.__config.seed)
        if self.__config.dump_directory and not self.__config.dump_directory.exists():
            self.__config.dump_directory.mkdir(parents=True)
        self.__cycle = 0
        self.__stop = False
        self.__global_ids: typ.Dict[typ.Type[agents.Agent], int] = {}
        self.__agents_registry: typ.List[agents.Agent] = []
        self.__agents_id_registry: typ.Dict[int, agents.Agent] = {}
        self.__output_agents: typ.List[agents.OutputAgent] = []
        self.__parameter_agents: typ.List[agents.ParameterAgent] = []
        self.__objective_agents: typ.List[agents.ObjectiveAgent] = []

    @property
    def config(self) -> CalicobaConfig:
        return self.__config

    @property
    def rng(self) -> random.Random:
        return self.__rng

    @property
    def logger(self) -> logging.Logger:
        return self.__logger

    @property
    def cycle(self) -> int:
        return self.__cycle

    def get_agents_for_type(self, type_: typ.Type[_T]) -> typ.Sequence[_T]:
        return list(filter(lambda a: isinstance(a, type_), self.__agents_registry))

    def get_agent(self, predicate: typ.Callable[[agents.Agent], bool]) -> typ.Optional[agents.Agent]:
        return next(filter(predicate, self.__agents_registry), None)

    def get_agent_by_id(self, agent_id: int) -> typ.Optional[agents.Agent]:
        return self.__agents_id_registry.get(agent_id)

    def add_parameter(self, source: data_sources.DataInput):
        self.__logger.info(f'Creating parameter "{source.name}".')
        self.add_agent(agents.ParameterAgent(source))

    def add_output(self, source: data_sources.DataOutput):
        self.__logger.info(f'Creating output "{source.name}".')
        self.add_agent(agents.OutputAgent(source))

    def add_objective(self, name: str, function: agents.CriticalityFunction):
        self.__logger.info(f'Creating output "{name}".')
        output_agents = list(filter(lambda a: a.name in function.parameter_names,
                                    self.get_agents_for_type(agents.OutputAgent)))
        # TEMP ne fonctionne que pour une seule sortie
        self.add_agent(
            agents.ObjectiveAgent(name, function, 0, abs(output_agents[0].sup), *output_agents))

    def add_agent(self, agent: agents.Agent):
        if not agent.world:
            self.__agents_registry.append(agent)
            c = agent.__class__
            if c not in self.__global_ids:
                self.__global_ids[c] = 0
            self.__agents_id_registry[self.__global_ids[c]] = agent
            self.__global_ids[c] += 1
            agent.world = self

    def remove_agent(self, agent: agents.Agent):
        self.__agents_registry.remove(agent)
        del self.__agents_id_registry[agent.id]

    def setup(self):
        self.__logger.info('Setting up CALICOBA…')
        self.__output_agents = self.get_agents_for_type(agents.OutputAgent)
        self.__parameter_agents = self.get_agents_for_type(agents.ParameterAgent)
        self.__objective_agents = self.get_agents_for_type(agents.ObjectiveAgent)
        for ob in self.__objective_agents:
            ob.init()
        self.__cycle = 0
        self.__logger.info('CALICOBA setup finished.')

    def step(self):
        if self.__stop:
            return

        self.__logger.info(f'Cycle {self.__cycle}')

        for oa in self.__output_agents:
            oa.perceive()
        for ob in self.__objective_agents:
            ob.perceive()
        for ob in self.__objective_agents:
            ob.decide_and_act()
            self.__logger.debug(f'Obj {ob.name}: {ob.criticality}')
        for pa in self.__parameter_agents:
            pa.perceive()

        point_agents = self.get_agents_for_type(agents.PointAgent)

        for po in point_agents:
            po.decide_and_act()
            if po.dead:
                self.remove_agent(po)
        for pa in self.__parameter_agents:
            self.__logger.debug(f'Param {pa.name}: {pa.value}')
            pa.decide_and_act()

        input('Paused')  # TEST

        self.__cycle += 1

    def stop(self):
        self.__stop = True

    @property
    def stopped(self) -> bool:
        return self.__stop


__all__ = [
    'Calicoba',
    'CalicobaConfig',
]
