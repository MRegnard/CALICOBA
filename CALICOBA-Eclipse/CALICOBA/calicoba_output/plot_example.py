#!/usr/bin/python3
import os
import random
import sys

import matplotlib.pyplot as plt
import matplotlib._color_data as mcd

alpha = float(sys.argv[1]) if len(sys.argv) != 1 else None

OBJ_NAMES = ['obj_1', 'obj_2']

# Data extraction

obj_data = {}

for dirname in sorted(os.listdir()):
    if os.path.isdir('./' + dirname) and (
            alpha is None and not dirname.startswith('learning_')
            or alpha is not None and dirname.startswith(f'learning_{alpha:.3f}_')):
        print(dirname)
        obj_data[dirname] = {}
        
        for obj_name in OBJ_NAMES:
          with open(os.path.join('.', dirname, obj_name.replace('_', '') + '.csv')) as f:
              cycles = []
              criticalities = []
              criticalities_abs = []
              crit_variations = []
              crit_variations_abs = []
              prev = None
              prev_abs = None
              for cycle, *m, crit in map(lambda line: line.strip().split(','), f.readlines()[1:]):
                  cycles.append(int(cycle))
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
              
              obj_data[dirname][obj_name] = {
                  'cycles': cycles,
                  'criticalities': criticalities,
                  'criticalities_abs': criticalities_abs,
                  'criticalities_variation': crit_variations,
                  'criticalities_variation_abs': crit_variations_abs,
              }

# Plots

COLORS = list(mcd.XKCD_COLORS.keys())
random.seed(1)
random.shuffle(COLORS)
marker = 'None'
line_style = '-'
x_min = min([j['cycles'][0] for i in obj_data.values() for j in i.values()])
x_max = max([j['cycles'][-1] for i in obj_data.values() for j in i.values()])
margin = abs(x_max - x_min) * 0.05
x_limits = {
    'xmin': x_min - margin,
    'xmax': x_max + margin,
}
title = 'Criticalities ({})'.format(f'learning, $α = {alpha}$' if alpha is not None else 'preset')

# Criticalities
fig, axes = plt.subplots(1, 2, constrained_layout=True)
fig.suptitle(title)
fig.canvas.manager.set_window_title(title.replace('$', ''))

for i, obj_name in enumerate(OBJ_NAMES):
  axes[i].set_title(f'Criticalities for objective agent ${obj_name}$')

  axes[i].set_xlabel('cycle')
  axes[i].set_ylabel('criticality')
  axes[i].set_xlim(**x_limits)
  axes[i].grid(True, which='both')

  for j, (dirname, obj) in enumerate(((k, v[obj_name]) for k, v in obj_data.items())):
      p1, p2 = dirname.split('_')[-2:]
      color = 'r'

      axes[i].plot(
          obj['cycles'], obj['criticalities_abs'],
          drawstyle='steps-post',
          marker=marker,
          linestyle=line_style,
          color=COLORS[j],
          label=f'$p_1 = {p1}, p_2 = {p2}$'
      )

  axes[i].legend(loc='best')

plt.show()
