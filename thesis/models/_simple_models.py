import math

import scipy.optimize

from . import _model


class Model1(_model.Model):
    def __init__(self):
        super().__init__(
            'model_1',
            {'p1': (-1500, 1500)},
            {'o1': (0, 1512)}
        )

    def _evaluate(self, p1: float):
        return {
            'o1': abs(p1 - 12),
        }


class Model2(_model.Model):
    def __init__(self):
        super().__init__(
            'model_2',
            {
                'p1': (-1500, 1500),
                'p2': (-1500, 1500),
            },
            {
                'o1': (-23, 1977),
                'o2': (-1512, 1488),
            }
        )

    def _evaluate(self, p1: float, p2: float):
        return {
            'o1': abs(p1 - p2) - 23,
            'o2': p2 - 12,
        }


class Model3(_model.Model):
    def __init__(self):
        super().__init__(
            'model_3',
            {
                'p1': (-1500, 1500),
                'p2': (-1500, 1500),
            },
            {
                'o1': (-1516, 2_251_484),
                'o2': (-1512, 1488),
            }
        )

    def _evaluate(self, p1: float, p2: float):
        return {
            'o1': p1 ** 2 + p2 - 16,
            'o2': p2 - 12,
        }


class Model4(_model.Model):
    def __init__(self):
        super().__init__(
            'model_4',
            {
                'p1': (-1500, 1500),
                'p2': (-1500, 1500),
            },
            {
                'o1': (-2999, 3001),
                'o2': (-3001, 2999),
                'o3': (-1520, 1580),
            }
        )

    def _evaluate(self, p1: float, p2: float):
        return {
            'o1': p1 + p2 + 1,
            'o2': p1 + p2 - 1,
            'o3': p2 - 20,
        }


class Model5(_model.Model):
    def __init__(self):
        super().__init__(
            'model_5',
            {'p1': (-3, 2)},
            {'o1': (-12, 63)}
        )

    def _evaluate(self, p1: float):
        return {
            'o1': -30 * p1 + 10 * p1 ** 3 - p1 ** 5,
        }


class SquareFunction(_model.Model):
    def __init__(self):
        super().__init__(
            'square',
            {'p1': (-100, 100)},
            {'o1': (0, 10_000)}
        )

    def _evaluate(self, p1: float):
        return {
            'o1': p1 ** 2
        }


class ModelGramacyAndLee2012(_model.Model):
    def __init__(self):
        super().__init__(
            'gramacy_and_lee_2012',
            {'p1': (0.5, 2.5)},
            {'o1': (-1, 5.1)}
        )

    def _evaluate(self, p1: float):
        return {
            'o1': (math.sin(10 * math.pi * p1) / (2 * p1)) + (p1 - 1) ** 4,
        }


class AckleyFunction(_model.Model):
    def __init__(self, dimensions: int = 1, a: float = 20, b: float = 0.2, c: float = 2 * math.pi):
        super().__init__(
            'ackley_function',
            {f'p{i + 1}': (-32, 32) for i in range(dimensions)},
            {'o1': (0, 25)}
        )
        self._a = a
        self._b = b
        self._c = c

    def _evaluate(self, **params: float):
        d = len(self.parameters_names)
        e1 = -self._b * math.sqrt(sum([p ** 2 for p in params.values()]) / d)
        e2 = sum([math.cos(self._c * p) for p in params.values()]) / d
        return {
            'o1': -self._a * math.exp(e1) - math.exp(e2) + self._a + math.exp(1),
        }


class LevyFunction(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'levy_function',
            {f'p{i + 1}': (-10, 10) for i in range(dimensions)},
            {'o1': (0, 100)}
        )

    def _evaluate(self, **params: float):
        π = math.pi
        d = len(self.parameters_names)
        w = [1 + (p - 1) / 4 for _, p in sorted(params.items(), key=lambda e: int(e[0][1:]))]
        a = math.sin(π * w[0]) ** 2
        b = sum(((wi - 1) ** 2) * (1 + 10 * math.sin(π * wi + 1) ** 2) for wi in w)
        c = ((w[d - 1] - 1) ** 2) * (1 + math.sin(2 * π * w[d - 1]) ** 2)
        return {
            'o1': a + b + c,
        }


class RastriginFunction(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'rastrigin_function',
            {f'p{i + 1}': (-5.12, 5.12) for i in range(dimensions)},
            {'o1': (0, 90)}
        )

    def _evaluate(self, **params: float):
        d = len(self.parameters_names)
        return {
            'o1': 10 * d + sum([p ** 2 - 10 * math.cos(2 * math.pi * p) for p in params.values()]),
        }


class LangermannFunction(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'langermann_function',
            {f'p{i + 1}': (0, 10) for i in range(dimensions)},
            {'o1': (-6, 6)}
        )
        self.__m = 5
        self.__c = (1, 2, 5, 2, 3)
        self.__a = ((3,), (5,), (2,), (1,), (7,))

    def _evaluate(self, **params: float):
        π = math.pi
        x = [p for _, p in sorted(params.items(), key=lambda e: int(e[0][1:]))]
        d = len(x)

        def f(i: int) -> float:
            return sum((x[j] - self.__a[i][j]) ** 2 for j in range(d))

        return {
            'o1': sum(self.__c[i] * math.exp(-f(i) / π) * math.cos(π * f(i)) for i in range(self.__m)),
        }


class StyblinskiTangFunction(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'styblinski_tang_function',
            {f'p{i + 1}': (-5, 5) for i in range(dimensions)},
            {'o1': (-100, 250)}
        )

    def _evaluate(self, **params: float):
        return {
            'o1': sum((p ** 4) - (16 * p ** 2) + (5 * p) for p in params.values()) / 2,
        }


class RosenbrockFunction(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'rosenbrock_function',
            {f'p{i + 1}': (-5, 10) for i in range(dimensions)},
            {'o1': (0, 18e4)}
        )

    def _evaluate(self, **kwargs: float):
        params = [v for k, v in sorted(kwargs.items(), key=lambda e: int(e[0][1:]))]
        return {
            'o1': scipy.optimize.rosen(params),
        }


class SimpleModelsFactory(_model.ModelFactory):
    __models = {
        'model_1': Model1,
        'model_2': Model2,
        'model_3': Model3,
        'model_4': Model4,
        'model_5': Model5,
        'square': SquareFunction,
        'gramacy_and_lee_2012': ModelGramacyAndLee2012,
        'ackley_function': AckleyFunction,
        'levy_function': LevyFunction,
        'rastrigin_function': RastriginFunction,
        'rosenbrock_function': RosenbrockFunction,
        'langermann_function': LangermannFunction,
        'styblinski_tang_function': StyblinskiTangFunction,
    }

    def generate_model(self, model_id: str, *args, **kwargs):
        return self.__models[model_id](*args, **kwargs)


__all__ = [
    'SimpleModelsFactory',
]
