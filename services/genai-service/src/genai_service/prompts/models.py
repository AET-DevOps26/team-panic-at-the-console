from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

Severity = Literal["SEV1", "SEV2", "SEV3", "SEV4"]
IncidentStatus = Literal["open", "investigating", "resolved"]


class _CamelModel(BaseModel):
    # incident-service exposes camelCase JSON (Spring/Jackson default). We keep
    # snake_case attributes and translate at the boundary.
    model_config = ConfigDict(
        extra="ignore", alias_generator=to_camel, populate_by_name=True
    )


class Event(_CamelModel):
    """One entry from an incident's Event Log (see CONTEXT.md). Chronological, immutable."""

    timestamp: datetime
    type: str
    description: str


class Incident(_CamelModel):
    """Subset of incident-service's Incident representation that the PromptBuilder needs."""

    id: str
    title: str
    description: str | None = None
    status: IncidentStatus
    severity: Severity
    created_at: datetime
    resolved_at: datetime | None = None


class SummaryResponse(BaseModel):
    summary: str = Field(
        min_length=1,
        description="One- or two-sentence description of the incident's current state.",
    )


class SeverityResponse(BaseModel):
    severity: Severity = Field(description="Suggested severity for this incident.")
    reason: str = Field(min_length=1, description="Why this severity fits.")


class SolutionsResponse(BaseModel):
    solutions: list[str] = Field(
        min_length=1,
        description="Concrete next steps an on-call engineer can try, ordered by likelihood.",
    )


class PostmortemResponse(BaseModel):
    root_cause: str = Field(min_length=1)
    timeline: list[str] = Field(
        min_length=1, description="Chronological bullet points of what happened."
    )
    action_items: list[str] = Field(
        min_length=1, description="Concrete follow-ups to prevent recurrence."
    )
