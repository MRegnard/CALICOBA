import dataclasses
import typing as typ


@dataclasses.dataclass(frozen=True)
class Suggestion:
    decision: str
    next_point: typ.Optional[float] = None
    direction: typ.Optional[float] = None
    step: typ.Optional[float] = None


@dataclasses.dataclass(frozen=True)
class LocalSearchSuggestion(Suggestion):
    pass


@dataclasses.dataclass(frozen=True)
class SemiLocalSearchSuggestion(Suggestion):
    from_value: typ.Optional[float] = None
    new_chain_next: bool = False
    check_for_out_of_bounds: bool = False


@dataclasses.dataclass(frozen=True)
class HillClimbSuggestion(Suggestion):
    new_chain_next: bool = False
