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


class ModelGramacyAndLee2012(_model.Model):
    def __init__(self):
        super().__init__(
            'gc',
            {'p1': (0.5, 2.5)},
            {'o1': (-0.87, 5.1)}
        )

    def _evaluate(self, p1: float):
        return {
            'o1': (math.sin(10 * math.pi * p1) / (2 * p1)) + (p1 - 1) ** 4,
        }


class AckleyFunction(_model.Model):
    def __init__(self, dimensions: int = 1, a: float = 20, b: float = 0.2, c: float = 2 * math.pi):
        super().__init__(
            'ackley',
            {f'p{i + 1}': (-32, 32) for i in range(dimensions)},
            {'o1': (0, 25)}
        )
        self._a = a
        self._b = b
        self._c = c

    def _evaluate(self, **params: float):
        d = len(self.parameters_names)
        e1 = -self._b * math.sqrt(sum(p ** 2 for p in params.values()) / d)
        e2 = sum(math.cos(self._c * p) for p in params.values()) / d
        return {
            'o1': -self._a * math.exp(e1) - math.exp(e2) + self._a + math.exp(1),
        }


class LevyFunction(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'levy',
            {f'p{i + 1}': (-10, 10) for i in range(dimensions)},
            {'o1': (0, 100 if dimensions == 1 else 175)}
        )

    def _evaluate(self, **params: float):
        d = len(self.parameters_names)
        w = [1 + (p - 1) / 4 for _, p in sorted(params.items(), key=lambda e: int(e[0][1:]))]
        a = math.sin(math.pi * w[0]) ** 2
        b = sum(((wi - 1) ** 2) * (1 + 10 * math.sin(math.pi * wi + 1) ** 2) for wi in w)
        c = ((w[d - 1] - 1) ** 2) * (1 + math.sin(2 * math.pi * w[d - 1]) ** 2)
        return {
            'o1': a + b + c,
        }


class RastriginFunction(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'rastrigin',
            {f'p{i + 1}': (-5.12, 5.12) for i in range(dimensions)},
            {'o1': (0, 90)}
        )

    def _evaluate(self, **params: float):
        d = len(self.parameters_names)
        return {
            'o1': 10 * d + sum(p ** 2 - 10 * math.cos(2 * math.pi * p) for p in params.values()),
        }


class LangermannFunction(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'langermann',
            {f'p{i + 1}': (0, 10) for i in range(dimensions)},
            {'o1': (-6, 6)}
        )
        self.__m = 5
        self.__c = (1, 2, 5, 2, 3)
        self.__a = ((3, 5), (5, 2), (2, 1), (1, 4), (7, 9))

    def _evaluate(self, **params: float):
        x = [p for _, p in sorted(params.items(), key=lambda e: int(e[0][1:]))]
        d = len(x)

        def f(i: int) -> float:
            return sum((x[j] - self.__a[i][j]) ** 2 for j in range(d))

        return {
            'o1': sum(self.__c[i] * math.exp(-f(i) / math.pi) * math.cos(math.pi * f(i)) for i in range(self.__m)),
        }


class StyblinskiTangFunction(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'styblinski_tang',
            {f'p{i + 1}': (-5, 5) for i in range(dimensions)},
            {'o1': (-100, 250)}
        )

    def _evaluate(self, **params: float):
        return {
            'o1': sum((p ** 4) - (16 * p ** 2) + (5 * p) for p in params.values()) / 2,
        }


class RosenbrockFunction(_model.Model):
    def __init__(self):
        super().__init__(
            'rosenbrock',
            {
                'p1': (-2, 2),
                'p2': (-1, 3),
            },
            {'o1': (0, 2500)}
        )

    def _evaluate(self, **params: float):
        values = [v for _, v in sorted(params.items(), key=lambda e: int(e[0][1:]))]
        return {
            'o1': scipy.optimize.rosen(values),
        }


class WeierstrassFunction(_model.Model):
    def __init__(self, a: float = 0.5, b: float = 3, precision: int = 10):
        if not (0 < a < 1):
            raise ValueError('a should be in [0, 1]')
        if b <= 0 or b % 2 == 0:
            raise ValueError('b should be a positive odd integer')
        super().__init__(
            'weierstrass',
            {'p1': (0, 2)},
            {'o1': (-1.998046875, 1.998046875)}  # Values for a = 0.5 and b = 3 and precision = 10
        )
        self._a = a
        self._b = b
        self._precision = precision

    def _evaluate(self, p1: float):
        return {
            'o1': sum(
                (self._a ** n) * math.cos((self._b ** n) * math.pi * p1)
                for n in range(self._precision)
            )
        }


