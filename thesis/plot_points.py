import argparse
import pathlib
import typing as typ

import matplotlib.pyplot as plt

import calicoba.agents as calicoba
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

param_name = model.parameters_names[0]
out_names = model.outputs_names
p_min, p_max = bounds or model.get_parameter_domain(param_name)

listened_points_xs = []
p_xs = []
p_ys = {out_name: [] for out_name in out_names}
normalizers = {output_name: calicoba.BoundNormalizer(*model.get_output_domain(output_name))
               for output_name in model.outputs_names}
new_chain_indices = []
with (path / f'{param_name}.csv').open(encoding='utf8') as f:
    for line in f.readlines()[1:]:
        cycle, param_value, _, _, listened_point_value, _, _, _, message = line.strip().split(',')
        param_value = float(param_value)
        if 'chain' in message:
            new_chain_indices.append(int(cycle))
        if listened_point_value != '':
            listened_point_value = float(listened_point_value)
            listened_points_xs.append(listened_point_value)
        else:
            listened_points_xs.append(None)
        p_xs.append(param_value)
        for out_name, out_value in model.evaluate(**{param_name: param_value}).items():
            p_ys[out_name].append(normalizers[out_name](out_value))
    new_chain_indices.append(len(p_xs))

if p_xs:
    colors = ['r', 'c', 'm', 'y', 'k']
    if split_figures:
        dest_path = path / 'figures'
        if not dest_path.exists():
            dest_path.mkdir()
        else:
            for file in dest_path.glob('*.png'):
                if file.is_file():
                    file.unlink()

        for i in range(len(p_xs)):
            fig = plt.figure()
            fig.suptitle(f'Comportement de CALICOBA sur le modèle {model.id}\n'
                         f'pour ${param_name}(0) = {p_xs[0]}$ (itération {i + 1}/{len(p_xs)})')
            subplot = fig.add_subplot(1, 1, 1)
            print(f'Generating plot {i + 1}/{len(p_xs)}')
            plot.plot_model_function(subplot, model, bounds, precision=sampling_precision)
            subplot.vlines(p_xs[0], 0, 1, color='black', linestyles='--', label=f'${param_name}(0)$')
            for j in range(len(new_chain_indices) - 1):
                index = new_chain_indices[j]
                next_index = new_chain_indices[j + 1]
                for out_name in model.outputs_names:
                    subplot.scatter(p_xs[index:min(i, next_index)], p_ys[out_name][index:min(i, next_index)],
                                    marker='x', color=colors[j])
            for j, out_name in enumerate(model.outputs_names):
                subplot.scatter(p_xs[i], p_ys[out_name][i], marker='o', color='g',
                                label='Dernier point' if j == 0 else None)
            if i > 0 and listened_points_xs[i - 1] is not None:
                for j, out_name in enumerate(model.outputs_names):
                    y = p_ys[out_name][p_xs.index(listened_points_xs[i - 1])]
                    subplot.scatter(listened_points_xs[i - 1], y, marker='x', color='b',
                                    label='Point suggérant' if j == 0 else None)
            subplot.legend()
            fig.savefig(dest_path / f'fig_{i + 1}.png', dpi=200)
            plt.close(fig)
    else:
        fig = plt.figure()
        fig.suptitle(f'Comportement de CALICOBA sur le modèle {model.id} pour ${param_name}(0) = {p_xs[0]}$')
        subplot = fig.add_subplot(1, 1, 1)
        plot.plot_model_function(subplot, model, bounds, precision=sampling_precision)
        for i, out_name in enumerate(model.outputs_names):
            subplot.scatter(p_xs, p_ys[out_name], marker='x', color='r', label=f'${param_name}(t)$' if i == 0 else None)
        subplot.vlines(p_xs[0], 0, 1, color='black', linestyles='--', label=f'${param_name}(0)$')
        subplot.vlines(p_xs[-1], 0, 1, color='limegreen', linestyles='--', label=f'${param_name}({len(p_xs) - 1})$')
        subplot.legend()
        plt.show()
