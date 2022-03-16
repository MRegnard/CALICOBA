import re
import typing as typ

import matplotlib.figure as fig
import numpy as np
import sklearn.linear_model as sk_lin
import sklearn.metrics as sk_metrics
import sklearn.preprocessing as sk_pre

import calicoba.agents as calicoba
import models


def format_name(name: str):
    return re.sub(r'(\d+)', r'_{\1}', name)


def plot_model_outputs(subplot: fig.Axes, model: models.Model, bounds: typ.Tuple[float, float], precision: int = 200,
                       normalized: bool = True, regressions: bool = False):
    colors = ['r', 'b', 'g', 'c', 'm', 'y']
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
            v = normalizers[output_name](value) if normalized else value
            maxi = max(maxi, v)
            ys[output_name].append(v)
        ys['front'].append(maxi)

    subplot.set_xlabel(f'${format_name(param_name)}$')
    model_id = model.id.replace('_', r'\_')
    subplot.set_ylabel(f'${model_id}({format_name(param_name)})$')
    for i, output_name in enumerate(sorted(model.outputs_names)):
        subplot.plot(xs, ys[output_name], color=colors[i % len(colors)],
                     label=f'${format_name(output_name)}({format_name(param_name)})$')
        if regressions:
            reg, degree = fit_regression(xs, ys[output_name])
            subplot.plot(xs, reg, color=colors[i % len(colors)], linestyle=':',
                         label=fr'$poly\_reg({format_name(output_name)})$ / degree: {degree}')
    subplot.plot(xs, ys['front'], color='k', label=r'$\max(o_i)$')
    if regressions:
        reg, degree = fit_regression(xs, ys['front'])
        subplot.plot(xs, reg, color='k', linestyle=':', label=fr'$poly\_reg(\max(o_i))$ / degree: {degree}')


def fit_regression(xs, ys):
    d = 0
    while True:
        poly_reg = sk_pre.PolynomialFeatures(degree=d)
        xs_trans = poly_reg.fit_transform(np.array([[x] for x in xs]))
        lin_reg = sk_lin.LinearRegression()
        lin_reg.fit(xs_trans, ys)
        prediction = lin_reg.predict(xs_trans)
        if sk_metrics.max_error(ys, prediction) <= 0.099:
            return prediction, d
        d += 1
