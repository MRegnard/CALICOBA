import typing as typ

import matplotlib.figure as fig
import matplotlib.pyplot as plt
import numpy as np

import models


def plot_model_function(model: models.Model, bounds: typ.Tuple[float, float], precision: int = 200) \
        -> typ.Tuple[fig.Figure, fig.Axes]:
    param_name = 'p1'
    out_name = 'o1'
    p_min, p_max = bounds or model.get_parameter_domain(param_name)
    xs = []
    ys = []
    for x in np.linspace(p_min, p_max, num=precision):
        xs.append(x)
        ys.append(model.evaluate(**{param_name: x})[out_name])
    figure: fig.Figure = plt.figure()
    subplot = figure.add_subplot(1, 1, 1)
    subplot.set_xlabel('$p$')
    model_id = model.id.replace('_', r'\_')
    subplot.set_ylabel(f'${model_id}(p)$')
    subplot.plot(xs, ys, label='$f(p)$')
    return figure, subplot