class MultiObj(_model.Model):
    def __init__(self):
        super().__init__(
            'multi_obj',
            {'p1': (0.5, 2.5)},
            {
                'o1': (-0.87, 5.1),
                'o2': (3.55, 10.22),
                'o3': (1, 26.25),
                'o4': (0, 2.1534276983111598),
            }
        )

    def _evaluate(self, **params: float):
        p1 = params[self.parameters_names[0]]
        d = len(self.parameters_names)

        a1 = 20
        b1 = 0.2
        c1 = 2 * math.pi

        e1 = -b1 * math.sqrt(sum(p ** 2 for p in params.values()) / d)
        e2 = sum(math.cos(c1 * p) for p in params.values()) / d

        w = [1 + (p - 1) / 4 for _, p in sorted(params.items(), key=lambda e: int(e[0][1:]))]
        a2 = math.sin(math.pi * w[0]) ** 2
        b2 = sum(((wi - 1) ** 2) * (1 + 10 * math.sin(math.pi * wi + 1) ** 2) for wi in w)
        c2 = ((w[d - 1] - 1) ** 2) * (1 + math.sin(2 * math.pi * w[d - 1]) ** 2)

        return {
            'o1': (math.sin(10 * math.pi * p1) / (2 * p1)) + (p1 - 1) ** 4,  # Gramacy & Lee
            'o2': -a1 * math.exp(e1) - math.exp(e2) + a1 + math.exp(1),  # Ackley
            'o3': 10 * d + sum(p ** 2 - 10 * math.cos(2 * math.pi * p) for p in params.values()),  # Rastrigin
            'o4': a2 + b2 + c2,  # Levy
        }


class ZDT1(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'ZDT1',
            {f'p{i + 1}': (0, 1) for i in range(dimensions)},
            {
                'f1': (0, 1),
                'f2': (0, 1),  # FIXME for 2D
            }
        )
        self._d = dimensions

    def _evaluate(self, **params: float):
        x1 = params['p1']
        f1 = x1
        g = 1 + 9 / 29 * sum(params['p' + str(i + 1)] for i in range(1, self._d))
        h = 1 - math.sqrt(f1 / g)
        f2 = g * h
        return {
            'f1': f1,
            'f2': f2,
        }


class ZDT3(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'ZDT3',
            {f'p{i + 1}': (0, 1) for i in range(dimensions)},
            {
                'f1': (0, 1),
                'f2': (-0.8, 1),  # FIXME for 2D
            }
        )
        self._d = dimensions

    def _evaluate(self, **params: float):
        x1 = params['p1']
        f1 = x1
        g = 1 + 9 / 29 * sum(params['p' + str(i + 1)] for i in range(1, self._d))
        h = 1 - math.sqrt(f1 / g) - (f1 / g) * math.sin(10 * math.pi * f1)
        f2 = g * h
        return {
            'f1': f1,
            'f2': f2,
        }


class ZDT6(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'ZDT6',
            {f'p{i + 1}': (0, 1) for i in range(dimensions)},
            {
                'f1': (0, 1),  # FIXME for 2D
                'f2': (0, 1),  # FIXME for 2D
            }
        )
        self._d = dimensions

    def _evaluate(self, **params: float):
        x1 = params['p1']
        f1 = 1 - math.exp(-4 * x1) * math.sin(6 * math.pi * x1) ** 6
        g = 1 + 9 * (sum(params['p' + str(i + 1)] for i in range(1, self._d)) / 9) ** 0.25
        h = 1 - (f1 / g) ** 2
        f2 = g * h
        return {
            'f1': f1,
            'f2': f2,
        }


class ViennetFunction(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'viennet',
            {f'p{i + 1}': (-3, 3) for i in range(dimensions)},
            {
                'f1': (0, 5.06),  # FIXME for 2D
                'f2': (15, 36.717592592592595),  # FIXME for 2D
                'f3': (-0.1, 0.2),  # FIXME for 2D
            }
        )
        self._d = dimensions

    def _evaluate(self, **params: float):
        x = params['p1']
        y = params['p2'] if self._d == 2 else 0
        return {
            'f1': 0.5 * (x ** 2 + y ** 2) + math.sin(x ** 2 + y ** 2),
            'f2': ((3 * x - 2 * y + 4) ** 2) / 8 + ((x - y + 1) ** 2) / 27 + 15,
            'f3': 1 / (x ** 2 + y ** 2 + 1) - 1.1 * math.exp(-(x ** 2 + y ** 2)),
        }


