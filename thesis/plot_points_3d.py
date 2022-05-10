import argparse
import pathlib
import typing as typ

import matplotlib.pyplot as plt

import cobopti.agents as calicoba
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
if len(model.parameters_names) != 2:
    raise ValueError('model has not 2 parameters')

p1_name = model.parameters_names[0]
p2_name = model.parameters_names[1]
out_names = model.outputs_names
p1_min, p1_max = bounds or model.get_parameter_domain(p1_name)
p2_min, p2_max = bounds or model.get_parameter_domain(p2_name)

ys = {out_name: [] for out_name in out_names}
normalizers = {output_name: calicoba.BoundNormalizer(*model.get_output_domain(output_name))
               for output_name in out_names}


def load_data(param_name):
    xs = []

    with (path / f'{param_name}.csv').open(encoding='utf8') as f:
        for line in f.readlines()[1:]:
            _, param_value, _, _, _, _, _, _ = line.strip().split(',', maxsplit=7)
            xs.append(float(param_value))

    return xs


p1_xs = load_data(p1_name)
p2_xs = load_data(p2_name)
for p1_v, p2_v in zip(p1_xs, p2_xs):
    for out_name, out_value in model.evaluate(**{p1_name: p1_v, p2_name: p2_v}).items():
        ys[out_name].append(normalizers[out_name](out_value))

if p1_xs and p2_xs:
    if split_figures:
        dest_path = path / 'figures'
        if not dest_path.exists():
            dest_path.mkdir()
        else:
            for file in dest_path.glob('*.png'):
                if file.is_file():
                    file.unlink()

        for i in range(len(p1_xs)):
            fig = plt.figure()
            fig.suptitle(f'CoBOpti’s behavior on model {model.id}\n'
                         f'for $p(0) = ({p1_xs[0]}, {p2_xs[0]})$ (iteration {i + 1}/{len(p1_xs)})')
            subplot = fig.add_subplot(1, 1, 1, projection='3d', computed_zorder=False)
            subplot.view_init(90, -90)  # "Top-down" view
            print(f'Generating plot {i + 1}/{len(p1_xs)}')
            plot.plot_model_outputs(subplot, model, bounds, precision=sampling_precision)
            for j, out_name in enumerate(out_names):
                subplot.scatter(p1_xs[:i], p2_xs[:i], ys[out_name][:i], marker='x', color='r', zorder=2)
            for j, out_name in enumerate(out_names):
                subplot.scatter(p1_xs[i], p2_xs[i], ys[out_name][i], marker='o', color='g',
                                label='Latest point' if j == 0 else None, zorder=3)
            subplot.legend()
            fig.savefig(dest_path / f'fig_{i + 1}.png', dpi=200)
            plt.close(fig)
    else:
        fig = plt.figure()
        fig.suptitle(f'CoBOpti’s behavior on model {model.id} for $p(0) = ({p1_xs[0]}, {p2_xs[0]})$')
        subplot = fig.add_subplot(1, 1, 1, projection='3d', computed_zorder=False)
        plot.plot_model_outputs(subplot, model, bounds, precision=sampling_precision)
        for i, out_name in enumerate(out_names):
            subplot.scatter(p1_xs, p2_xs, ys[out_name], marker='x', color='r',
                            label=f'$p(t)$' if i == 0 else None, zorder=2)  # Always draw points on foreground
        subplot.legend()
        plt.show()
