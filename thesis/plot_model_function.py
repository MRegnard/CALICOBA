#!/usr/bin/python3
import argparse
import pathlib

import matplotlib.pyplot as plt
import numpy as np

import models

arg_parser = argparse.ArgumentParser(description='Plot the path of a parameter on a 1D function.')
arg_parser.add_argument(dest='model_id', type=str, help='The model to display the function of')
arg_parser.add_argument(dest='path', type=pathlib.Path,
                        help='Path to the directory containing the parameter’s CSV file')
arg_parser.add_argument('-b', '--bounds', dest='bounds', metavar='BOUND', nargs=2, type=float,
                        help='The lower and upper bounds for the parameter’s domain', default=None)

args = arg_parser.parse_args()
model_id = args.model_id
bounds = args.bounds
path = args.path

model = models.get_model_factory(models.FACTORY_SIMPLE).generate_model(model_id)
if len(model.parameters_names) != 1:
    raise ValueError('model has more than one parameter')

param_name = 'p1'
out_name = 'o1'
p_min, p_max = bounds or model.get_parameter_domain(param_name)
xs = []
ys = []
for x in np.linspace(p_min, p_max, num=200):
    xs.append(x)
    ys.append(model.evaluate(**{param_name: x})[out_name])

p_xs = []
p_ys = []
with (path / f'{param_name}.csv').open(encoding='utf8') as f:
    for line in f.readlines()[1:]:
        _, value, *_ = line.strip().split(',')
        value = float(value)
        p_xs.append(value)
        p_ys.append(model.evaluate(**{param_name: value})[out_name])

plt.title(f'Comportement de CALICOBA avec un minimum local pour $p(0) = {p_xs[0]}$')
plt.xlabel('$p$')
plt.ylabel('$f(p)$')
plt.plot(xs, ys)
if p_xs:
    plt.scatter(p_xs, p_ys, marker='x', color='r', label='$p(t)$')
    plt.vlines(p_xs[0], min(ys), max(ys), color='black', linestyles='--', label='$p(0)$')
    plt.vlines(p_xs[-1], min(ys), max(ys), color='limegreen', linestyles='--', label=f'$p({len(p_xs) - 1})$')
plt.legend()
plt.show()
