import dataclasses
import enum


class SearchPhase(enum.Enum):
    LOCAL = 1
    HILL_CLIMBING = 2
    SEMI_LOCAL = 3


@dataclasses.dataclass(frozen=True)
class VariationSuggestion:
    """Object containing an action suggestion for a model variable."""
    agent: 'cobopti.PointAgent'
    search_phase: SearchPhase
    next_point: float
    decision: str
    criticality: float
    local_min_found: bool
    new_chain_next: bool = False
    step: float = None
    direction: int = None
