#!/usr/bin/python3
import os
import pathlib
import sys
import typing as typ

import matplotlib.pyplot as plt

OBJ_NAMES = ['obj1', 'obj2']
PARAM_NAMES = ['p1', 'p2']

dirname = sys.argv[1]
if len(sys.argv) == 4:
    interval = int(sys.argv[2]), int(sys.argv[3])
else:
    interval = None

obj_data = {}
for obj_name in OBJ_NAMES:
    cycles = []
    raw_values = []
    criticalities = []
    crit_variations = []
    criticalities_abs = []
    crit_variations_abs = []
    with open(os.path.join(dirname, obj_name + '.csv')) as f:
        prev = None
        prev_abs = None
        for cycle, *_, raw_value, crit in map(lambda line: line.strip().split(','), f.readlines()[1:]):
            cycles.append(int(cycle))
            raw_values.append(float(raw_value))
            criticalities.append(float(crit))
            criticalities_abs.append(abs(criticalities[-1]))
            if prev is not None:
                crit_variations.append(criticalities[-1] - prev)
                crit_variations_abs.append(criticalities_abs[-1] - prev_abs)
            else:
                crit_variations.append(None)
                crit_variations_abs.append(None)
            prev = criticalities[-1]
            prev_abs = criticalities_abs[-1]
    obj_data[obj_name.replace('bj', '_')] = {
        'cycles': cycles,
        'raw_values': raw_values,
        'criticalities': criticalities,
        'crit_variations': crit_variations,
        'abs_criticalities': criticalities_abs,
        'abs_crit_variations': crit_variations_abs,
    }

param_data = {}
for param_name in PARAM_NAMES:
    cycles = []
    values = []
    actions = []
    with open(os.path.join(dirname, param_name + '.csv')) as f:
        for cycle, value, action, _, _ in map(lambda line: line.strip().split(','), f.readlines()[1:]):
            cycles.append(int(cycle))
            values.append(float(value))
            actions.append(int(action))
    param_data[param_name.replace('p', 'p_')] = {
        'cycles': cycles,
        'values': values,
        'actions': actions,
    }


rows = 2
cols = 2
line_style = '-'
x_min = min([data['cycles'][0] for data in obj_data.values()] + [data['cycles'][0] for data in param_data.values()])
x_max = max([data['cycles'][-1] for data in obj_data.values()] + [data['cycles'][-1] for data in param_data.values()])
if interval:
    x_min = max(x_min, interval[0])
    x_max = min(x_max, interval[1])
margin = abs(x_max - x_min) * 0.05
x_limits = {
    'xmin': x_min - margin,
    'xmax': x_max + margin,
}
p1, p2 = dirname.split('_', maxsplit=1)
title = f'$p_1 = {p1}, p_2 = {p2}$'

fig, axes = plt.subplots(2, 2, constrained_layout=True)
fig.suptitle(title)
fig.canvas.manager.set_window_title(title.replace('$', ''))

# Criticalities
p = 0, 0
axes[p].set_title('Objective criticalities')
axes[p].set_xlabel('cycle')
axes[p].set_ylabel('criticality')
axes[p].set_xlim(**x_limits)
axes[p].grid(True, which='both')
axes[p].hlines(0, **x_limits, color='k')

for obj_name, obj_d in obj_data.items():
    axes[p].plot(obj_d['cycles'], obj_d['criticalities'], marker='None', linestyle=line_style, label=f'${obj_name}$')

axes[p].legend(loc='best')

# Absolute criticalities
p = 0, 1
axes[p].set_title(f'Absolute objective criticalities')
axes[p].set_xlabel('cycle')
axes[p].set_ylabel('absolute criticality')
axes[p].set_xlim(**x_limits)
axes[p].grid(True, which='both')

for obj_name, obj_d in obj_data.items():
    axes[p].plot(obj_d['cycles'], obj_d['abs_criticalities'], marker='None', linestyle=line_style, label=f'${obj_name}$')

axes[p].legend(loc='best')

# Objective values
p = 1, 1
axes[p].set_title(f'Raw objective values')
axes[p].set_xlabel('cycle')
axes[p].set_ylabel('value')
axes[p].set_xlim(**x_limits)
axes[p].grid(True, which='both')

for obj_name, obj_d in obj_data.items():
    axes[p].plot(obj_d['cycles'], obj_d['raw_values'], marker='None', linestyle=line_style, label=f'${obj_name}$')

axes[p].legend(loc='best')

# Param values
p = 1, 0
axes[p].set_title(f'Parameter values')
axes[p].set_xlabel('cycle')
axes[p].set_ylabel('value')
axes[p].set_xlim(**x_limits)
axes[p].grid(True, which='both')

for param_name, param_d in param_data.items():
    axes[p].plot(param_d['cycles'], param_d['values'], marker='None', linestyle=line_style, label=f'${param_name}$')

axes[p].legend(loc='best')

#
plt.show()
