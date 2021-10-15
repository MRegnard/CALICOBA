import math

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
            {f'p{i + 1}': (-40, 40) for i in range(dimensions)},
            {'o1': (0, 25)}
        )
        self._a = a
        self._b = b
        self._c = c

    def _evaluate(self, **params: float):
        d = len(self.parameters_names)
        e1 = -self._b * math.sqrt(sum([params[k] ** 2 for k in params]) / d)
        e2 = sum([math.cos(self._c * params[k]) for k in params]) / d
        return {
            'o1': -self._a * math.exp(e1) - math.exp(e2) + self._a + math.exp(1),
        }


class SimpleModelsFactory(_model.ModelFactory):
    __models = {
        'model_1': Model1,
        'model_2': Model2,
        'model_3': Model3,
        'model_4': Model4,
        'model_5': Model5,
        'gramacy_and_lee_2012': ModelGramacyAndLee2012,
        'ackley_function': AckleyFunction,
    }

    def generate_model(self, model_id: str, *args, **kwargs):
        return self.__models[model_id](*args, **kwargs)


__all__ = [
    'SimpleModelsFactory',
]
