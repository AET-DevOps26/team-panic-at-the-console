from http import HTTPStatus
from typing import Any, cast
from urllib.parse import quote
from uuid import UUID

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.solutions_response import SolutionsResponse
from ...types import Response


def _get_kwargs(
    incident_id: UUID,
) -> dict[str, Any]:
    _kwargs: dict[str, Any] = {
        "method": "get",
        "url": "/incidents/{incident_id}/genai/solutions".format(
            incident_id=quote(str(incident_id), safe=""),
        ),
    }

    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Any | SolutionsResponse | None:
    if response.status_code == 200:
        response_200 = SolutionsResponse.from_dict(response.json())

        return response_200

    if response.status_code == 404:
        response_404 = cast(Any, None)
        return response_404

    if client.raise_on_unexpected_status:
        raise errors.UnexpectedStatus(response.status_code, response.content)
    else:
        return None


def _build_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Response[Any | SolutionsResponse]:
    return Response(
        status_code=HTTPStatus(response.status_code),
        content=response.content,
        headers=response.headers,
        parsed=_parse_response(client=client, response=response),
    )


def sync_detailed(
    incident_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Response[Any | SolutionsResponse]:
    """Get latest AI-generated solution suggestions for an incident

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | SolutionsResponse]
    """

    kwargs = _get_kwargs(
        incident_id=incident_id,
    )

    response = client.get_httpx_client().request(
        **kwargs,
    )

    return _build_response(client=client, response=response)


def sync(
    incident_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Any | SolutionsResponse | None:
    """Get latest AI-generated solution suggestions for an incident

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | SolutionsResponse
    """

    return sync_detailed(
        incident_id=incident_id,
        client=client,
    ).parsed


async def asyncio_detailed(
    incident_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Response[Any | SolutionsResponse]:
    """Get latest AI-generated solution suggestions for an incident

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | SolutionsResponse]
    """

    kwargs = _get_kwargs(
        incident_id=incident_id,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    incident_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Any | SolutionsResponse | None:
    """Get latest AI-generated solution suggestions for an incident

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | SolutionsResponse
    """

    return (
        await asyncio_detailed(
            incident_id=incident_id,
            client=client,
        )
    ).parsed
