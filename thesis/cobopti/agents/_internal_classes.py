import dataclasses
import typing as typ


@dataclasses.dataclass(frozen=True)
class Suggestion:
    """Base class for action suggestions during the various search phases."""
    decision: str
    next_point: typ.Optional[float] = None
    direction: typ.Optional[float] = None
    step: typ.Optional[float] = None


@dataclasses.dataclass(frozen=True)
class LocalSearchSuggestion(Suggestion):
    """Object containing an action suggestion for the local search phase."""
    pass


@dataclasses.dataclass(frozen=True)
class SemiLocalSearchSuggestion(Suggestion):
    """Object containing an action suggestion for the semi-local search phase."""
    from_value: typ.Optional[float] = None
    new_chain_next: bool = False
    check_for_out_of_bounds: bool = False


@dataclasses.dataclass(frozen=True)
class HillClimbSuggestion(Suggestion):
    """Object containing an action suggestion for the hill-climbing phase."""
    new_chain_next: bool = False
