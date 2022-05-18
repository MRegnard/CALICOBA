import dataclasses
import logging
import math
import pathlib
import random
import typing as typ

from . import agents

_T = typ.TypeVar('_T', bound=agents.Agent)


@dataclasses.dataclass(frozen=True)
class CoBOptiConfig:
    """Configuration object for CoBOpti."""
    dump_directory: pathlib.Path = None
    seed: typ.Optional[int] = None
    logging_level: int = logging.INFO


# TODO réintégrer le controle du modèle dans CoBOpti
class CoBOpti:
    """CoBOpti is a global optimizer based on a self-adaptive multi-agent system."""

    def __init__(self, config: CoBOptiConfig):
        """Creates a new optimizer instance with the given configuration.

        :param config: The configuration object.
        """
        self._config = config
        self._logger = logging.getLogger('CoBOpti')
        self._logger.setLevel(self._config.logging_level)
        self._rng = random.Random(self._config.seed)
        if self._config.dump_directory and not self._config.dump_directory.exists():
            self._config.dump_directory.mkdir(parents=True)
        self._cycle = 0
        self._agents_registry: typ.List[agents.Agent] = []
        self._parameter_agents: typ.List[agents.VariableAgent] = []
        self._objective_agents: typ.List[agents.ObjectiveAgent] = []
        self._create_new_chain_for_params = set()
        self._search_phases: typ.Dict[str, agents.SearchPhase] = {}

    @property
    def config(self) -> CoBOptiConfig:
        """The configuration object."""
        return self._config

    @property
    def rng(self) -> random.Random:
        """The internal random number generator."""
        return self._rng

    @property
    def cycle(self) -> int:
        """The current search cycle. Starts at 0."""
        return self._cycle

    def get_agents_for_type(self, type_: typ.Type[_T]) -> typ.Sequence[_T]:
        """Returns all agents that match the given type.

        :param type_: Type of agents to fetch.
        :return: A list of all matching agents.
        """
        return list(filter(lambda a: isinstance(a, type_), self._agents_registry))

    def get_agent(self, predicate: typ.Callable[[agents.Agent], bool]) -> typ.Optional[agents.Agent]:
        """Return the first agent that matches the given predicate.

        :param predicate: Only agents for which this predicate returns true will be selected.
        :return: The first agent to match or None if none matched.
        """
        return next(filter(predicate, self._agents_registry), None)

    def add_parameter(self, name: str, lower: float, upper: float):
        """Declares a variable to optimize.

        :param name: Variable’s name.
        :param lower: Variable’s lower bound.
        :param upper: Variable’s upper bound.
        """
        self._logger.info(f'Creating parameter "{name}".')
        self.add_agent(agents.VariableAgent(name, lower, upper, logger=self._logger))

    def add_objective(self, name: str, lower: float, upper: float):
        """Declares an objective function.

        :param name: Objective’s name.
        :param lower: Objective’s lower bound.
        :param upper: Objective’s upper bound.
        """
        self._logger.info(f'Creating objective "{name}".')
        self.add_agent(agents.ObjectiveAgent(name, lower, upper))

    def add_agent(self, agent: agents.Agent):
        """Adds an agent to the environment managed by this optimizer.
        Sets the agent’s world property to this instance.

        :param agent: The agent to add.
        :raise ValueError: If the agent is already registered in this world.
        """
        if agent in self._agents_registry:
            raise ValueError(f'agent {agent.name} already registered in this instance')
        self._agents_registry.append(agent)
        agent.world = self

    def remove_agent(self, agent: agents.Agent):
        """Removes the given agent from the environment managed by this optimizer.

        :param agent: The agent to remove.
        """
        self._agents_registry.remove(agent)

    def setup(self):
        """Sets up this instance. Must be called before suggest_new_point method."""
        self._logger.info('Setting up CoBOpti…')
        self._parameter_agents = self.get_agents_for_type(agents.VariableAgent)
        self._objective_agents = self.get_agents_for_type(agents.ObjectiveAgent)
        self._cycle = 0
        with (self._config.dump_directory / 'feedback.csv').open(mode='w') as f:
            f.write(f'cycle,{",".join(sorted(a.name for a in self._parameter_agents))}\n')
        self._logger.info('CoBOpti setup finished.')

    def suggest_new_point(self, variables_values: typ.Dict[str, float], objectives_values: typ.Dict[str, float]) \
            -> typ.Dict[str, typ.List[agents.VariationSuggestion]]:
        """Suggests the next value for each one of the declared variables.

        :param variables_values: Current variable values.
        :param objectives_values: Current objectives values.
        :return: A dictionary that associates a list of suggestions for each declared variable.
        """
        self._logger.debug(f'Cycle {self._cycle}')

        # Update criticalities
        crits = {}
        for objective in self._objective_agents:
            objective.perceive(self._cycle, objectives_values[objective.name], dump_dir=self.config.dump_directory)
            crits[objective.name] = objective.criticality
            self._logger.debug(f'Obj {objective.name}: {objective.criticality}')

        # Update parameters, current points, and directions
        suggestions: typ.Dict[str, typ.List[agents.VariationSuggestion]] = {}
        with (self._config.dump_directory / 'feedback.csv').open(mode='a') as f:
            line = []
            for parameter in self._parameter_agents:
                p_name = parameter.name
                suggestions[p_name] = []
                diff = variables_values[p_name] - parameter.value
                last_step = abs(diff) if not math.isnan(diff) else parameter.default_step
                if diff > 0:
                    last_direction = agents.DIR_INCREASE
                elif diff < 0:
                    last_direction = agents.DIR_DECREASE
                else:
                    last_direction = agents.DIR_NONE
                new_chain = p_name in self._create_new_chain_for_params
                parameter.perceive(variables_values[p_name], new_chain, crits, last_step, last_direction,
                                   self._search_phases[p_name])
                line.append(f'{last_direction}/{diff}')
            f.write(f'{self._cycle},' + ','.join(map(str, line)) + '\n')
        self._create_new_chain_for_params.clear()

        # Update chain agents
        for chain in self.get_agents_for_type(agents.ChainAgent):
            chain.perceive()

        # Update point agents
        point_agents = self.get_agents_for_type(agents.PointAgent)
        for point in point_agents:
            point.perceive()

        # Let point agents decide where to go next
        for point in point_agents:
            suggestion = point.decide()
            if point.dead:
                self.remove_agent(point)
            elif suggestion:
                if isinstance(suggestion, agents.VariationSuggestion) and suggestion.new_chain_next:
                    self._create_new_chain_for_params.add(point.variable_name)
                suggestions[point.variable_name].append(suggestion)

        for parameter in self._parameter_agents:
            self._search_phases[parameter.name] = suggestions[parameter.name][0].search_phase

        self._cycle += 1

        return suggestions


__all__ = [
    'CoBOpti',
    'CoBOptiConfig',
]
