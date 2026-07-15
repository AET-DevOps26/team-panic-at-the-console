from http import HTTPStatus
from typing import Any, cast

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.external_event_list_response import ExternalEventListResponse
from ...types import UNSET, Response, Unset


def _get_kwargs(
    *,
    source: str | Unset = UNSET,
    event_type: str | Unset = UNSET,
    page: int | Unset = 0,
    size: int | Unset = 50,
) -> dict[str, Any]:
    params: dict[str, Any] = {}

    params["source"] = source

    params["eventType"] = event_type

    params["page"] = page

    params["size"] = size

    params = {k: v for k, v in params.items() if v is not UNSET and v is not None}

    _kwargs: dict[str, Any] = {
        "method": "get",
        "url": "/external-events",
        "params": params,
    }

    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Any | ExternalEventListResponse | None:
    if response.status_code == 200:
        response_200 = ExternalEventListResponse.from_dict(response.json())

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
) -> Response[Any | ExternalEventListResponse]:
    return Response(
        status_code=HTTPStatus(response.status_code),
        content=response.content,
        headers=response.headers,
        parsed=_parse_response(client=client, response=response),
    )


def sync_detailed(
    *,
    client: AuthenticatedClient | Client,
    source: str | Unset = UNSET,
    event_type: str | Unset = UNSET,
    page: int | Unset = 0,
    size: int | Unset = 50,
) -> Response[Any | ExternalEventListResponse]:
    """List persisted external events, newest first

    Args:
        source (str | Unset):
        event_type (str | Unset):
        page (int | Unset):  Default: 0.
        size (int | Unset):  Default: 50.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | ExternalEventListResponse]
    """

    kwargs = _get_kwargs(
        source=source,
        event_type=event_type,
        page=page,
        size=size,
    )

    response = client.get_httpx_client().request(
        **kwargs,
    )

    return _build_response(client=client, response=response)


def sync(
    *,
    client: AuthenticatedClient | Client,
    source: str | Unset = UNSET,
    event_type: str | Unset = UNSET,
    page: int | Unset = 0,
    size: int | Unset = 50,
) -> Any | ExternalEventListResponse | None:
    """List persisted external events, newest first

    Args:
        source (str | Unset):
        event_type (str | Unset):
        page (int | Unset):  Default: 0.
        size (int | Unset):  Default: 50.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | ExternalEventListResponse
    """

    return sync_detailed(
        client=client,
        source=source,
        event_type=event_type,
        page=page,
        size=size,
    ).parsed


async def asyncio_detailed(
    *,
    client: AuthenticatedClient | Client,
    source: str | Unset = UNSET,
    event_type: str | Unset = UNSET,
    page: int | Unset = 0,
    size: int | Unset = 50,
) -> Response[Any | ExternalEventListResponse]:
    """List persisted external events, newest first

    Args:
        source (str | Unset):
        event_type (str | Unset):
        page (int | Unset):  Default: 0.
        size (int | Unset):  Default: 50.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | ExternalEventListResponse]
    """

    kwargs = _get_kwargs(
        source=source,
        event_type=event_type,
        page=page,
        size=size,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    *,
    client: AuthenticatedClient | Client,
    source: str | Unset = UNSET,
    event_type: str | Unset = UNSET,
    page: int | Unset = 0,
    size: int | Unset = 50,
) -> Any | ExternalEventListResponse | None:
    """List persisted external events, newest first

    Args:
        source (str | Unset):
        event_type (str | Unset):
        page (int | Unset):  Default: 0.
        size (int | Unset):  Default: 50.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | ExternalEventListResponse
    """

    return (
        await asyncio_detailed(
            client=client,
            source=source,
            event_type=event_type,
            page=page,
            size=size,
        )
    ).parsed
