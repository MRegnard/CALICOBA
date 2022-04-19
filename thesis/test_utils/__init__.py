import math
import typing as typ

import numpy as np

Map = typ.Dict[str, float]


def sobol_to_param(v: float, mini: float, maxi: float) -> float:
    return v * (maxi - mini) + mini


def map_to_string(m: Map, sep: str = ';') -> str:
    return sep.join(f'{k}={v}' for k, v in m.items())


def desired_parameters(*values: float) -> Map:
    return {f'p{i + 1}': v for i, v in enumerate(values)}


def desired_outputs(*values: float) -> Map:
    return {f'o{i + 1}': v for i, v in enumerate(values)}


def gaussian_noise(mean: float = 0, stdev: float = 1):
    return np.random.normal(mean, stdev)


MODEL_SOLUTIONS = {
    'model_1': [desired_parameters(-12)],
    'model_2': [desired_parameters(-11, 12)],
    'model_3': [desired_parameters(2, 12)],
    # Partial solutions
    'model_4': [desired_parameters(-21, 20), desired_parameters(-19, 20)],
    'model_5': [desired_parameters(math.sqrt(3 - math.sqrt(3)))],
    'gc': [desired_parameters(0.548563)],
    'ackley': [desired_parameters(0)],
    'levy': [desired_parameters(1)],
    'rastrigin': [desired_parameters(0)],
    'weierstrass': [desired_parameters(1)],
    'rosenbrock': [desired_parameters(1, 1)],
    'styblinski_tang': [desired_parameters(-39.16599)],
    'multi_obj': [desired_parameters(0.95)],
    'himmelblau': [desired_parameters(3, 2), desired_parameters(-2.805118, 3.131312),
                   desired_parameters(-3.779310, -3.283186), desired_parameters(3.584428, -1.848126)],
    'ZDT1': [desired_parameters(0.38)],
    'ZDT3': [desired_parameters(0.433)],
    'ZDT6': [desired_parameters(0.248)],
    'viennet': [desired_parameters(-1 / 3)],
    'gl_offset': [desired_parameters(0.548563)],
    'ackley_offset': [desired_parameters(0)],
    'rastrigin_offset': [desired_parameters(0)],
    'ackley_2d': [desired_parameters(0, 0)],
    'rastrigin_2d': [desired_parameters(0, 0)],
}


