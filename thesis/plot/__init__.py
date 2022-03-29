import math
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
                       normalized: bool = True, regressions: bool = False, simple: bool = False,
                       mins: typ.Sequence[float] = None):
    params_nb = len(model.parameters_names)
    if params_nb == 1:
        _plot_2d(subplot, model, bounds, precision, normalized, regressions, simple, mins=mins or [])
    elif params_nb == 2:
        _plot_3d(subplot, model, bounds, precision, normalized, simple)
    else:
        raise ValueError('cannot plot model with more than 2 parameters')


def _plot_2d(subplot: fig.Axes, model: models.Model, bounds: typ.Tuple[float, float], precision: int, normalized: bool,
             regressions: bool, simple, mins: typ.Sequence[float]):
    colors = ['r', 'b', 'g', 'c', 'm', 'y']
    param_name = model.parameters_names[0]
    p_min, p_max = bounds or model.get_parameter_domain(param_name)
    xs = []
    ys = {output_name: [] for output_name in model.outputs_names}
    ys['_front'] = []
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
        ys['_front'].append(maxi)

    if mins:
        ys['_mins'] = []
        for x in mins:
            outputs = model.evaluate(**{param_name: x})
            maxi = 0
            for output_name, value in outputs.items():
                v = normalizers[output_name](value) if normalized else value
                maxi = max(maxi, v)
                ys[output_name].append(v)
            ys['_mins'].append(maxi)

    subplot.set_xlabel(f'${format_name(param_name)}$')
    model_id = model.id.replace('_', r'\_')
    subplot.set_ylabel(f'${model_id}({format_name(param_name)})$')
    if not simple:
        for i, output_name in enumerate(sorted(model.outputs_names)):
            subplot.plot(xs, ys[output_name], color=colors[i % len(colors)],
                         label=f'${format_name(output_name)}({format_name(param_name)})$')
            if regressions:
                reg, degree, error = fit_regression(xs, ys[output_name])
                subplot.plot(xs, reg, color=colors[i % len(colors)], linestyle=':',
                             label=fr'$poly\_reg({format_name(output_name)})$ / degree: {degree} '
                                   f'/ max error: {error:.4f}')
    subplot.plot(xs, ys['_front'], color='k', label=r'$\max(o_i)$')
    if regressions:
        reg, degree, error = fit_regression(xs, ys['_front'])
        subplot.plot(xs, reg, color='k', linestyle=':',
                     label=fr'$poly\_reg(\max(o_i))$ / degree: {degree} / max error: {error:.4f}')
    if mins:
        reg, degree, error = fit_regression(mins, ys['_mins'])
        subplot.plot(mins, reg, color='k', linestyle='--',
                     label=f'$f_{{min}}(x))$ / degree: {degree} / max error: {error:.4f}')


def _plot_3d(subplot: fig.Axes, model: models.Model, bounds: typ.Tuple[float, float], precision: int, normalized: bool,
             simple: bool):
    colormaps = ['jet', 'autumn', 'turbo', 'winter', 'hot', 'pink', 'bone']
    p1_name = model.parameters_names[0]
    p2_name = model.parameters_names[1]
    p1_min, p1_max = bounds or model.get_parameter_domain(p1_name)
    p2_min, p2_max = bounds or model.get_parameter_domain(p2_name)
    x1s, x2s = np.meshgrid(np.linspace(p1_min, p1_max, num=precision),
                           np.linspace(p2_min, p2_max, num=precision),
                           indexing='ij')
    ys = {output_name: np.ndarray((len(x1s), len(x2s))) for output_name in model.outputs_names}
    ys['front'] = np.ndarray((len(x1s), len(x2s)))
    normalizers = {output_name: calicoba.BoundNormalizer(*model.get_output_domain(output_name))
                   for output_name in model.outputs_names}

    for i, x1 in enumerate(x1s[:, 0]):
        for j, x2 in enumerate(x2s[0, :]):
            outputs = model.evaluate(**{p1_name: x1, p2_name: x2})
            y_max = 0
            for output_name, value in outputs.items():
                y = normalizers[output_name](value) if normalized else value
                y_max = max(y_max, y)
                ys[output_name][i, j] = y
            ys['front'][i, j] = y_max

    subplot.set_xlabel(f'${format_name(p1_name)}$')
    subplot.set_ylabel(f'${format_name(p2_name)}$')
    model_id = model.id.replace('_', r'\_')
    subplot.set_zlabel(f'${model_id}({format_name(p1_name)}, {format_name(p2_name)})$')
    if not simple:
        for i, output_name in enumerate(sorted(model.outputs_names)):
            surface = subplot.plot_surface(
                x1s, x2s, ys[output_name],
                cmap=colormaps[i % len(colormaps)],
                label=f'${format_name(output_name)}({format_name(p1_name)}, {format_name(p2_name)})$'
            )
            _fix_3d_surface(surface)
    if simple or len(ys) > 2:
        surface = subplot.plot_surface(x1s, x2s, ys['front'], color='k' if not simple else None,
                                       cmap='jet' if simple else None, label=r'$\max(o_i)$')
        _fix_3d_surface(surface)


def _fix_3d_surface(surface):
    # Bugfix https://stackoverflow.com/a/65554278/3779986
    # noinspection PyProtectedMember
    surface._facecolors2d = surface._facecolor3d
    # noinspection PyProtectedMember
    surface._edgecolors2d = surface._edgecolor3d


def fit_regression(xs, ys):
    min_error = math.inf
    prediction = None
    degree = 1
    for d in range(1, 101):
        poly_reg = sk_pre.PolynomialFeatures(degree=d)
        xs_trans = poly_reg.fit_transform(np.array([[x] for x in xs]))
        lin_reg = sk_lin.LinearRegression()
        lin_reg.fit(xs_trans, ys)
        pred = lin_reg.predict(xs_trans)
        error = sk_metrics.max_error(ys, pred)
        if error < min_error:
            min_error = error
            prediction = pred
            degree = d
    return prediction, degree, min_error
