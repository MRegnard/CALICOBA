import re
import sys

import matplotlib.pyplot as plt

fname = sys.argv[1]
if m := re.fullmatch(r'(\w+)_CFParams{inf=(-?\d+\.\d+),infl1=(-?\d+\.\d+),nullMin=(-?\d+\.\d+),nullMax=(-?\d+\.\d+),infl2=(-?\d+\.\d+),sup=(-?\d+\.\d+)}.csv', fname):
  obj_name = m[1]
  inf = float(m[2])
  infl1 = float(m[3])
  null_min = float(m[4])
  null_max = float(m[5])
  infl2 = float(m[6])
  sup = float(m[7])
else:
  raise ValueError('invalid file name')

xs = []
ys = []
with open(fname, encoding='UTF-8') as f:
    for line in f.readlines()[1:]:
        x, y = map(float, line.strip().split(','))
        xs.append(x)
        ys.append(y)


def xline(y: float, text: str):
    plt.axvline(y, color='k', linewidth=1, linestyle='--')
    plt.text(y + 1, 0, text)


plt.title(f'{obj_name}\n$inf = {inf}, \eta_1 = {infl1}, \epsilon_1 = {null_min}, \epsilon_2 = {null_max}, \eta_2 = {infl2}, sup = {sup}$')

xline(inf, '$inf$')
xline(infl1, '$\eta_1$')
xline(null_min, '$\epsilon_1$')
xline(null_max, '$\epsilon_2$')
xline(infl2, '$\eta_2$')
xline(sup, '$sup$')

plt.axhline(0, color='r', linewidth=1, linestyle=':')
plt.axhline(100, color='r', linewidth=1, linestyle=':')

plt.xlabel('x')
plt.ylabel('crit(x)')

plt.plot(xs, ys)
plt.show()
