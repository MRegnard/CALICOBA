#!/usr/bin/python3
import os
import pathlib
import sys

import matplotlib.pyplot as plt

fname1 = sys.argv[1]
fname2 = sys.argv[2]
measure_i = int(sys.argv[3] if len(sys.argv) == 4 else 1) - 1
param_name = pathlib.Path(fname1).stem
obj_name = pathlib.Path(fname2).stem

cycles = []
values = []
criticalities = []
crit_variations = []
criticalities_abs = []
crit_variations_abs = []
actions = []
est_delays = []
measures = []

with open(fname1) as f:
    for cycle, value, action, _, delay in map(lambda line: line.strip().split(','), f.readlines()[1:]):
        cycles.append(int(cycle))
        values.append(float(value))
        actions.append(int(action))
        est_delays.append(int(delay))

with open(fname2) as f:
    prev = None
    prev_abs = None
    for _, *m, crit in map(lambda line: line.strip().split(','), f.readlines()[1:]):
        measures.append(float(m[measure_i]))
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

rows = 2
cols = 3
marker = 'None'
marker2 = 'None'
color = 'r'
color2 = 'b'
line_style = '-'
x_min = cycles[0]
x_max = cycles[-1]
margin = abs(x_max - x_min) * 0.05
x_limits = {
    'xmin': x_min - margin,
    'xmax': x_max + margin,
}

# Criticalities
plt.subplot(rows, cols, 1)
plt.title(f'Criticality of objective agent ${obj_name}$')

plt.xlabel('cycle')
plt.ylabel('criticality')
plt.xlim(**x_limits)
plt.grid(True, which='both')
plt.hlines(0, **x_limits, color='k')

crit_line = plt.plot(cycles, criticalities, marker=marker, linestyle=line_style, color=color, label='Relative')
abs_crit_line = plt.plot(cycles, criticalities_abs, marker=marker2, linestyle=line_style, color=color2,
                         label='Absolute')

plt.legend(loc='best')

# Criticality variations
plt.subplot(rows, cols, 4)
plt.title(f'Criticality variation of objective agent ${obj_name}$')

plt.xlabel('cycle')
plt.ylabel('criticality')
plt.xlim(**x_limits)
plt.grid(True, which='both')
plt.hlines(0, **x_limits, color='k')

plt.plot(cycles, crit_variations, marker=marker, linestyle=line_style, color=color, label='Relative')
plt.plot(cycles, crit_variations_abs, marker=marker2, linestyle=line_style, color=color2, label='Absolute')

plt.legend(loc='best')

# Param values
plt.subplot(rows, cols, 2)
plt.title(f'Value of parameter agent ${param_name}$')

plt.xlabel('cycle')
plt.ylabel('value')
plt.xlim(**x_limits)
plt.grid(True, which='both')

plt.plot(cycles, values, marker=marker, linestyle=line_style, color=color)

# Param actions
plt.subplot(rows, cols, 5)
plt.title(f'Actions of parameter agent ${param_name}$')

plt.xlabel('cycle')
plt.ylabel('action')
plt.xlim(**x_limits)
plt.grid(True, which='both')
plt.hlines([-1, 0, 1], **x_limits, color='k')

plt.plot(cycles, actions, marker=marker, linestyle='None', color=color)

# Measures
plt.subplot(rows, cols, 3)
plt.title(f'Value of measure agent of ${obj_name}$')

plt.xlabel('cycle')
plt.ylabel('value')
plt.xlim(**x_limits)
plt.grid(True, which='both')

plt.plot(cycles, measures, marker=marker, linestyle=line_style, color=color)

#
plt.show()
