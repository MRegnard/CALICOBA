import typing as _typ

from . import _procedural_models
from ._model import *
from ._simple_models import *

FACTORY_SIMPLE = 'simple'
FACTORY_PROCEDURAL = 'procedural'


def get_model_factory(factory_type: str, *args) -> _typ.Optional[ModelFactory]:
    return {
        FACTORY_SIMPLE: lambda: _simple_models.SimpleModelsFactory(),
        FACTORY_PROCEDURAL: lambda: _procedural_models.ProceduralModelFactory(*args),
    }[factory_type]()


__MODELS = {}
