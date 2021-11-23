import dataclasses


@dataclasses.dataclass(frozen=True)
class ExperimentResult:
    solution_found: bool
    error: bool
    speed: int
    points_number: int = None
    unique_points_number: int = None
