from __future__ import annotations

import math
import typing as typ

VT = typ.TypeVar('VT', int, float)
VT_ = typ.TypeVar('VT_', int, float)
Number = typ.Union[int, float]


class Vector(typ.Generic[VT]):
    """A class that represents an immutable vector of numbers, each associated to a unique name."""

    @staticmethod
    def zero(*names: str) -> Vector[int]:
        """Creates a vector filled with zeroes.

        :param names: List of names for each value.
        :returns: A new vector.
        """
        return Vector.filled(0, *names)

    @staticmethod
    def filled(value: VT_, *names: str) -> Vector[VT_]:
        """Creates a vector filled with the given value.

        :param value: The value to fill the vector with.
        :param names: List of names for each value.
        :returns: A new vector.
        """
        return Vector(**{name: value for name in names})

    def __init__(self, **values: VT):
        """Creates a vector with the given values.

        :param values: The values and names to use to fill this vector.
        """
        if not values:
            raise ValueError('no values specified')
        self._values_map = values

    def is_zero(self) -> bool:
        return all(v == 0 for v in self.values())

    def names(self) -> typ.KeysView[str]:
        """Returns a set-like object providing a view on this vector’s names."""
        return self._values_map.keys()

    def values(self) -> typ.ValuesView[VT]:
        """Returns a set-like object providing a view on this vector’s values."""
        return self._values_map.values()

    def entries(self) -> typ.ItemsView[str, VT]:
        """Returns a set-like object providing a view on this vector’s entries."""
        return self._values_map.items()

    def distance(self, other: Vector[Number], topology: Vector[Number]) -> float:
        """Returns the distance between this vector and the provided one.
        Distances in each dimension is normalize using the coefficient found in the second argument.

        :param other: Other vector to get the distance to.
        :param topology: Positive coefficients to normalize each dimension with.
        :returns: The distance between this vector and the specified one.
        """
        self._ensure_same_names(other)
        self._ensure_same_names(topology)
        return math.sqrt(sum(((v2 - v1) / topology[vname1]) ** 2 for (vname1, v1), (vname2, v2) in zip(self, other)))

    def map(self, f: typ.Callable[[str, VT], VT_]) -> Vector[VT_]:
        """Returns a new vector that is the result of applying the given function to each entry of this vector.

        :param f: A function to apply to each entry of this vector and returns a new value.
        :returns: A new vector.
        """
        return Vector(**{k: f(k, v) for k, v in self})

    def __abs__(self) -> Vector[VT]:
        return self.map(lambda _, v: abs(v))

    def __neg__(self) -> Vector[VT]:
        return self.map(lambda _, v: -v)

    def __add__(self, other: Vector[Number]) -> Vector[Number]:
        self._ensure_same_names(other)
        return self.map(lambda k, v: v + other._values_map[k])

    def __sub__(self, other: Vector[Number]) -> Vector[Number]:
        self._ensure_same_names(other)
        return self.map(lambda k, v: v - other._values_map[k])

    def __pow__(self, power: Number, modulo: Number = None):
        return self.map(lambda _, v: (v ** power) if modulo is None else ((v ** power) % modulo))

    def __mul__(self, x: Number) -> Vector[Number]:
        return self.map(lambda _, v: v * x)

    def __rmul__(self, x: Number) -> Vector[Number]:
        return self * x

    def __matmul__(self, other: Vector[Number]) -> Vector[Number]:
        """Performs an element-wise multiplication operation between two Vectors."""
        self._ensure_same_names(other)
        return self.map(lambda k, v: v * other._values_map[k])

    def __mod__(self, x: Number) -> Vector[Number]:
        return self.map(lambda _, v: v % x)

    def __truediv__(self, x: Number) -> Vector[float]:
        return self.map(lambda _, v: v / x)

    def __floordiv__(self, x: Number) -> Vector[int]:
        return self.map(lambda _, v: v // x)

    def __eq__(self, other: Vector[Number]):
        return self._values_map == other._values_map

    def __contains__(self, item: str):
        return item in self._values_map

    def __getitem__(self, item: str) -> VT:
        return self._values_map[item]

    def __iter__(self) -> typ.Iterator[typ.Tuple[str, VT]]:
        for k, v in self._values_map.items():
            yield k, v

    def __dict__(self) -> typ.Dict[str, VT]:
        return dict(self._values_map)

    def __len__(self) -> int:
        return len(self._values_map)

    def __repr__(self):
        return f'Vector({", ".join(f"{k}={v}" for k, v in sorted(self.entries()))})'

    def _ensure_same_names(self, other: Vector[Number]):
        if self.names() != other.names():
            raise ValueError(f'names mismatch between vectors: {self} and {other}')
