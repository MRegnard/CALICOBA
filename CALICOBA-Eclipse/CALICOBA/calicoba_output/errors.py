#!/usr/bin/python3
import os
import sys

DIR_NAME = sys.argv[1]
alpha = float(sys.argv[2]) if len(sys.argv) == 3 else None

PARAM_NAMES = ['p1', 'p2']

for dirname in sorted(os.listdir(os.path.join('.', DIR_NAME))):
    if os.path.isdir(os.path.join('.', DIR_NAME, dirname)) and (
            alpha is None and not dirname.startswith('learning_')
            or alpha is not None and dirname.startswith(f'learning_{alpha:.3f}_')):
        print(dirname.replace('_', ' '))
        print('\test\tmin\tmax\tΔ')
        param_data = {}
        for param_name in PARAM_NAMES:
            cycles = []
            values = []
            actions = []
            with open(os.path.join(DIR_NAME, dirname, param_name + '.csv')) as f:
                for cycle, value, action, _, _ in map(lambda line: line.strip().split(','), f.readlines()[1:]):
                    cycles.append(int(cycle))
                    values.append(float(value))
                    actions.append(int(action))
            param_data[param_name.replace('p', 'p_')] = {
                'cycles': cycles,
                'values': values,
                'actions': actions,
            }
            last_values = values[-10:]
            mini = min(last_values)
            maxi = max(last_values)
            est_value = (maxi + mini) / 2
            deviation = maxi - est_value
            print(f'{param_name}\t{est_value:.2f}\t{mini:.2f}\t{maxi:.2f}\t{deviation:.2f}')
        print()
