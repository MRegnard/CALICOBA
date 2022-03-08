#!/usr/bin/python3
import dataclasses
import math
import pathlib
import sys
import typing as typ

import numpy as np


@dataclasses.dataclass(frozen=True)
class StatsObject:
    mean: float
    median: float
    std_dev: float

    def __str__(self):
        return f'Mean: {self.mean:.4f}; Median: {self.median:.4f}; Std dev: {self.std_dev:.4f}'


class DataSet:
    def __init__(self, file: pathlib.Path):
        self.total_runs = 0
        self.successes_number = 0
        self.errors_number = 0
        self.cycles_numbers = []
        self.soluction_cycles = []
        self.speeds = []
        self.visited_points_numbers = []
        self.unique_visited_points_number = []
        self.error_messages = {}

        with file.open(encoding='utf8') as f:
            for line in f.readlines()[1:]:
                (p0, solution_found, error, cycles_number, solution_cycle,
                 speed, nb_points, unique_points, error_message) = line.split(',', maxsplit=8)
                if int(solution_found):
                    self.successes_number += 1
                if int(error):
                    self.errors_number += 1
                self.total_runs += 1
                self.cycles_numbers.append(int(cycles_number))
                self.soluction_cycles.append(int(solution_cycle))
                self.speeds.append(float(speed))
                self.visited_points_numbers.append(int(nb_points))
                self.unique_visited_points_number.append(int(unique_points))
                if error_message:
                    self.error_messages[p0] = error_message

    @property
    def failures_number(self) -> int:
        return self.total_runs - self.successes_number

    @property
    def success_rate(self) -> float:
        return self.successes_number / self.total_runs

    @property
    def failure_rate(self) -> float:
        return (self.total_runs - self.successes_number) / self.total_runs

    @property
    def error_rate(self) -> float:
        if self.total_runs == self.successes_number:
            return math.nan
        return self.errors_number / (self.total_runs - self.successes_number)

    @property
    def cycles_numbers_stats(self) -> StatsObject:
        return self._get_stats(self.cycles_numbers)

    @property
    def solution_cycles_stats(self) -> StatsObject:
        return self._get_stats(self.soluction_cycles)

    @property
    def speed_stats(self) -> StatsObject:
        return self._get_stats(self.speeds)

    @property
    def visited_points_stats(self) -> StatsObject:
        return self._get_stats(self.visited_points_numbers)

    @property
    def unique_points_stats(self) -> StatsObject:
        return self._get_stats(self.unique_visited_points_number)

    @staticmethod
    def _get_stats(values: typ.List[typ.Union[int, float]]) -> StatsObject:
        # noinspection PyTypeChecker
        return StatsObject(
            mean=np.mean(values),
            median=np.median(values),
            std_dev=np.std(values),
        )

    def __str__(self):
        return f"""
Successes: {self.successes_number}/{self.total_runs} ({self.success_rate * 100:.2f} %)
Failures:  {self.failures_number}/{self.total_runs} ({self.failure_rate * 100:.2f} %)
Errors:    {self.errors_number}/{self.failures_number} ({self.error_rate * 100:.2f} %)
Cycles numbers stats:        {self.cycles_numbers_stats}
Solution cycles stats:       {self.solution_cycles_stats}
Speed stats (s):             {self.speed_stats}
Visited points stats:        {self.visited_points_stats}
Unique visited points stats: {self.unique_points_stats}
""".strip()


def main():
    path = pathlib.Path(sys.argv[1]).absolute()
    if path.is_file():
        print(DataSet(path))
    elif path.is_dir():
        for file in sorted(path.glob('*.csv')):
            if file.is_file():
                print(f'{file.stem}:')
                print(DataSet(file))
                print()


if __name__ == '__main__':
    main()
