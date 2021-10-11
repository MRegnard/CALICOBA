#!/usr/bin/python3
import sys

import matplotlib.pyplot as plt

xs = []
ys = []
with open('experiment/model_5_function.csv', encoding='UTF-8') as f:
    for line in f.readlines()[1:]:
        x, y = map(float, line.strip().split(','))
        xs.append(x)
        ys.append(y)

p_xs = []
p_ys = []
if len(sys.argv) == 2:
    p1 = float(sys.argv[1])
    with open(f'experiment/model_5/p1={p1:.1f}/p1.csv') as f:
        for line in f.readlines()[1:]:
            _, value, *_ = line.strip().split(',')
            value = float(value)
            p_xs.append(value)
            p_ys.append(-value ** 5 + 10 * value ** 3 - 30 * value)

plt.title(f'Comportement de CALICOBA avec un minimum local pour $p(0) = {p_xs[0]}$')
plt.plot(xs, ys, label='$f(p) = -p^5 + 10 p^3 - 30 p$')
if p_xs:
    plt.scatter(p_xs, p_ys, marker='x', color='r', label='$p(t)$')
    plt.vlines(p_xs[0], min(ys), max(ys), color='black', linestyles='--', label='$p(0)$')
    plt.vlines(p_xs[-1], min(ys), max(ys), color='limegreen', linestyles='--', label=f'$p({len(p_xs) - 1})$')
plt.legend()
plt.show()
