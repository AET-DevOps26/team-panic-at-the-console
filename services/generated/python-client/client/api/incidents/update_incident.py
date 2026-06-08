from http import HTTPStatus
from typing import Any, cast
from urllib.parse import quote
from uuid import UUID

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.incident import Incident
from ...models.update_incident_request import UpdateIncidentRequest
from ...types import Response


def _get_kwargs(
    incident_id: UUID,
    *,
    body: UpdateIncidentRequest,
) -> dict[str, Any]:
    headers: dict[str, Any] = {}

    _kwargs: dict[str, Any] = {
        "method": "patch",
        "url": "/incidents/{incident_id}".format(
            incident_id=quote(str(incident_id), safe=""),
        ),
    }

    _kwargs["json"] = body.to_dict()

    headers["Content-Type"] = "application/json"

    _kwargs["headers"] = headers
    return _kwargs


def _parse_response(*, client: AuthenticatedClient | Client, response: httpx.Response) -> Any | Incident | None:
    if response.status_code == 200:
        response_200 = Incident.from_dict(response.json())

        return response_200

    if response.status_code == 404:
        response_404 = cast(Any, None)
        return response_404

    if client.raise_on_unexpected_status:
        raise errors.UnexpectedStatus(response.status_code, response.content)
    else:
        return None


def _build_response(*, client: AuthenticatedClient | Client, response: httpx.Response) -> Response[Any | Incident]:
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
    body: UpdateIncidentRequest,
) -> Response[Any | Incident]:
    """Update incident status or severity

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (UpdateIncidentRequest): Partial update for an incident's mutable fields.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | Incident]
    """

    kwargs = _get_kwargs(
        incident_id=incident_id,
        body=body,
    )

    response = client.get_httpx_client().request(
        **kwargs,
    )

    return _build_response(client=client, response=response)


def sync(
    incident_id: UUID,
    *,
    client: AuthenticatedClient | Client,
    body: UpdateIncidentRequest,
) -> Any | Incident | None:
    """Update incident status or severity

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (UpdateIncidentRequest): Partial update for an incident's mutable fields.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | Incident
    """

    return sync_detailed(
        incident_id=incident_id,
        client=client,
        body=body,
    ).parsed


async def asyncio_detailed(
    incident_id: UUID,
    *,
    client: AuthenticatedClient | Client,
    body: UpdateIncidentRequest,
) -> Response[Any | Incident]:
    """Update incident status or severity

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (UpdateIncidentRequest): Partial update for an incident's mutable fields.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | Incident]
    """

    kwargs = _get_kwargs(
        incident_id=incident_id,
        body=body,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    incident_id: UUID,
    *,
    client: AuthenticatedClient | Client,
    body: UpdateIncidentRequest,
) -> Any | Incident | None:
    """Update incident status or severity

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (UpdateIncidentRequest): Partial update for an incident's mutable fields.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | Incident
    """

    return (
        await asyncio_detailed(
            incident_id=incident_id,
            client=client,
            body=body,
        )
    ).parsed
