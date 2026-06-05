"""Direct tests of LogosClient.generate against the real TUM Logos endpoint.

These verify the wiring: that we send a request shape Logos accepts, that
`response_format=json_schema` is honored, and that we parse the response back
into a Pydantic model. Assertions are structural only - we never check exact text.

Opt-in: requires LOGOS_INTEGRATION_KEY and the TUM network / eduVPN (see conftest).
"""

import pytest
from pydantic import BaseModel, Field

from genai_service.logos_client import LogosClient


class CityInfo(BaseModel):
    city: str = Field(min_length=1)
    country: str = Field(min_length=1)


@pytest.mark.integration
@pytest.mark.flaky(reruns=3)
async def test_generate_returns_text(logos_client: LogosClient):
    text = await logos_client.generate(
        "Reply with a single short greeting and nothing else.",
        system="You are concise. Reply with at most five words.",
    )
    assert isinstance(text, str)
    assert text.strip() != ""


@pytest.mark.integration
@pytest.mark.flaky(reruns=3)
async def test_generate_with_response_model_returns_typed_instance(
    logos_client: LogosClient,
):
    result = await logos_client.generate(
        "Give me the city and country where the Eiffel Tower is located.",
        system="Reply in JSON with the requested fields.",
        response_model=CityInfo,
    )
    assert isinstance(result, CityInfo)
    # The Eiffel Tower's location is effectively deterministic; substring + casefold
    # keeps the content check meaningful while tolerating "City of Paris" / casing.
    assert "paris" in result.city.casefold()
    assert "france" in result.country.casefold()
