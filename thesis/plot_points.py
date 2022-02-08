#!/usr/bin/python3
import argparse
import pathlib
import typing as typ

import matplotlib.pyplot as plt

import models
import plot

arg_parser = argparse.ArgumentParser(description='Plot all visited points on a 1D function.')
arg_parser.add_argument(dest='path', type=pathlib.Path,
                        help='path to the directory containing the parameter’s CSV file')
arg_parser.add_argument('-m', '--model', dest='model_id', type=str,
                        help='ID of the model to display if it cannot be guessed from path')
arg_parser.add_argument('-b', '--bounds', dest='bounds', metavar='BOUND', nargs=2, type=float,
                        help='the lower and upper bounds for the parameter’s domain', default=None)
arg_parser.add_argument('-s', '--split', dest='split', action='store_true', help='create one figure per step')
arg_parser.add_argument('-p', '--precision', dest='sampling_precision', metavar='PRECISION', type=int, default=200,
                        help='sampling precision when plotting the function')

args = arg_parser.parse_args()
path: pathlib.Path = args.path
model_id: str = args.model_id or path.parent.name
bounds: typ.Tuple[float, float] = args.bounds
split_figures: bool = args.split
sampling_precision: int = args.sampling_precision

model = models.get_model_factory(models.FACTORY_SIMPLE).generate_model(model_id)
if len(model.parameters_names) != 1:
    raise ValueError('model has more than one parameter')

param_name = 'p1'
out_names = ['o1', 'o2']
p_min, p_max = bounds or model.get_parameter_domain(param_name)

listened_points_xs = []
listened_points_ys = {out_name: [] for out_name in out_names}
mins = []
p_xs = []
p_ys = {out_name: [] for out_name in out_names}
helped_objs = []
new_chain_indices = []
with (path / f'{param_name}.csv').open(encoding='utf8') as f:
    for line in f.readlines()[1:]:
        cycle, value, objective, _, listened_point_value, is_min, _, _, message = line.strip().split(',')
        value = float(value)
        is_min = bool(int(is_min))
        mins.append(is_min)
        helped_objs.append(objective[objective.index('obj_') + 4:])
        if 'chain' in message:
            new_chain_indices.append(int(cycle))
        if listened_point_value != '':
            listened_point_value = float(listened_point_value)
            listened_points_xs.append(listened_point_value)
            for out_name in out_names:
                listened_points_ys[out_name].append(model.evaluate(**{param_name: listened_point_value})[out_name])
        else:
            listened_points_xs.append(None)
            for out_name in out_names:
                listened_points_ys[out_name].append(None)
        p_xs.append(value)
        for out_name in out_names:
            p_ys[out_name].append(model.evaluate(**{param_name: value})[out_name])
    new_chain_indices.append(len(p_xs))

if p_xs:
    colors = ['r', 'c', 'm', 'y', 'k', 'w']
    if split_figures:
        dest_path = path / 'figures'
        if not dest_path.exists():
            dest_path.mkdir()
        else:
            for file in dest_path.glob('*.png'):
                if file.is_file():
                    file.unlink()

        for j, x in enumerate(p_xs):
            fig = plt.figure()
            fig.suptitle(f'Comportement de CALICOBA sur le modèle {model.id}\n'
                         f'pour $p(0) = {p_xs[0]}$ (itération {j + 1}/{len(p_xs)})')
            for i, out_name in enumerate(out_names):
                subplot = fig.add_subplot(1, len(out_names), i + 1)
                print(f'Generating plot {j + 1}/{len(p_xs)}')
                y_min, y_max = plot.plot_model_function(subplot, model, out_name, bounds,
                                                        precision=sampling_precision)
                subplot.vlines(p_xs[0], y_min, y_max, color='black', linestyles='--', label='$p(0)$')
                for k in range(len(new_chain_indices) - 1):
                    index = new_chain_indices[k]
                    next_index = new_chain_indices[k + 1]
                    subplot.scatter(p_xs[index:min(j, next_index)], p_ys[out_name][index:min(j, next_index)],
                                    marker='x', color=colors[k])
                subplot.scatter(p_xs[j], p_ys[out_name][j], marker='o', color='g', label='Dernier point')
                if j > 0 and listened_points_xs[j - 1] is not None and out_name == helped_objs[j - 1]:
                    subplot.scatter(listened_points_xs[j - 1], listened_points_ys[out_name][j - 1], marker='x',
                                    color='b', label='Point suggérant')
                subplot.legend()
            fig.savefig(dest_path / f'fig_{j}.png', dpi=200)
            plt.close(fig)
    else:
        fig = plt.figure()
        fig.suptitle(f'Comportement de CALICOBA sur le modèle {model.id} pour $p(0) = {p_xs[0]}$')
        for i, out_name in enumerate(out_names):
            subplot = fig.add_subplot(1, len(out_names), i + 1)
            y_min, y_max = plot.plot_model_function(subplot, model, out_name, bounds, precision=sampling_precision)
            subplot.scatter(p_xs, p_ys[out_name], marker='x', color='r', label='$p(t)$')
            subplot.vlines(p_xs[0], y_min, y_max, color='black', linestyles='--', label='$p(0)$')
            subplot.vlines(p_xs[-1], y_min, y_max, color='limegreen', linestyles='--', label=f'$p({len(p_xs) - 1})$')
            subplot.legend()
        plt.show()
