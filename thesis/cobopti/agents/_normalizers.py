import abc


class Normalizer(abc.ABC):
    """A normalizer is a function that normalizes a value between bounds."""

    @abc.abstractmethod
    def __call__(self, value: float) -> float:
        pass


class AllTimeAbsoluteNormalizer(Normalizer):
    def __init__(self):
        """A normalizer that uses the all-time absolute max of the passed values."""
        self.__max = 0

    def __call__(self, value):
        if abs(value) > self.__max:
            self.__max = abs(value)
        return value if self.__max == 0 else (value / self.__max)


class SlidingNormalizer(Normalizer):
    def __init__(self, window_size: int):
        """A normalizer that uses the maximum and minimum of the last n values.

        :param window_size: The number of values to keep in memory.
        """
        self.__window_size = window_size
        self.__previous_values = []

    def __call__(self, value):
        self.__previous_values.append(value)
        if len(self.__previous_values) > self.__window_size:
            self.__previous_values.pop(0)
        maxi = max(self.__previous_values)
        mini = min(self.__previous_values)
        return (value - mini) / (maxi - mini)


class BoundNormalizer(Normalizer):
    def __init__(self, inf: float, sup: float):
        """A normalizer that uses the given lower and upper bounds.

        :param inf: The lower bound.
        :param sup: The upper bound.
        """
        self.__inf = inf
        self.__sup = sup

    def __call__(self, value):
        return abs(value - self.__inf) / abs(self.__sup - self.__inf)
