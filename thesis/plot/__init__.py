import re
import typing as typ

import matplotlib.figure as fig
import numpy as np

import calicoba.agents as calicoba
import models


def format_name(name: str):
    return re.sub(r'(\d+)', r'_{\1}', name)


def plot_model_function(subplot: fig.Axes, model: models.Model, bounds: typ.Tuple[float, float], precision: int = 200):
    colors = ['b', 'g', 'c', 'm', 'y', 'k']
    param_name = model.parameters_names[0]
    p_min, p_max = bounds or model.get_parameter_domain(param_name)
    xs = []
    ys = {output_name: [] for output_name in model.outputs_names}
    ys['front'] = []
    normalizers = {output_name: calicoba.BoundNormalizer(*model.get_output_domain(output_name))
                   for output_name in model.outputs_names}
    for x in np.linspace(p_min, p_max, num=precision):
        xs.append(x)
        outputs = model.evaluate(**{param_name: x})
        maxi = 0
        for output_name, value in outputs.items():
            v = normalizers[output_name](value)
            maxi = max(maxi, v)
            ys[output_name].append(v)
        ys['front'].append(maxi)
    subplot.set_xlabel(f'${format_name(param_name)}$')
    model_id = model.id.replace('_', r'\_')
    subplot.set_ylabel(f'${model_id}({format_name(param_name)})$')
    for i, output_name in enumerate(model.outputs_names):
        subplot.plot(xs, ys[output_name], color=colors[i], label=f'${format_name(output_name)}$')
    subplot.plot(xs, ys['front'], color='r', linestyle='--', label='Max criticality')
