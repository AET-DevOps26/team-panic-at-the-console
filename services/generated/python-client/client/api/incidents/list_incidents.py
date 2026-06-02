from http import HTTPStatus
from typing import Any, cast

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.incident_list_response import IncidentListResponse
from ...models.incident_status import IncidentStatus
from ...models.severity import Severity
from ...types import UNSET, Response, Unset


def _get_kwargs(
    *,
    status: IncidentStatus | Unset = UNSET,
    severity: Severity | Unset = UNSET,
    limit: int | Unset = 50,
    offset: int | Unset = 0,
) -> dict[str, Any]:
    params: dict[str, Any] = {}

    json_status: str | Unset = UNSET
    if not isinstance(status, Unset):
        json_status = status.value

    params["status"] = json_status

    json_severity: str | Unset = UNSET
    if not isinstance(severity, Unset):
        json_severity = severity.value

    params["severity"] = json_severity

    params["limit"] = limit

    params["offset"] = offset

    params = {k: v for k, v in params.items() if v is not UNSET and v is not None}

    _kwargs: dict[str, Any] = {
        "method": "get",
        "url": "/incidents",
        "params": params,
    }

    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Any | IncidentListResponse | None:
    if response.status_code == 200:
        response_200 = IncidentListResponse.from_dict(response.json())

        return response_200

    if response.status_code == 401:
        response_401 = cast(Any, None)
        return response_401

    if client.raise_on_unexpected_status:
        raise errors.UnexpectedStatus(response.status_code, response.content)
    else:
        return None


def _build_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Response[Any | IncidentListResponse]:
    return Response(
        status_code=HTTPStatus(response.status_code),
        content=response.content,
        headers=response.headers,
        parsed=_parse_response(client=client, response=response),
    )


def sync_detailed(
    *,
    client: AuthenticatedClient | Client,
    status: IncidentStatus | Unset = UNSET,
    severity: Severity | Unset = UNSET,
    limit: int | Unset = 50,
    offset: int | Unset = 0,
) -> Response[Any | IncidentListResponse]:
    """List incidents with optional filtering

    Args:
        status (IncidentStatus | Unset):
        severity (Severity | Unset): Incident severity. SEV1 is highest impact.
        limit (int | Unset):  Default: 50.
        offset (int | Unset):  Default: 0.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | IncidentListResponse]
    """

    kwargs = _get_kwargs(
        status=status,
        severity=severity,
        limit=limit,
        offset=offset,
    )

    response = client.get_httpx_client().request(
        **kwargs,
    )

    return _build_response(client=client, response=response)


def sync(
    *,
    client: AuthenticatedClient | Client,
    status: IncidentStatus | Unset = UNSET,
    severity: Severity | Unset = UNSET,
    limit: int | Unset = 50,
    offset: int | Unset = 0,
) -> Any | IncidentListResponse | None:
    """List incidents with optional filtering

    Args:
        status (IncidentStatus | Unset):
        severity (Severity | Unset): Incident severity. SEV1 is highest impact.
        limit (int | Unset):  Default: 50.
        offset (int | Unset):  Default: 0.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | IncidentListResponse
    """

    return sync_detailed(
        client=client,
        status=status,
        severity=severity,
        limit=limit,
        offset=offset,
    ).parsed


async def asyncio_detailed(
    *,
    client: AuthenticatedClient | Client,
    status: IncidentStatus | Unset = UNSET,
    severity: Severity | Unset = UNSET,
    limit: int | Unset = 50,
    offset: int | Unset = 0,
) -> Response[Any | IncidentListResponse]:
    """List incidents with optional filtering

    Args:
        status (IncidentStatus | Unset):
        severity (Severity | Unset): Incident severity. SEV1 is highest impact.
        limit (int | Unset):  Default: 50.
        offset (int | Unset):  Default: 0.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | IncidentListResponse]
    """

    kwargs = _get_kwargs(
        status=status,
        severity=severity,
        limit=limit,
        offset=offset,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    *,
    client: AuthenticatedClient | Client,
    status: IncidentStatus | Unset = UNSET,
    severity: Severity | Unset = UNSET,
    limit: int | Unset = 50,
    offset: int | Unset = 0,
) -> Any | IncidentListResponse | None:
    """List incidents with optional filtering

    Args:
        status (IncidentStatus | Unset):
        severity (Severity | Unset): Incident severity. SEV1 is highest impact.
        limit (int | Unset):  Default: 50.
        offset (int | Unset):  Default: 0.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | IncidentListResponse
    """

    return (
        await asyncio_detailed(
            client=client,
            status=status,
            severity=severity,
            limit=limit,
            offset=offset,
        )
    ).parsed
