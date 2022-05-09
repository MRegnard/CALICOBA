import dataclasses
import logging
import pathlib
import typing as typ

import models
import test_utils


@dataclasses.dataclass(frozen=True)
class ExperimentsConfig:
    method: str
    null_crit_threshold: float
    runs_number: int
    max_steps: int
    step_by_step: bool
    output_directory: pathlib.Path
    dump_data: bool
    log_level: int
    noisy_functions: bool
    noise_mean: float
    noise_stdev: float
    model_id: typ.Optional[str] = None
    seed: typ.Optional[int] = None
    parameters_values: typ.Sequence[str] = ()
    free_parameter: typ.Optional[str] = None


@dataclasses.dataclass(frozen=True)
class RunConfig:
    method: str
    model: models.Model
    p_init: test_utils.Map
    target_parameters: typ.Sequence[test_utils.Map]
    max_steps: int
    step_by_step: bool = False
    seed: typ.Optional[int] = None
    free_parameter: typ.Optional[str] = None
    noisy: bool = False
    noise_mean: typ.Optional[float] = None
    noise_stdev: typ.Optional[float] = None
    output_dir: pathlib.Path = None
    logger: logging.Logger = None
    logging_level: int = logging.INFO


@dataclasses.dataclass(frozen=True)
class RunResult:
    solution_found: bool
    error: bool
    cycles_number: int
    solution_cycle: int
    time: float
    points_number: int = None
    unique_points_number: int = None
    created_chains: int = None
    error_message: str = None
