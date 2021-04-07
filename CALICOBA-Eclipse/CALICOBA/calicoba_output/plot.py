import sys

import matplotlib.pyplot as plt

fname1 = sys.argv[1]
fname2 = sys.argv[2]

cycles = []
values = []
criticalities = []
crit_variations = []
actions = []
est_delays = []
measures = []

with open(fname1 + '.csv') as f:
    prev = None
    for cycle, value, crit, action, delay in map(lambda line: line.strip().split(','), f.readlines()[1:]):
        cycles.append(int(cycle))
        values.append(float(value))
        criticalities.append(float(crit))
        actions.append(int(action))
        est_delays.append(int(delay))
        if prev is not None:
            crit_variations.append(criticalities[-1] - prev)
        else:
            crit_variations.append(None)
        prev = criticalities[-1]

with open(fname2 + '.csv') as f:
    for _, measure, _ in map(lambda line: line.strip().split(','), f.readlines()[1:-1]):
        measures.append(float(measure))

rows = 2
cols = 3
marker = 'x'
color = 'r'
line_style = ':'
x_min = cycles[0]
x_max = cycles[-1]
margin = abs(x_max - x_min) * 0.05
x_limits = {
    'xmin': x_min - margin,
    'xmax': x_max + margin,
}

# Criticalities
plt.subplot(rows, cols, 1)
plt.title(f'Criticality of satisfaction agent ${fname2}$')

plt.xlabel('cycle')
plt.ylabel('criticality')
plt.xlim(**x_limits)
plt.grid(True, which='both')

plt.plot(cycles, criticalities, drawstyle='steps-post', marker=marker, linestyle=line_style, color=color)

# Criticality variations
plt.subplot(rows, cols, 4)
plt.title(f'Criticality variation of satisfaction agent ${fname2}$')

plt.xlabel('cycle')
plt.ylabel('criticality')
plt.xlim(**x_limits)
plt.grid(True, which='both')

plt.plot(cycles, crit_variations, marker=marker, linestyle=line_style, color=color)

# Param actions
plt.subplot(rows, cols, 5)
plt.title(f'Actions of parameter agent ${fname1}$')

plt.xlabel('cycle')
plt.ylabel('action')
plt.xlim(**x_limits)
plt.grid(True, which='both')

plt.plot(cycles, actions, drawstyle='steps-post', marker=marker, linestyle=line_style, color=color)

# Param values
plt.subplot(rows, cols, 2)
plt.title(f'Value of parameter agent ${fname1}$')

plt.xlabel('cycle')
plt.ylabel('value')
plt.xlim(**x_limits)
plt.grid(True, which='both')

plt.plot(cycles, values, drawstyle='steps-post', marker=marker, linestyle=line_style, color=color)

# Measures
plt.subplot(rows, cols, 3)
plt.title(f'Value of measure agent of ${fname2}$')

plt.xlabel('cycle')
plt.ylabel('value')
plt.xlim(**x_limits)
plt.grid(True, which='both')

plt.plot(cycles, measures, drawstyle='steps-post', marker=marker, linestyle=line_style, color=color)

#
plt.show()
