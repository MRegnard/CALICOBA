from __future__ import annotations

import abc
import itertools
import random
import typing as typ

import numpy as np

from . import _model


class Node(abc.ABC):
    def __init__(self, branch: int, parents_nb: int):
        self._branch = branch
        # noinspection PyTypeChecker
        self._parent_nodes: typ.List[Node] = [None] * parents_nb

    @property
    def branch(self) -> int:
        return self._branch

    @property
    def inputs_number(self):
        return len(self._parent_nodes)

    def is_parent_set(self, index: int) -> bool:
        return self._parent_nodes[index] is not None

    def set_parent_node(self, index: int, node: Node):
        if node is None:
            raise ValueError('node is None')
        if node.branch != -1 and node.branch != self.branch:
            raise ValueError(f'invalid branch for parent node, expected {self.branch}, got {node.branch}')
        self._parent_nodes[index] = node

    def compute(self) -> float:
        if any(n is None for n in self._parent_nodes):
            raise ValueError('missing parent node')
        return self._compute()

    @abc.abstractmethod
    def _compute(self) -> float:
        pass

    def reset(self):
        for parent_node in self._parent_nodes:
            parent_node.reset()

    def __str__(self):
        return f'{self.__class__.__name__}{{{self.branch}}}{self._parent_nodes}'


class ParameterNode(Node):
    def __init__(self, name: str, initial_branching_factor: int):
        super().__init__(-1, 0)
        self._name = name
        self._branching_factor = initial_branching_factor
        self._value = 0

    @property
    def name(self) -> str:
        return self._name

    @property
    def value(self) -> float:
        return self._value

    @value.setter
    def value(self, value: float):
        self._value = value

    @property
    def branching_factor(self) -> int:
        return self._branching_factor

    def decrease_branching_factor(self):
        if self._branching_factor == 0:
            raise RuntimeError('branching factor is 0')
        self._branching_factor -= 1

    def _compute(self):
        return self._value

    def reset(self):
        self._value = 0


class BiNode(Node):
    def __init__(self, branch: int, function: typ.Callable[[float, float], float], operator_symbol: str):
        super().__init__(branch, 2)
        self._operator = operator_symbol
        self._function = function

    def _compute(self):
        return self._function(self._parent_nodes[0].compute(), self._parent_nodes[1].compute())

    def __str__(self):
        return f'({self._parent_nodes[0]} {self._operator} {self._parent_nodes[1]})'


class SumNode(BiNode):
    def __init__(self, branch: int):
        super().__init__(branch, lambda x, y: x + y, '+')


class ProductNode(BiNode):
    def __init__(self, branch: int):
        super().__init__(branch, lambda x, y: x * y, '×')


class DivisionNode(BiNode):
    def __init__(self, branch: int):
        super().__init__(branch, lambda x, y: x / y, '/')


class DelayNode(Node):
    BUFFER_SIZE = 30

    def __init__(self, branch: int):
        super().__init__(branch, 1)
        self._buffer = []

    def _compute(self):
        if len(self._buffer) == self.BUFFER_SIZE:
            del self._buffer[0]
        self._buffer.append(self._parent_nodes[0].compute())
        return np.average(self._buffer)

    def reset(self):
        self._buffer = []
        super().reset()

    def __str__(self):
        return f'delay({self._parent_nodes[0]})'


class GeneratedModel(_model.Model):
    def __init__(self, parameters: typ.List[ParameterNode], outputs: typ.Dict[str, Node]):
        super().__init__(f'generated_model_p{len(parameters)}_o{len(outputs)}',
                         self._generate_domains(p.name for p in parameters),
                         self._generate_domains(outputs.keys()))
        self._parameters = {p.name: p for p in parameters}
        self._outputs = outputs

    def _evaluate(self, **kwargs: float) -> typ.Dict[str, float]:
        for k, v in kwargs.items():
            self._parameters[k].value = v
        return {name: o.compute() for name, o in self._outputs}

    @staticmethod
    def _generate_domains(node_names: typ.Iterable[str]) -> typ.Dict[str, typ.Tuple[float, float]]:
        return {n: (1e-9, 1e9) for n in node_names}

    def reset(self):
        for o in self._outputs.values():
            o.reset()

    def __str__(self):
        return str(self._outputs)


_T = typ.TypeVar('_T', bound=Node)


class ProceduralModelFactory(_model.ModelFactory):
    def __init__(self, *node_types: typ.Type[_T]):
        if not node_types:
            raise ValueError('empty node list')
        self._rng = random.Random()
        self._node_types = [lambda branch: nt(branch) for nt in node_types]

    def set_seed(self, seed: int):
        self._rng.seed(seed)

    def generate_model(self, layers_nb: int, outputs_nb: int, branching_factor: int):
        if not (1 <= branching_factor <= outputs_nb):
            raise ValueError(f'branching factor should be between 1 and the number of outputs ({outputs_nb}), '
                             f'got {branching_factor}')
        output_nodes = self._generate_outputs(outputs_nb)
        top_layer, inputs_nb = self._generate_layers(layers_nb, *output_nodes.values())
        parameter_nodes = self._generate_inputs(inputs_nb, branching_factor, *top_layer)
        return GeneratedModel(parameter_nodes, output_nodes)

    def _generate_outputs(self, outputs_nb: int) -> typ.Dict[str, Node]:
        output_nodes = {}

        for i in range(outputs_nb):
            node = self._get_random_node(i)
            output_nodes[f'o{i + 1}'] = node

        return output_nodes

    def _generate_layers(self, layers_nb: int, *output_nodes: Node) -> typ.Tuple[typ.List[Node], int]:
        inputs_nb = 0
        current_layer = output_nodes

        for layer in range(layers_nb):
            next_layer = []
            for node in current_layer:
                for i in range(node.inputs_number):
                    parent = self._get_random_node(node.branch)
                    next_layer.append(parent)
                    node.set_parent_node(i, parent)
                    if layer == layers_nb - 1:
                        inputs_nb += parent.inputs_number
            current_layer = next_layer

        return current_layer, inputs_nb

    @staticmethod
    def _generate_inputs(inputs_nb: int, branching_factor: int, *top_layer: Node) -> typ.List[ParameterNode]:
        parameters = []
        branches: typ.List[typ.Tuple[int, typ.Iterable[Node]]] = sorted(
            itertools.groupby(top_layer, key=lambda n: n.branch),
            key=lambda e: e[0]
        )
        parameter_id = 1

        # Rules:
        # - A parameter can connect at most once on each branch.
        # - A parameter cannot connect to more inputs than its branching factor.
        # - If an input cannot find a free parameter, a new parameter is created.
        # - Repeat until all inputs have found a parameter.
        while inputs_nb != 0:
            param_name = f'p{parameter_id}'
            parameter = ParameterNode(param_name, branching_factor)
            parameters.append(parameter)
            parameter_id += 1

            param_done = False
            for branch, nodes in branches:
                parent_set = False
                for node in nodes:
                    for i in range(node.inputs_number):
                        if not node.is_parent_set(i):
                            node.set_parent_node(i, parameter)
                            parameter.decrease_branching_factor()
                            inputs_nb -= 1
                            parent_set = True
                            if parameter.branching_factor == 0:
                                param_done = True
                            break
                    if parent_set:
                        break
                if param_done:
                    break

        return parameters

    def _get_random_node(self, branch: int) -> _T:
        index = self._rng.randint(0, len(self._node_types) - 1)
        return self._node_types[index](branch)
