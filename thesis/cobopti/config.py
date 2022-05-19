import abc
import dataclasses
import logging
import pathlib
import typing as typ


class Metadata(abc.ABC):
    def __init__(self, name: str, lower_bound: float, upper_bound: float):
        """Base class that represents an input or output of a model to optimize.

        :param name: Element’s name.
        :param lower_bound: Lowest possible value.
        :param upper_bound: Highest possible value.
        :raise ValueError: If lower bound ≥ upper bound.
        """
        self._name = name
        self._lower_bound = lower_bound
        self._upper_bound = upper_bound

        if self.lower_bound >= self.upper_bound:
            raise ValueError('lower bound is greater than upper bound')

    @property
    def name(self) -> str:
        return self._name

    @property
    def lower_bound(self) -> float:
        return self._lower_bound

    @property
    def upper_bound(self) -> float:
        return self._upper_bound


class Variable(Metadata):
    """This class represents all relevent metadata about a decision variable."""


class ObjectiveFunction(Metadata):
    """This abstract class represents an objective function used internally by CoBOpti."""

    @abc.abstractmethod
    def __call__(self, **kwargs) -> float:
        """Evaluates this objective function.

        :param kwargs: Values to evaluate the function on.
        :return: The objective’s value.
        """


@dataclasses.dataclass(frozen=True)
class CoBOptiConfig:
    """Configuration object for CoBOpti."""
    variables_metadata: typ.List[Variable]
    objective_functions: typ.List[ObjectiveFunction]
    max_cycles: int
    step_by_step: bool = False
    expected_solutions: typ.Sequence[typ.Dict[str, float]] = None
    output_directory: typ.Optional[pathlib.Path] = None
    seed: typ.Optional[int] = None
    logging_level: int = logging.INFO
