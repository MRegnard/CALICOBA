import dataclasses

from cobopti.agents import PointAgent


@dataclasses.dataclass(frozen=True)
class VariationSuggestion:
    agent: PointAgent
    next_point: float
    decision: str
    selected_objective: str
    criticality: float
    local_min_found: bool
    new_chain_next: bool = False
    step: float = None
    direction: int = None


@dataclasses.dataclass(frozen=True)
class GlobalMinimumFound:
    pass