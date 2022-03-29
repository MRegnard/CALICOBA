import argparse
import typing as typ

import matplotlib.pyplot as plt

import models
import plot

arg_parser = argparse.ArgumentParser(description='Plot normalized outputs of a model with a single parameter.')
arg_parser.add_argument('model_id', metavar='MODEL', type=str, help='ID of the model to plot')
arg_parser.add_argument('-b', '--bounds', dest='bounds', metavar='BOUND', nargs=2, type=float,
                        help='the lower and upper bounds for the parameter’s domain', default=None)
arg_parser.add_argument('-p', '--precision', dest='sampling_precision', metavar='PRECISION', type=int, default=200,
                        help='number of sampled points (default: 200)')
arg_parser.add_argument('-r', '--raw', dest='raw', action='store_true', default=False,
                        help='display non-normalized function')
arg_parser.add_argument('-R', '--regressions', dest='regressions', action='store_true', default=False,
                        help='display polynomial regressions of function(s)')
arg_parser.add_argument('-s', '--simple', dest='simple', action='store_true', default=False,
                        help='only displays the max function')
arg_parser.add_argument('-m', '--mins', dest='mins', metavar='MIN', type=float, nargs='+', default=[],
                        help='list of minimums of the max function to use to display the minimums function')

args = arg_parser.parse_args()
model_id: str = args.model_id
bounds: typ.Tuple[float, float] = args.bounds
sampling_precision: int = args.sampling_precision
raw: bool = args.raw
reg: bool = args.regressions
simple: bool = args.simple
mins: typ.List[float] = args.mins

model = models.get_model_factory(models.FACTORY_SIMPLE).generate_model(model_id)
params_nb = len(model.parameters_names)
if params_nb > 2:
    raise ValueError('model has more than two parameters')

fig = plt.figure()
title = ('Outputs' if raw else 'Normalized outputs') \
        + f' of model {model.id} along ${plot.format_name(model.parameters_names[0])}$'
if params_nb == 2:
    title += f' and ${plot.format_name(model.parameters_names[1])}$'
fig.suptitle(title)
subplot = fig.add_subplot(1, 1, 1, projection='3d' if params_nb == 2 else None)
plot.plot_model_outputs(subplot, model, bounds, precision=sampling_precision, normalized=not raw, regressions=reg,
                        simple=simple, mins=mins)
subplot.legend()
plt.show()
