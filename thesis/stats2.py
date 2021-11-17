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
        failures_not_found = 0
        errors = 0
        speeds = []
        for line in f.readlines()[1:]:
            p0, solution_found, error, speed = line.split(',')
            solution_found = int(solution_found)
            error = int(error)
            speed = int(speed)
            total_runs += 1

            speeds.append(speed)

            if not solution_found:
                failures_not_found += 1
            if error:
                errors += 1

        speeds.sort()

        not_found_pc = 100 * failures_not_found / total_runs
        errors_rate = 100 * errors / failures_not_found

        speed_mean = numpy.mean(speeds)
        speed_med = numpy.median(speeds)
        speed_std = numpy.std(speeds)

        print(f'Nombre d’échecs (solution non trouvée) : '
              f'{failures_not_found}/{total_runs} ({not_found_pc:.2f} %)')
        print(f'Dont exceptions : '
              f'{errors}/{failures_not_found} ({errors_rate:.2f} %)')

        print('Moyenne des vitesses :', speed_mean)
        print('Médiane des vitesses :', speed_med)
        print('Écart-type des vitesses :', speed_std)

        return {
            'not_found': not_found_pc,
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
            m, var = 'Speed', 'N_c'
            return (fr'\textbf{{{m}}} & '
                    + ' & '.join([fr'\Longunderstack{{$E({var}) = {v[key + "_mean"]:.1f}$\\'
                                  fr'$M({var}) = {v[key + "_med"]:.1f}$\\'
                                  fr'$\sigma({var}) = {v[key + "_std"]:.1f}$}}'
                                  for v in table_data.values()]))

        def get_data(key: str):
            return (fr'\textbf{{{key.replace("_", " ").capitalize()}}} & '
                    + ' & '.join([fr'{v[key]:.1f}~\%' for v in table_data.values()]))

        print(table_data)
        print(fr'\begin{{tabular}}{{|{"|".join("c" * (len(table_data) + 1))}|}}')
        print(r'    \hline')
        print(r'    ~ & ' + ' & '.join([fr'\textbf{{{k}}}' for k in table_data.keys()])
              + r' \\')
        print(r'    \hline')
        print(fr'    {get_data("not_found")} \\')
        print(r'    \hline')
        print(fr'    {get_stacked_data("speed")} \\')
        print(r'    \hline')
        print(r'\end{tabular}')


if __name__ == '__main__':
    main()
