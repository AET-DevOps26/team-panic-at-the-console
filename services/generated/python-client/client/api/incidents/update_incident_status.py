from http import HTTPStatus
from typing import Any, cast
from urllib.parse import quote
from uuid import UUID

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.error_response import ErrorResponse
from ...models.incident import Incident
from ...models.update_status_request import UpdateStatusRequest
from ...types import Response


def _get_kwargs(
    incident_id: UUID,
    *,
    body: UpdateStatusRequest,
) -> dict[str, Any]:
    headers: dict[str, Any] = {}

    _kwargs: dict[str, Any] = {
        "method": "patch",
        "url": "/incidents/{incident_id}/status".format(
            incident_id=quote(str(incident_id), safe=""),
        ),
    }

    _kwargs["json"] = body.to_dict()

    headers["Content-Type"] = "application/json"

    _kwargs["headers"] = headers
    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Any | ErrorResponse | Incident | None:
    if response.status_code == 200:
        response_200 = Incident.from_dict(response.json())

        return response_200

    if response.status_code == 400:
        response_400 = ErrorResponse.from_dict(response.json())

        return response_400

    if response.status_code == 401:
        response_401 = cast(Any, None)
        return response_401

    if response.status_code == 403:
        response_403 = cast(Any, None)
        return response_403

    if response.status_code == 404:
        response_404 = cast(Any, None)
        return response_404

    if client.raise_on_unexpected_status:
        raise errors.UnexpectedStatus(response.status_code, response.content)
    else:
        return None


def _build_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Response[Any | ErrorResponse | Incident]:
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
    body: UpdateStatusRequest,
) -> Response[Any | ErrorResponse | Incident]:
    """Transition incident status

     Any transition between distinct statuses is allowed (including reopening
    a resolved incident, which clears resolvedAt).
    Requires RESPONDER or COMMANDER role.

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (UpdateStatusRequest): Request to update incident status.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | ErrorResponse | Incident]
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
    body: UpdateStatusRequest,
) -> Any | ErrorResponse | Incident | None:
    """Transition incident status

     Any transition between distinct statuses is allowed (including reopening
    a resolved incident, which clears resolvedAt).
    Requires RESPONDER or COMMANDER role.

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (UpdateStatusRequest): Request to update incident status.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | ErrorResponse | Incident
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
    body: UpdateStatusRequest,
) -> Response[Any | ErrorResponse | Incident]:
    """Transition incident status

     Any transition between distinct statuses is allowed (including reopening
    a resolved incident, which clears resolvedAt).
    Requires RESPONDER or COMMANDER role.

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (UpdateStatusRequest): Request to update incident status.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | ErrorResponse | Incident]
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
    body: UpdateStatusRequest,
) -> Any | ErrorResponse | Incident | None:
    """Transition incident status

     Any transition between distinct statuses is allowed (including reopening
    a resolved incident, which clears resolvedAt).
    Requires RESPONDER or COMMANDER role.

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (UpdateStatusRequest): Request to update incident status.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | ErrorResponse | Incident
    """

    return (
        await asyncio_detailed(
            incident_id=incident_id,
            client=client,
            body=body,
        )
    ).parsed
