from pydantic import BaseModel, Field

from client.models import Severity


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
