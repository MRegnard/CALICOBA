import dataclasses
import json
import logging
import math
import random
import time
import typing as typ

from . import agents, config as cfg, _data_types as dt

_T = typ.TypeVar('_T', bound=agents.Agent)


@dataclasses.dataclass(frozen=True)
class OptimizationResult:
    solution: typ.Dict[str, float]
    cycles: int
    obj_evals: int
    chains: int
    time: float
    points: int
    unique_points: int
    error: bool
    error_message: typ.Optional[str] = None


def minimize(config: cfg.CoBOptiConfig) -> OptimizationResult:
    """Runs the optimizer.

    :return: An object containing the found solution and various metrics.
    """
    c = CoBOpti(config)
    return c.minimize()


class CoBOpti:
    """CoBOpti is a global optimizer based on a self-adaptive multi-agent system."""

    def __init__(self, config: cfg.CoBOptiConfig):
        """Creates a new optimizer instance with the given configuration.

        :param config: The configuration object.
        """
        self._config = config
        self._objectives = list(config.objective_functions)
        self._logger = logging.getLogger('CoBOpti')
        self._logger.setLevel(self._config.logging_level)
        self._rng = random.Random(self._config.seed)
        if self._config.output_directory and not self._config.output_directory.exists():
            self._config.output_directory.mkdir(parents=True)
        self._cycle = 0
        self._agents_registry: typ.List[agents.Agent] = []
        # noinspection PyTypeChecker
        self._registry_agent: agents.RegistryAgent = None
        self._objective_agents: typ.List[agents.ObjectiveAgent] = []
        self._create_new_chain_next = False
        self._search_phase: agents.SearchPhase = agents.SearchPhase.LOCAL
        self._x0 = dt.Vector(**{var.name: config.x0[var.name] for var in config.variables_metadata})
        self._expected_solutions = [dt.Vector(**s) for s in config.expected_solutions]
        self._variables = self._x0
        self._best_solution = None
        self._best_crits = None

        self._variables_data = {}
        self._objectives_data = {}

    @property
    def config(self) -> cfg.CoBOptiConfig:
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

    def _add_objective(self, function: cfg.ObjectiveFunction):
        """Declares an objective function.

        :param function: The objective function.
        """
        self._logger.info(f'Creating objective "{function.name}".')
        self.add_agent(agents.ObjectiveAgent(function))

    def _setup(self):
        """Sets up this instance. Must be called before minimize method."""
        self._logger.info('Setting up CoBOpti…')

        for variable in self.config.variables_metadata:
            if self.config.output_directory:
                self._variables_data[variable.name] = []

        self._registry_agent = agents.RegistryAgent(*self.config.variables_metadata, logger=self._logger)

        for obj in self.config.objective_functions:
            self._add_objective(obj)
            if self.config.output_directory:
                self._objectives_data[obj.name] = []

        self._objective_agents = self.get_agents_for_type(agents.ObjectiveAgent)

        self._cycle = 0

        self._logger.info('CoBOpti setup finished.')

    def minimize(self) -> OptimizationResult:
        """Runs the optimizer.

        :return: An object containing the found solution and various metrics.
        """
        self._setup()
        solution_found = False
        start_time = time.time()
        error_message = ''

        for _ in range(self.config.max_cycles):
            self._logger.debug(f'Cycle {self._cycle}')

            try:
                # Execute one minimization step
                suggestion = self._step()
            except BaseException as e:
                self._logger.exception(e)
                error_message = str(e)
                break
            self._logger.debug(suggestion)

            if self.config.output_directory:
                for var_name, v in suggestion.steps:
                    self._variables_data[var_name].append({
                        'cycle': self._cycle,
                        'value': self._variables[var_name],
                        'criticality': suggestion.criticality,
                        'decider': suggestion.agent.name,
                        'is_local_min': suggestion.agent.is_local_minimum,
                        'steps': suggestion.steps,
                        'decision': suggestion.decision,
                    })

            if suggestion.local_min_found:
                # Global minimum detection
                if suggestion.steps.is_zero():
                    solution_found = True
                else:
                    # TEMP until a better criterion has been defined
                    solution_found = any(
                        self._variables.distance(expected_solution) < agents.PointAgent.SAME_POINT_THRESHOLD
                        for expected_solution in self._expected_solutions
                    )

            # Update variables
            self._variables = suggestion.next_point

            self._cycle += 1
            if solution_found or error_message:
                break
            if self.config.step_by_step:
                input('Paused')

        total_time = time.time() - start_time
        # All objective agents are called for the exact same points, no need to check more than one
        all_points = self._objective_agents[0].points
        unique_points = []
        for p in all_points:
            if p not in unique_points:
                unique_points.append(p)

        if self.config.output_directory:
            for var_name in self._variables.names():
                with (self.config.output_directory / (var_name + '.json')).open(mode='w', encoding='utf8') as f:
                    json.dump(self._variables_data[var_name], f)
            for obj in self._objectives:
                with (self.config.output_directory / (obj.name + '.json')).open(mode='w', encoding='utf8') as f:
                    json.dump(self._objectives_data[obj.name], f)

        return OptimizationResult(
            solution=self._best_solution,
            cycles=self._cycle,
            obj_evals=sum(obj.evaluations for obj in self._objective_agents),
            chains=len(self._registry_agent.chains),
            time=total_time if not self.config.step_by_step else math.nan,
            points=len(all_points),
            unique_points=len(unique_points),
            error=error_message != '',
            error_message=error_message,
        )

    def _step(self) -> agents.VariationSuggestion:
        """Executes a single minimization cycle.

        :return: A single variation suggestion.
        """
        # Update objectives
        crits = {}
        for objective in self._objective_agents:
            objective.evaluate_function(**self._variables.as_dict(), update=True)
            crits[objective.name] = objective.criticality
            self._logger.debug(f'Obj {objective.name}: {objective.criticality}')
            if self.config.output_directory:
                self._objectives_data[objective.name].append({
                    'cycle': self._cycle,
                    'raw_value': objective.objective_value,
                    'criticality': objective.criticality,
                })

        crits = dt.Vector(**crits)
        # Update best solution
        if not self._best_crits or max(crits.values()) < max(self._best_crits.values()):
            self._best_solution = self._variables
            self._best_crits = crits

        self._logger.debug(self._variables, crits)

        # Update registry agent, current point, and directions
        if (previous_values := self._registry_agent.variables_values) is not None:
            diff = self._variables - previous_values
            last_steps = abs(diff)

            def m(v):
                if v > 0:
                    return agents.DIR_INCREASE
                elif v < 0:
                    return agents.DIR_DECREASE
                else:
                    return agents.DIR_NONE

            last_directions = diff.map(lambda _, v: m(v))
        else:
            last_steps = dt.Vector(**{
                vname: self._registry_agent.get_variable_default_step(vname)
                for vname in self._variables.names()
            })
            last_directions = dt.Vector.filled(agents.DIR_NONE, *self._variables.names())
        self._registry_agent.perceive(self._variables, self._create_new_chain_next, crits, last_steps,
                                      last_directions, self._search_phase)
        self._create_new_chain_next = False

        # Update chain agents
        for chain in self.get_agents_for_type(agents.ChainAgent):
            chain.perceive()

        # Update point agents
        point_agents = self.get_agents_for_type(agents.PointAgent)
        for point in point_agents:
            point.perceive()

        # Let point agents decide where to go next
        # noinspection PyTypeChecker
        suggestion: agents.VariationSuggestion = None
        for point in point_agents:
            point_suggestion = point.decide()
            if point.dead:
                self.remove_agent(point)
            elif point_suggestion:
                if suggestion:
                    if point_suggestion.priority > suggestion.priority:
                        self._logger.debug('found suggestion with higher priority')
                    else:
                        raise ValueError(f'several suggestions with same priority: {suggestion} vs {point_suggestion}')
                self._create_new_chain_next = point_suggestion.new_chain_next
                suggestion = point_suggestion

        if suggestion:
            self._search_phase = suggestion.search_phase
        else:
            raise ValueError(f'no suggestions')

        return suggestion


__all__ = [
    'CoBOpti',
]
