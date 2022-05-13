import abc
import typing as typ


class Model(abc.ABC):
    def __init__(self, ident: str, parameters_domains: typ.Dict[str, typ.Tuple[float, float]],
                 outputs_domains: typ.Dict[str, typ.Tuple[float, float]]):
        self.__id = ident
        self.__parameters = {k: 0 for k in parameters_domains.keys()}
        self.__outputs = {k: 0 for k in outputs_domains.keys()}
        self.__parameters_domains = dict(parameters_domains)
        self.__outputs_domains = dict(outputs_domains)

    @property
    def id(self) -> str:
        return self.__id

    @property
    def nameForFiles(self) -> str:
        return self._id     # Sometimes overrided

    def update(self):
        self.__outputs = self.evaluate(**self.__parameters)

    def evaluate(self, **kwargs: float) -> typ.Dict[str, float]:
        if any(map(lambda k: k not in self.__parameters, kwargs)):
            raise KeyError('Invalid parameter name')
        for p, v in kwargs.items():
            inf, sup = self.get_parameter_domain(p)
            if v < inf:
                kwargs[p] = inf
            elif v > sup:
                kwargs[p] = sup
        return self._evaluate(**kwargs)

    @abc.abstractmethod
    def _evaluate(self, **kwargs: float) -> typ.Dict[str, float]:
        pass

    @property
    def parameters_names(self) -> typ.List[str]:
        return list(self.__parameters.keys())

    def get_parameter_domain(self, param_name: str) -> typ.Optional[typ.Tuple[float, float]]:
        return self.__parameters_domains.get(param_name)

    def get_parameter(self, param_name: str) -> float:
        if param_name not in self.__parameters:
            raise KeyError(f'Invalid parameter name "{param_name}"')
        return self.__parameters[param_name]

    def set_parameter(self, param_name: str, value: float):
        if param_name not in self.__parameters:
            raise KeyError(f'Invalid parameter name "{param_name}"')
        inf, sup = self.__parameters_domains[param_name]
        self.__parameters[param_name] = max(inf, min(sup, value))

    @property
    def outputs_names(self) -> typ.Set[str]:
        return set(self.__outputs.keys())

    def get_output_domain(self, output_name: str) -> typ.Optional[typ.Tuple[float, float]]:
        return self.__outputs_domains.get(output_name)

    def get_output(self, output_name: str) -> float:
        if output_name not in self.__outputs:
            raise KeyError(f'Invalid output name "{output_name}"')
        return self.__outputs[output_name]

    def reset(self):
        for k in self.__parameters:
            self.__parameters[k] = 0
        for k in self.__outputs:
            self.__outputs[k] = 0


class ModelFactory(abc.ABC):
    @abc.abstractmethod
    def generate_model(self, *args, **kwargs) -> Model:
        pass


__all__ = [
    'Model',
    'ModelFactory',
]
