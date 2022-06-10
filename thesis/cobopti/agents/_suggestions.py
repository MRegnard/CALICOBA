import dataclasses
import enum

from .. import _data_types as dt


class SearchPhase(enum.Enum):
    LOCAL = 1
    HILL_CLIMBING = 2
    SEMI_LOCAL = 3
    LOCAL_SAMPLING = 4


@dataclasses.dataclass(frozen=True)
class VariationSuggestion:
    """Object containing an action suggestion."""
    search_phase: SearchPhase
    next_point: dt.Vector[float]
    decision: str
    criticality: float
    local_min_found: bool
    new_chain_next: bool = False
    steps: dt.Vector[float] = None
    directions: dt.Vector[int] = None
    # Used to prioritize some suggestions over others in rare cases
    # when several point agents make a suggestion at the same time
    priority: int = 0
    agent: '' = None  # Mandatory
    """:type: cobopti.agents.PointAgent"""
