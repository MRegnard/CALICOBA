import abc


class DataOutput(abc.ABC):
    def __init__(self, inf: float, sup: float, name: str):
        self._inf = inf
        self._sup = sup
        self._name = name

    @property
    def inf(self) -> float:
        return self._inf

    @property
    def sup(self) -> float:
        return self._sup

    @property
    def name(self) -> str:
        return self._name

    @abc.abstractmethod
    def get_data(self) -> float:
        pass


class DataInput(DataOutput, abc.ABC):
    @abc.abstractmethod
    def set_data(self, value: float):
        pass
