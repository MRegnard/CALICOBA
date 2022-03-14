import argparse
import pathlib
import typing as typ

import matplotlib.pyplot as plt
from PIL import Image

import calicoba.agents as calicoba
import models
import plot

FRAMES_DIR = 'frames'


def load_data(directory: pathlib.Path, model: models.Model, param_name: str, out_names: typ.Iterable[str],
              proportion: int, normalizers):
    print(f'Loading data from {directory}')
    max_cycles = 0
    all_xs = {}
    all_ys = {}

    for i, dir_name in enumerate(directory.glob('p1=*')):
        if not dir_name.is_dir() or i % proportion != 0:
            continue
        xs = []
        ys = {out_name: [] for out_name in out_names}
        with (dir_name / f'{param_name}.csv').open(encoding='utf8') as f:
            for line in f.readlines()[1:]:
                _, param_value, *_ = line.strip().split(',')
                param_value = float(param_value)
                xs.append(param_value)
                for out_name, out_value in model.evaluate(**{param_name: param_value}).items():
                    ys[out_name].append(normalizers[out_name](out_value))
        k = float(dir_name.name.split('=')[1])
        max_cycles = max(max_cycles, len(xs))
        all_xs[k] = xs
        all_ys[k] = ys

    print('Done.')
    return max_cycles, all_xs, all_ys


def generate_frames(model: models.Model, bounds: typ.Tuple[float, float], all_xs, all_ys, max_cycles: int,
                    precision: int, full: bool, output_dir: pathlib.Path):
    directory = output_dir / FRAMES_DIR
    if not directory.exists():
        directory.mkdir(parents=True)
    else:
        print('Frames already generated, skipping.')
        return

    color_names = []
    with open('xkcd_rgb.txt', encoding='utf8') as f:
        for line in f.readlines():
            color_names.append('xkcd:' + line.split('\t')[0])

    for cycle in range(max_cycles):
        fig = plt.figure()
        fig.suptitle(f'CoBOpti’s behavior on model {model.id} over {len(all_xs)} run(s)\n'
                     f'Iteration {cycle + 1}/{max_cycles}')
        subplot = fig.add_subplot(1, 1, 1)
        print(f'Generating frame {cycle + 1}/{max_cycles}')
        plot.plot_model_outputs(subplot, model, bounds, precision=precision)
        for run_index, start_value in enumerate(all_xs):
            xs = all_xs[start_value]
            ys = all_ys[start_value]
            if cycle < len(xs):
                max_out_name = max(ys.items(), key=lambda y: y[1][cycle])[0]
                for out_name in model.outputs_names:
                    if full or out_name == max_out_name:
                        subplot.scatter(xs[cycle], ys[out_name][cycle], marker='o', color=color_names[run_index])
        subplot.legend()
        fig.savefig(directory / f'{cycle + 1}.png', dpi=200)
        plt.close(fig)


def generate_gif(delay: int, output_dir: pathlib.Path):
    print('Generating GIF from frames…')
    frames = [Image.open(image) for image in sorted((output_dir / FRAMES_DIR).glob('*.png'),
                                                    key=lambda p: int(p.name.split('.')[0]))]
    frame_one = frames[0]
    frame_one.save(output_dir / f'trajectories-{delay}ms.gif', format='GIF', append_images=frames,
                   save_all=True, duration=delay, loop=True)
    print(f'Generated image in {output_dir}')


def main():
    arg_parser = argparse.ArgumentParser(
        description='Generate GIF image of trajectory of points for selected model and data.')
    arg_parser.add_argument('model_id', metavar='MODEL', type=str, help='ID of the model to plot')
    arg_parser.add_argument(dest='path', type=pathlib.Path,
                            help='path to the directory containing the runs directories')
    arg_parser.add_argument('-b', '--bounds', dest='bounds', metavar='BOUND', nargs=2, type=float,
                            help='the lower and upper bounds for the parameter’s domain', default=None)
    arg_parser.add_argument('-p', '--precision', dest='sampling_precision', metavar='PRECISION', type=int, default=200,
                            help='number of sampled points (default: 200)')
    arg_parser.add_argument('-d', '--delay', dest='delay', metavar='DELAY', type=int, default=100,
                            help='delay between frames in ms (default: 100)')
    arg_parser.add_argument('-f', '--full', dest='full', action='store_true', default=False,
                            help='display points on all functions')
    arg_parser.add_argument('-P', '--proportion', dest='proportion', type=int, default=1,
                            help='proportion of points to display (default: 1)')

    args = arg_parser.parse_args()
    path: pathlib.Path = args.path.absolute()
    model_id: str = args.model_id
    bounds: typ.Tuple[float, float] = args.bounds
    sampling_precision: int = args.sampling_precision
    frames_delay: int = args.delay
    full: bool = args.full
    proportion: int = args.proportion

    model = models.get_model_factory(models.FACTORY_SIMPLE).generate_model(model_id)
    if len(model.parameters_names) != 1:
        raise ValueError('model has more than one parameter')

    param_name = model.parameters_names[0]
    out_names = model.outputs_names
    p_min, p_max = bounds or model.get_parameter_domain(param_name)

    normalizer_functions = {output_name: calicoba.BoundNormalizer(*model.get_output_domain(output_name))
                            for output_name in model.outputs_names}

    out_dir = path / 'animation'
    longest_cycles, p_xs, p_ys = load_data(path, model, param_name, out_names, proportion, normalizer_functions)
    generate_frames(model, (p_min, p_max), p_xs, p_ys, longest_cycles, sampling_precision, full, out_dir)
    generate_gif(frames_delay, out_dir)


if __name__ == '__main__':
    main()
