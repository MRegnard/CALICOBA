import dataclasses
import pathlib
import typing as typ


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
class ExperimentResult:
    solution_found: bool
    error: bool
    speed: int
    points_number: int = None
    unique_points_number: int = None
    error_message: str = None
