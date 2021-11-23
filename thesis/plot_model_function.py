#!/usr/bin/python3
import argparse
import pathlib
import typing as typ

import matplotlib.pyplot as plt
import numpy as np

import models
import plot

arg_parser = argparse.ArgumentParser(description='Plot the path of a parameter on a 1D function.')
arg_parser.add_argument(dest='model_id', type=str, help='model to display the function of')
arg_parser.add_argument(dest='path', type=pathlib.Path,
                        help='path to the directory containing the parameter’s CSV file')
arg_parser.add_argument('-b', '--bounds', dest='bounds', metavar='BOUND', nargs=2, type=float,
                        help='the lower and upper bounds for the parameter’s domain', default=None)
arg_parser.add_argument('--split', dest='split', action='store_true', help='create one figure per step')
arg_parser.add_argument('-p', '--precision', dest='sampling_precision', metavar='PRECISION', type=int, default=200,
                        help='sampling precision when plotting the function')

args = arg_parser.parse_args()
model_id: str = args.model_id
bounds: typ.Tuple[float, float] = args.bounds
path: pathlib.Path = args.path
split_figures: bool = args.split
sampling_precision: int = args.sampling_precision

model = models.get_model_factory(models.FACTORY_SIMPLE).generate_model(model_id)
if len(model.parameters_names) != 1:
    raise ValueError('model has more than one parameter')

param_name = 'p1'
out_name = 'o1'
p_min, p_max = bounds or model.get_parameter_domain(param_name)
ys = []  # TODO supprimer
for x in np.linspace(p_min, p_max, num=sampling_precision):
    ys.append(model.evaluate(**{param_name: x})[out_name])

listened_points_xs = []
listened_points_ys = []
mins = []
p_xs = []
p_ys = []
with (path / f'{param_name}.csv').open(encoding='utf8') as f:
    for line in f.readlines()[1:]:
        _, value, _, _, listened_point_value, is_min, *_ = line.strip().split(',')
        value = float(value)
        is_min = bool(int(is_min))
        mins.append(is_min)
        if listened_point_value != '':
            listened_point_value = float(listened_point_value)
            listened_points_xs.append(listened_point_value)
            listened_points_ys.append(model.evaluate(**{param_name: listened_point_value})[out_name])
        else:
            listened_points_xs.append(None)
            listened_points_ys.append(None)
        p_xs.append(value)
        p_ys.append(model.evaluate(**{param_name: value})[out_name])

if p_xs:
    if split_figures:
        dest_path = path / 'figures'
        if not dest_path.exists():
            dest_path.mkdir()
        else:
            for file in dest_path.glob('*.png'):
                if file.is_file():
                    file.unlink()

        for i, (x, y) in enumerate(zip(p_xs, p_ys)):
            print(f'Generating plot {i + 1}/{len(p_xs)}')
            fig, subplot = plot.plot_model_function(model, bounds, precision=sampling_precision)
            fig.suptitle(f'Comportement de CALICOBA sur le modèle {model.id}\n'
                         f'pour $p(0) = {p_xs[0]}$ (itération {i + 1}/{len(p_xs)})')
            subplot.vlines(p_xs[0], min(ys), max(ys), color='black', linestyles='--', label='$p(0)$')
            subplot.scatter(p_xs[:i], p_ys[:i], marker='x', color='r', label='$p(t)$')
            subplot.scatter(p_xs[i], p_ys[i], marker='o', color='g', label='Dernier point')
            if i > 0 and listened_points_xs[i - 1] is not None:
                subplot.scatter(listened_points_xs[i - 1], listened_points_ys[i - 1], marker='x', color='b',
                                label='Point écouté')
            subplot.legend()
            fig.savefig(dest_path / f'fig_{i}.png', dpi=200)
            plt.close(fig)
    else:
        fig, subplot = plot.plot_model_function(model, bounds, precision=sampling_precision)
        fig.suptitle(f'Comportement de CALICOBA sur le modèle {model.id} pour $p(0) = {p_xs[0]}$')
        subplot.scatter(p_xs, p_ys, marker='x', color='r', label='$p(t)$')
        subplot.vlines(p_xs[0], min(ys), max(ys), color='black', linestyles='--', label='$p(0)$')
        subplot.vlines(p_xs[-1], min(ys), max(ys), color='limegreen', linestyles='--', label=f'$p({len(p_xs) - 1})$')
        subplot.legend()
        plt.show()