class SobolSequence:
    """Implementation of a Sobol sequence as a iterator.

    A Sobol sequence is a low-discrepancy sequence with the property
    that for all values of N, its subsequence (x1, ... xN) has a low
    discrepancy. It can be used to generate pseudo-random points in
    a space S, which are equi-distributed.

    The implementation already comes with support for up to 1000
    dimensions with direction numbers calculated from Stephen Joe
    and Frances Kuo (https://web.maths.unsw.edu.au/~fkuo/sobol/).

    See:

    - https://en.wikipedia.org/wiki/Sobol_sequence
    - https://web.maths.unsw.edu.au/~fkuo/sobol/
    """
    BITS = 52
    MAX_DIMENSION = 1000
    SCALE = 2 ** BITS

    def __init__(self, dimension: int, points_nb: int):
        """Construct a new Sobol sequence generator for the given space dimension.

        :param dimension: The space dimension.
        :param points_nb: The maximum number of points to generate.
        """
        if not (1 <= dimension <= self.MAX_DIMENSION):
            raise ValueError(f'dimension should be in [1, {self.MAX_DIMENSION}]')
        self.__dimension = dimension
        self.__points_nb = points_nb
        self.__direction = [[0] * (self.BITS + 1) for _ in range(dimension)]
        self.__x = [0] * dimension
        self.__count = 0

        self.__init_from_file()

    def __init_from_file(self):
        """Loads the direction vector for each dimension from a file."""
        for i in range(1, self.BITS + 1):
            self.__direction[0][i] = 1 << (self.BITS - i)

        with open('test_utils/new-joe-kuo-6.1000', encoding='ascii') as f:
            index = 1
            for line in f.readlines()[1:]:
                parts = line.strip().split()
                dim = int(parts[0])
                if 2 <= dim <= self.__dimension:
                    s = int(parts[1])
                    a = int(parts[2])
                    m = [0, *(int(parts[i]) for i in range(1, s + 1))]
                    self.__init_direction_vector(index, a, m)
                    index += 1
                if dim > self.__dimension:
                    break

    def __init_direction_vector(self, d: int, a: int, m: typ.List[int]):
        """Calculate the direction numbers from the given polynomial.

        :param d: The dimension.
        :param a: The coefficients of the primitive polynomial.
        :param m: The initial direction numbers.
        :return:
        """
        s = len(m) - 1
        for i in range(1, s + 1):
            self.__direction[d][i] = m[i] << (self.BITS - i)
        for i in range(s + 1, self.BITS + 1):
            self.__direction[d][i] = self.__direction[d][i - s] ^ (self.__direction[d][i - s] >> s)
            for j in range(1, s):
                self.__direction[d][i] ^= ((a >> (s - 1 - j)) & 1) * self.__direction[d][i - j]

    def __iter__(self):
        return self

    def __next__(self) -> typ.Tuple[float]:
        """Calculates then returns the next point.

        :return: A tuple containing the "dimension" values.
        """
        if self.__count == self.__points_nb:
            raise StopIteration()

        v = [0.0] * self.__dimension
        if self.__count == 0:
            self.__count += 1
            return tuple(v)

        # Find the index c of the rightmost 0
        c = 1
        value = self.__count - 1
        while (value & 1) == 1:
            value >>= 1
            c += 1

        for i in range(self.__dimension):
            self.__x[i] ^= self.__direction[i][c]
            v[i] = self.__x[i] / self.SCALE
        self.__count += 1
        return tuple(v)


class PeriodDetector:
    def __init__(self, buffer_size: int):
        self.__buffer_size = buffer_size
        self.__buffer = []
        self.__needs_updating = True
        self.__periods_cache = []

    @property
    def is_full(self) -> bool:
        return len(self.__buffer) == self.__buffer_size

    @property
    def has_converged(self) -> bool:
        return any(d < 1e-5 for d, _ in self.get_period_distances())

    def append(self, value: float):
        if self.is_full:
            self.__buffer.pop(0)
        self.__buffer.append(value)
        self.__needs_updating = True

    def values(self) -> typ.List[float]:
        return list(self.__buffer)

    def get_period_distances(self) -> typ.Sequence[typ.Tuple[float, int]]:
        """Tries to find the period of the values in the buffer.

        Author: Otunba, R. and Lin, J., 2014. APT: Approximate Period Detection in Time Series. In SEKE (pp. 490-494).

        :return: A list of distance/period pairs.
        """
        if not self.__needs_updating:
            return self.__periods_cache

        periods = []
        period = 2
        distance = math.inf
        for i in range(2, (len(self.__buffer) // 2) + 1):
            current_distance = self.__euclidian_distance(self.__buffer, i, distance)
            if current_distance < distance:
                distance = current_distance
                if i == period + 1:
                    del periods[-1]
                period = i
                periods.append((distance, period))

        periods.sort(key=lambda p: p[0])
        self.__periods_cache = periods
        self.__needs_updating = False

        return tuple(periods)

    @staticmethod
    def __euclidian_distance(values: typ.Sequence[float], period: int, current_distance: float) -> float:
        """Computes the euclidian distance of the given series to a
        generated series of the given period. The second series is
        generated using the first "period" elements of the
        given series.

        Author: Otunba, R. and Lin, J., 2014. APT: Approximate Period Detection in Time Series. In SEKE (pp. 490-494).

        :param values: Series to use.
        :param period: Period of the series to generate.
        :param current_distance: Current minimal distance for early stopping.
        :return: The euclidian distance.
        """
        d = 0
        for i in range(len(values)):
            d += (values[i] - values[i % period]) ** 2
            if d >= current_distance ** 2:  # Optimization
                return current_distance
        return math.sqrt(d)
