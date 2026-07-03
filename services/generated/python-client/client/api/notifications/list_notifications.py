from http import HTTPStatus
from typing import Any
from uuid import UUID

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.notification_list_response import NotificationListResponse
from ...types import UNSET, Response, Unset


def _get_kwargs(
    *,
    recipient_id: UUID | Unset = UNSET,
    unread_only: bool | Unset = False,
    page: int | Unset = 0,
    size: int | Unset = 50,
) -> dict[str, Any]:
    params: dict[str, Any] = {}

    json_recipient_id: str | Unset = UNSET
    if not isinstance(recipient_id, Unset):
        json_recipient_id = str(recipient_id)
    params["recipientId"] = json_recipient_id

    params["unreadOnly"] = unread_only

    params["page"] = page

    params["size"] = size

    params = {k: v for k, v in params.items() if v is not UNSET and v is not None}

    _kwargs: dict[str, Any] = {
        "method": "get",
        "url": "/notifications",
        "params": params,
    }

    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> NotificationListResponse | None:
    if response.status_code == 200:
        response_200 = NotificationListResponse.from_dict(response.json())

        return response_200

    if client.raise_on_unexpected_status:
        raise errors.UnexpectedStatus(response.status_code, response.content)
    else:
        return None


def _build_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Response[NotificationListResponse]:
    return Response(
        status_code=HTTPStatus(response.status_code),
        content=response.content,
        headers=response.headers,
        parsed=_parse_response(client=client, response=response),
    )


def sync_detailed(
    *,
    client: AuthenticatedClient | Client,
    recipient_id: UUID | Unset = UNSET,
    unread_only: bool | Unset = False,
    page: int | Unset = 0,
    size: int | Unset = 50,
) -> Response[NotificationListResponse]:
    """List notifications, newest first

    Args:
        recipient_id (UUID | Unset):
        unread_only (bool | Unset):  Default: False.
        page (int | Unset):  Default: 0.
        size (int | Unset):  Default: 50.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[NotificationListResponse]
    """

    kwargs = _get_kwargs(
        recipient_id=recipient_id,
        unread_only=unread_only,
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
    recipient_id: UUID | Unset = UNSET,
    unread_only: bool | Unset = False,
    page: int | Unset = 0,
    size: int | Unset = 50,
) -> NotificationListResponse | None:
    """List notifications, newest first

    Args:
        recipient_id (UUID | Unset):
        unread_only (bool | Unset):  Default: False.
        page (int | Unset):  Default: 0.
        size (int | Unset):  Default: 50.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        NotificationListResponse
    """

    return sync_detailed(
        client=client,
        recipient_id=recipient_id,
        unread_only=unread_only,
        page=page,
        size=size,
    ).parsed


async def asyncio_detailed(
    *,
    client: AuthenticatedClient | Client,
    recipient_id: UUID | Unset = UNSET,
    unread_only: bool | Unset = False,
    page: int | Unset = 0,
    size: int | Unset = 50,
) -> Response[NotificationListResponse]:
    """List notifications, newest first

    Args:
        recipient_id (UUID | Unset):
        unread_only (bool | Unset):  Default: False.
        page (int | Unset):  Default: 0.
        size (int | Unset):  Default: 50.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[NotificationListResponse]
    """

    kwargs = _get_kwargs(
        recipient_id=recipient_id,
        unread_only=unread_only,
        page=page,
        size=size,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    *,
    client: AuthenticatedClient | Client,
    recipient_id: UUID | Unset = UNSET,
    unread_only: bool | Unset = False,
    page: int | Unset = 0,
    size: int | Unset = 50,
) -> NotificationListResponse | None:
    """List notifications, newest first

    Args:
        recipient_id (UUID | Unset):
        unread_only (bool | Unset):  Default: False.
        page (int | Unset):  Default: 0.
        size (int | Unset):  Default: 50.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        NotificationListResponse
    """

    return (
        await asyncio_detailed(
            client=client,
            recipient_id=recipient_id,
            unread_only=unread_only,
            page=page,
            size=size,
        )
    ).parsed
