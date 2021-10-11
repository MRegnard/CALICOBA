#!/usr/bin/python3
import pathlib
import sys
import typing as typ

import numpy


def to_dict(s: str) -> typ.Dict[str, float]:
    def aux(i: str) -> typ.Tuple[str, float]:
        name, value = i.split('=')
        return name, float(value)

    return {k: v for k, v in map(aux, s.split(';'))}


def stats(filename: pathlib.Path) -> typ.Dict[str, float]:
    with filename.open(encoding='UTF-8') as f:
        total_runs = 0
        failures = []
        failures_range = []
        failures_dist = []
        failures_speed = []
        distances = []
        speeds = []
        for line in f.readlines()[1:]:
            p0, result, closest_solution, distance, speed = line.split(',')
            p0 = to_dict(p0)
            result = to_dict(result)
            closest_solution = to_dict(closest_solution)
            distance = float(distance)
            speed = int(speed)
            total_runs += 1

            distances.append(distance)
            speeds.append(speed)

            append = True
            if any([abs(p) == 1500 for p in result.values()]):
                failures_range.append(result)
                failures.append(result)
                append = False
            if distance > 1:
                failures_dist.append(result)
                if append:
                    failures.append(result)
                    append = False
            if speed == 990:
                failures_speed.append(result)
                if append:
                    failures.append(result)

        distances.sort()
        speeds.sort()

        l0 = len(failures)
        l1 = len(failures_range)
        l2 = len(failures_dist)
        l3 = len(failures_speed)
        l1_pc = 100 * l1 / total_runs
        l2_pc = 100 * l2 / total_runs
        l3_pc = 100 * l3 / total_runs

        dist_mean = numpy.mean(distances)
        dist_med = numpy.median(distances)
        dist_std = numpy.std(distances)
        speed_mean = numpy.mean(speeds)
        speed_med = numpy.median(speeds)
        speed_std = numpy.std(speeds)

        print(f'Nombre de dépassements de domaine : '
              f'{l1}/{total_runs} ({l1_pc:.2f} %)')
        print(f'Nombre de résultats trop éloignés : '
              f'{l2}/{total_runs} ({l2_pc:.2f} %)')
        print(f'Nombre de cas trop lents : '
              f'{l3}/{total_runs} ({l3_pc:.2f} %)')
        print(f'Nombre de cas problématiques (dépassement + distance + vitesse) : '
              f'{l0}/{total_runs} ({100 * l0 / total_runs:.2f} %)')

        print('Moyenne des distances :', dist_mean)
        print('Médiane des distances :', dist_med)
        print('Écart-type des distances :', dist_std)

        print('Moyenne des vitesses :', speed_mean)
        print('Médiane des vitesses :', speed_med)
        print('Écart-type des vitesses :', speed_std)

        return {
            'm3a': l1_pc,
            'm3b': l2_pc,
            'm3c': l3_pc,
            'dist_mean': dist_mean,
            'dist_med': dist_med,
            'dist_std': dist_std,
            'speed_mean': speed_mean,
            'speed_med': speed_med,
            'speed_std': speed_std,
        }


def main():
    filename = pathlib.Path(sys.argv[1]).absolute()
    if filename.is_file():
        stats(filename)
    elif filename.is_dir():
        table_data = {}
        for fname in sorted(filename.glob('*.csv')):
            if fname.is_file():
                table_data[fname.stem] = stats(fname)

        def get_stacked_data(key: str):
            m, var = ('M1', 'D') if key == 'dist' else ('M2', 'N_c')
            return (fr'\textbf{{{m}}} & '
                    + ' & '.join([fr'\Longunderstack{{$E({var}) = {v[key + "_mean"]:.1f}$\\'
                                  fr'$M({var}) = {v[key + "_med"]:.1f}$\\'
                                  fr'$\sigma({var}) = {v[key + "_std"]:.1f}$}}'
                                  for v in table_data.values()]))

        def get_data(key: str):
            return (fr'\textbf{{{key.capitalize()}}} & '
                    + ' & '.join([fr'{v[key]:.1f}~\%' for v in table_data.values()]))

        print(table_data)
        print(fr'\begin{{tabular}}{{|{"|".join("c" * (len(table_data) + 1))}|}}')
        print(r'    \hline')
        print(r'    ~ & ' + ' & '.join([fr'\textbf{{{k.replace("model_", "Modèle ")}}}' for k in table_data.keys()])
              + r' \\')
        print(r'    \hline')
        print(fr'    {get_stacked_data("dist")} \\')
        print(r'    \hline')
        print(fr'    {get_stacked_data("speed")} \\')
        print(r'    \hline')
        print(fr'    {get_data("m3a")} \\')
        print(r'    \hline')
        print(fr'    {get_data("m3b")} \\')
        print(r'    \hline')
        print(fr'    {get_data("m3c")} \\')
        print(r'    \hline')
        print(r'\end{tabular}')


if __name__ == '__main__':
    main()
