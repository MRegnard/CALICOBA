import dataclasses

from cobopti.agents import PointAgent


@dataclasses.dataclass(frozen=True)
class VariationSuggestion:
    """Object containing an action suggestion for a model variable."""
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
    """When a point agent returns an instance of this class,
    this means that it represents a global minimum of the optimization problem.
    """
    pass
