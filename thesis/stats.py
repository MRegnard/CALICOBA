#!/usr/bin/python3
import dataclasses
import json
import math
import pathlib
import sys
import typing as typ

import numpy as np

import test_utils


@dataclasses.dataclass(frozen=True)
class StatsObject:
    mean: float
    median: float
    std_dev: float
    minimum: float
    maximum: float

    def __str__(self):
        return f'Mean: {self.mean:.4f}; Median: {self.median:.4f}; Std dev: {self.std_dev:.4f};' \
               f' Min: {self.minimum:.4f}; Max: {self.maximum:.4f}'


class DataSet:
    def __init__(self, file: pathlib.Path):
        self.total_runs = 0
        self.successes_number = 0
        self.errors_number = 0
        self.cycles_numbers = []
        self.solution_cycles = []
        self.speeds = []
        self.visited_points_numbers = []
        self.unique_visited_points_number = []
        self.created_chains = []
        self.distance_with_expected = []
        self.error_messages = {}

        self.cycles_numbers_success = []
        self.solution_cycles_success = []
        self.speeds_success = []
        self.visited_points_numbers_success = []
        self.unique_visited_points_number_success = []
        self.created_chains_success = []
        self.distance_with_expected_success = []

        self.cycles_numbers_failure = []
        self.speeds_failure = []
        self.visited_points_numbers_failure = []
        self.distance_with_expected_failure = []

        with file.open(encoding='utf8') as f:
            for item in json.load(f):
                x0 = dict(item['x0'])
                solution_found = bool(item['solution_found'])
                error = bool(item['error'])
                if solution_found:
                    self.successes_number += 1
                    self.cycles_numbers_success.append(int(item['cycles']))
                    self.solution_cycles_success.append(int(item['solution_cycle']))
                    self.speeds_success.append(float(item['speed']))
                    self.visited_points_numbers_success.append(int(item['points_number']))
                    self.unique_visited_points_number_success.append(int(item['unique_points_number']))
                    self.created_chains_success.append(int(item['chains_number']))
                    self.distance_with_expected_success.append(float(item['distance_with_expected']))
                else:
                    if error:
                        self.errors_number += 1
                    else:
                        self.distance_with_expected_failure.append(float(item['distance_with_expected']))
                    self.speeds_failure.append(float(item['speed']))

                self.total_runs += 1
                self.cycles_numbers.append(int(item['cycles']))
                self.solution_cycles.append(int(item['solution_cycle']))
                self.speeds.append(float(item['speed']))
                self.visited_points_numbers.append(int(item['points_number']))
                self.unique_visited_points_number.append(int(item['unique_points_number']))
                self.created_chains.append(int(item['chains_number']))
                self.distance_with_expected.append(float(item['distance_with_expected']))

                if error_message := item['error_message']:
                    self.error_messages[test_utils.map_to_string(x0)] = str(error_message)

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
    def total_error_rate(self) -> float:
        if self.total_runs == self.successes_number:
            return math.nan
        return self.errors_number / self.total_runs

    @property
    def cycles_numbers_stats(self) -> StatsObject:
        return self._get_stats(self.cycles_numbers)

    @property
    def cycles_numbers_stats_success(self) -> StatsObject:
        return self._get_stats(self.cycles_numbers_success)

    @property
    def solution_cycles_stats(self) -> StatsObject:
        return self._get_stats(self.solution_cycles)

    @property
    def solution_cycles_stats_success(self) -> StatsObject:
        return self._get_stats(self.solution_cycles_success)

    @property
    def speed_stats(self) -> StatsObject:
        return self._get_stats(self.speeds)

    @property
    def speed_stats_success(self) -> StatsObject:
        return self._get_stats(self.speeds_success)

    @property
    def speed_stats_failure(self) -> StatsObject:
        return self._get_stats(self.speeds_failure)

    @property
    def visited_points_stats(self) -> StatsObject:
        return self._get_stats(self.visited_points_numbers)

    @property
    def visited_points_stats_success(self) -> StatsObject:
        return self._get_stats(self.visited_points_numbers_success)

    @property
    def unique_points_stats(self) -> StatsObject:
        return self._get_stats(self.unique_visited_points_number)

    @property
    def unique_points_stats_success(self) -> StatsObject:
        return self._get_stats(self.unique_visited_points_number_success)

    @property
    def created_chains_stats(self) -> StatsObject:
        return self._get_stats(self.created_chains)

    @property
    def created_chains_stats_success(self) -> StatsObject:
        return self._get_stats(self.created_chains_success)

    @property
    def distance_with_expected_stats(self) -> StatsObject:
        return self._get_stats(self.distance_with_expected)

    @property
    def distance_with_expected_stats_success(self) -> StatsObject:
        return self._get_stats(self.distance_with_expected_success)

    @property
    def distance_with_expected_stats_failure(self) -> StatsObject:
        return self._get_stats(self.distance_with_expected_failure)

    @staticmethod
    def _get_stats(values: typ.List[typ.Union[int, float]]) -> StatsObject:
        # noinspection PyTypeChecker
        return StatsObject(
            mean=np.mean(values),
            median=np.median(values),
            std_dev=np.std(values),
            minimum=min(values),
            maximum=max(values)
        )

    def __str__(self):
        res = "\n" + f"""
Successes: {self.successes_number}/{self.total_runs} ({self.success_rate * 100:.2f}%)
Failures:  {self.failures_number}/{self.total_runs} ({self.failure_rate * 100:.2f}%)

Cycles numbers stats:        {self.cycles_numbers_stats}
Solution cycles stats:       {self.solution_cycles_stats}
Speed stats (s):             {self.speed_stats}
Visited points stats:        {self.visited_points_stats}
Unique visited points stats: {self.unique_points_stats}
Created chains stats:        {self.created_chains_stats}
Distance to expected stats : {self.distance_with_expected_stats}
""".strip()

        if self.successes_number > 0:
            res += "\n\n\t" + f"""
When the solution has been found ({self.successes_number} time(s)):
Cycles numbers stats:        {self.cycles_numbers_stats_success}
Solution cycles stats:       {self.solution_cycles_stats_success}
Speed stats (s):             {self.speed_stats_success}
Visited points stats:        {self.visited_points_stats_success}
Unique visited points stats: {self.unique_points_stats_success}
Created chains stats:        {self.created_chains_stats_success}
Distance to expected point:  {self.distance_with_expected_stats_success}
""".strip()

        if self.failures_number > 0:
            res += "\n\n\t" + f"""
When the solution has not been found ({self.failures_number} time(s)):
Number of errors:            {self.errors_number}/{self.failures_number} ({self.error_rate * 100:.2f}% of failures,\
 {self.total_error_rate * 100:.2f}% of total)
Speed stats (s):             {self.speed_stats_failure}
Distance (when no error) :   {self.distance_with_expected_stats_failure}
""".strip()

        return res


def main():
    path = pathlib.Path(sys.argv[1]).absolute()
    if path.is_file():
        print(DataSet(path))
    elif path.is_dir():
        for file in sorted(path.glob('*.json')):
            if file.is_file():
                print(f'{file.stem}:')
                print(DataSet(file))
                print()


if __name__ == '__main__':
    main()
