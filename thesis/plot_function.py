import argparse
import typing as typ

import matplotlib.pyplot as plt

import calicoba.agents as calicoba
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

args = arg_parser.parse_args()
model_id: str = args.model_id
bounds: typ.Tuple[float, float] = args.bounds
sampling_precision: int = args.sampling_precision
raw: bool = args.raw
reg: bool = args.regressions

model = models.get_model_factory(models.FACTORY_SIMPLE).generate_model(model_id)
if len(model.parameters_names) != 1:
    raise ValueError('model has more than one parameter')

param_name = model.parameters_names[0]
out_names = model.outputs_names
p_min, p_max = bounds or model.get_parameter_domain(param_name)

normalizers = {output_name: calicoba.BoundNormalizer(*model.get_output_domain(output_name))
               for output_name in model.outputs_names}
fig = plt.figure()
title = 'Outputs' if raw else 'Normalized outputs'
fig.suptitle(f'{title} of model {model.id} along ${plot.format_name(param_name)}$')
subplot = fig.add_subplot(1, 1, 1)
plot.plot_model_outputs(subplot, model, bounds, precision=sampling_precision, normalized=not raw, regressions=reg)
subplot.legend()
plt.show()
