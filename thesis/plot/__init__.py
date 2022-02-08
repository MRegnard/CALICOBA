import typing as typ

import matplotlib.figure as fig
import numpy as np

import models


def plot_model_function(subplot: fig.Axes, model: models.Model, out_name: str, bounds: typ.Tuple[float, float],
                        precision: int = 200) -> typ.Tuple[float, float]:
    param_name = 'p1'
    p_min, p_max = bounds or model.get_parameter_domain(param_name)
    xs = []
    ys = []
    for x in np.linspace(p_min, p_max, num=precision):
        xs.append(x)
        ys.append(model.evaluate(**{param_name: x})[out_name])
    subplot.set_xlabel('$p$')
    model_id = model.id.replace('_', r'\_')
    subplot.set_ylabel(f'${model_id}(p)$')
    subplot.plot(xs, ys, label='$f(p)$')
    return min(ys), max(ys)
