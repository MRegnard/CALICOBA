import abc
import typing as typ


class ObjectiveFunction(abc.ABC):
    def __init__(self, *outputs_names: str):
        self._outputs_names = outputs_names

    @property
    def outputs_names(self) -> typ.Tuple[str]:
        return self._outputs_names

    @abc.abstractmethod
    def __call__(self, **outputs_values: float) -> float:
        pass