class HimmelblauFunction(_model.Model):
    def __init__(self):
        super().__init__(
            'himmelblau',
            {
                f'p1': (-6, 6),
                f'p2': (-6, 6),
            },
            {
                'o1': (0, 2200),
            }
        )

    def _evaluate(self, **params: float):
        x = params['p1']
        y = params['p2']
        return {
            'o1': (x ** 2 + y - 11) ** 2 + (x + y ** 2 - 7) ** 2,
        }


class GLOffset(_model.Model):
    def __init__(self):
        super().__init__(
            'gl_offset',
            {'p1': (0.5, 2.5)},
            {'o1': (-5, 5.1)}
        )

    def _evaluate(self, p1: float):
        return {
            'o1': (math.sin(10 * math.pi * p1) / (2 * p1)) + (p1 - 1) ** 4,
        }


class AckleyOffset(_model.Model):
    def __init__(self, dimensions: int = 1, a: float = 20, b: float = 0.2, c: float = 2 * math.pi):
        super().__init__(
            'ackley_offset',
            {f'p{i + 1}': (-32, 32) for i in range(dimensions)},
            {'o1': (-10, 25)}
        )
        self._a = a
        self._b = b
        self._c = c

    def _evaluate(self, **params: float):
        d = len(self.parameters_names)
        e1 = -self._b * math.sqrt(sum(p ** 2 for p in params.values()) / d)
        e2 = sum(math.cos(self._c * p) for p in params.values()) / d
        return {
            'o1': -self._a * math.exp(e1) - math.exp(e2) + self._a + math.exp(1),
        }


class RastriginOffset(_model.Model):
    def __init__(self, dimensions: int = 1):
        super().__init__(
            'rastrigin_offset',
            {f'p{i + 1}': (-5.12, 5.12) for i in range(dimensions)},
            {'o1': (-30, 90)}
        )

    def _evaluate(self, **params: float):
        d = len(self.parameters_names)
        return {
            'o1': 10 * d + sum(p ** 2 - 10 * math.cos(2 * math.pi * p) for p in params.values()),
        }


class Circles(_model.Model):
    def __init__(self):
        super().__init__(
            'circles',
            {
                'p1': (-2, 4),
                'p2': (-2, 4),
            },
            {
                'o1': (0, 32),
            }
        )

    def _evaluate(self, p1: float, p2: float):
        return {
            'o1': p1 ** 2 + p2 ** 2,
        }


class SimpleModelsFactory(_model.ModelFactory):
    __models = {
        # Mono-objective mono-variable
        'model_1': Model1,
        'model_5': Model5,
        'gc': ModelGramacyAndLee2012,
        'ackley': AckleyFunction,
        'levy': LevyFunction,
        'rastrigin': RastriginFunction,
        'weierstrass': WeierstrassFunction,
        'langermann': LangermannFunction,
        'styblinski_tang': StyblinskiTangFunction,
        'gl_offset': GLOffset,
        'ackley_offset': AckleyOffset,
        'rastrigin_offset': RastriginOffset,

        # Multi-objective mono-variable
        'viennet': ViennetFunction,
        'ZDT1': ZDT1,
        'ZDT3': ZDT3,
        'ZDT6': ZDT6,
        'multi_obj': MultiObj,

        # Mono-objective multi-variable
        'ackley_2d': lambda: AckleyFunction(dimensions=2),
        'levy_2d': lambda: LevyFunction(dimensions=2),
        'rastrigin_2d': lambda: RastriginFunction(dimensions=2),
        'langermann_2d': lambda: LangermannFunction(dimensions=2),
        'styblinski_tang_2d': lambda: StyblinskiTangFunction(dimensions=2),
        'himmelblau': HimmelblauFunction,
        'rosenbrock': RosenbrockFunction,

        # Multi-objective multi-variable
        'model_2': Model2,
        'model_3': Model3,
        'model_4': Model4,
        'viennet_2d': lambda: ViennetFunction(dimensions=2),
        'ZDT1_2d': lambda: ZDT1(dimensions=2),
        'ZDT3_2d': lambda: ZDT3(dimensions=2),
        'ZDT6_2d': lambda: ZDT6(dimensions=2),
        'circles': Circles,
    }

    def generate_model(self, model_id: str, *args, **kwargs):
        return self.__models[model_id](*args, **kwargs)


__all__ = [
    'SimpleModelsFactory',
]
