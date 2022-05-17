import abc
import typing as typ


class ObjectiveFunction(abc.ABC):
    """This abstract class represents an objective function used internally by CoBOpti."""

    def __init__(self, *outputs_names: str):
        """Creates an objective function.

        :param outputs_names: The list of model outputs that this objective function
         should aggregate into a single float value.
        """
        self._outputs_names = outputs_names

    @property
    def outputs_names(self) -> typ.Tuple[str]:
        """The list of model outputs that this objective function aggregates into a single float value."""
        return self._outputs_names

    @abc.abstractmethod
    def __call__(self, **outputs_values: float) -> float:
        """Evaluates this objective function on the given model outputs.

        :param outputs_values: The values of each model output.
        :return: The objective’s value.
        """
        pass
