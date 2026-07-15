from http import HTTPStatus
from typing import Any, cast
from urllib.parse import quote
from uuid import UUID

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.external_event_detail import ExternalEventDetail
from ...types import Response


def _get_kwargs(
    external_event_id: UUID,
) -> dict[str, Any]:
    _kwargs: dict[str, Any] = {
        "method": "get",
        "url": "/external-events/{external_event_id}".format(
            external_event_id=quote(str(external_event_id), safe=""),
        ),
    }

    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Any | ExternalEventDetail | None:
    if response.status_code == 200:
        response_200 = ExternalEventDetail.from_dict(response.json())

        return response_200

    if response.status_code == 401:
        response_401 = cast(Any, None)
        return response_401

    if response.status_code == 404:
        response_404 = cast(Any, None)
        return response_404

    if client.raise_on_unexpected_status:
        raise errors.UnexpectedStatus(response.status_code, response.content)
    else:
        return None


def _build_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Response[Any | ExternalEventDetail]:
    return Response(
        status_code=HTTPStatus(response.status_code),
        content=response.content,
        headers=response.headers,
        parsed=_parse_response(client=client, response=response),
    )


def sync_detailed(
    external_event_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Response[Any | ExternalEventDetail]:
    """Fetch a single external event including its raw payload

    Args:
        external_event_id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000e1.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | ExternalEventDetail]
    """

    kwargs = _get_kwargs(
        external_event_id=external_event_id,
    )

    response = client.get_httpx_client().request(
        **kwargs,
    )

    return _build_response(client=client, response=response)


def sync(
    external_event_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Any | ExternalEventDetail | None:
    """Fetch a single external event including its raw payload

    Args:
        external_event_id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000e1.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | ExternalEventDetail
    """

    return sync_detailed(
        external_event_id=external_event_id,
        client=client,
    ).parsed


async def asyncio_detailed(
    external_event_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Response[Any | ExternalEventDetail]:
    """Fetch a single external event including its raw payload

    Args:
        external_event_id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000e1.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | ExternalEventDetail]
    """

    kwargs = _get_kwargs(
        external_event_id=external_event_id,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    external_event_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Any | ExternalEventDetail | None:
    """Fetch a single external event including its raw payload

    Args:
        external_event_id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000e1.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | ExternalEventDetail
    """

    return (
        await asyncio_detailed(
            external_event_id=external_event_id,
            client=client,
        )
    ).parsed
