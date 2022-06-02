import dataclasses
import typing as typ


@dataclasses.dataclass(frozen=True)
class Suggestion:
    """Base class for action suggestions during the various search phases."""
    decision: str
    next_point: typ.Optional[float] = None
    direction: typ.Optional[float] = None
    step: typ.Optional[float] = None
    from_value: typ.Optional[float] = None
    distance_to_neighbor: typ.Optional[float] = None
    # Used to prioritize some suggestions over others in rare cases
    # when several point agents make a suggestion at the same time
    priority: int = 0


@dataclasses.dataclass(frozen=True)
class LocalSearchSuggestion(Suggestion):
    """Object containing an action suggestion for the local search phase."""
    pass


@dataclasses.dataclass(frozen=True)
class SemiLocalSearchSuggestion(Suggestion):
    """Object containing an action suggestion for the semi-local search phase."""
    new_chain_next: bool = False
    check_for_out_of_bounds: bool = False


@dataclasses.dataclass(frozen=True)
class HillClimbSuggestion(Suggestion):
    """Object containing an action suggestion for the hill-climbing phase."""
    new_chain_next: bool = False
