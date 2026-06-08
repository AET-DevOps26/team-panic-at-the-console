from http import HTTPStatus
from typing import Any, cast
from urllib.parse import quote
from uuid import UUID

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.comment import Comment
from ...models.create_comment_request import CreateCommentRequest
from ...types import Response


def _get_kwargs(
    incident_id: UUID,
    *,
    body: CreateCommentRequest,
) -> dict[str, Any]:
    headers: dict[str, Any] = {}

    _kwargs: dict[str, Any] = {
        "method": "post",
        "url": "/incidents/{incident_id}/comments".format(
            incident_id=quote(str(incident_id), safe=""),
        ),
    }

    _kwargs["json"] = body.to_dict()

    headers["Content-Type"] = "application/json"

    _kwargs["headers"] = headers
    return _kwargs


def _parse_response(*, client: AuthenticatedClient | Client, response: httpx.Response) -> Any | Comment | None:
    if response.status_code == 201:
        response_201 = Comment.from_dict(response.json())

        return response_201

    if response.status_code == 404:
        response_404 = cast(Any, None)
        return response_404

    if client.raise_on_unexpected_status:
        raise errors.UnexpectedStatus(response.status_code, response.content)
    else:
        return None


def _build_response(*, client: AuthenticatedClient | Client, response: httpx.Response) -> Response[Any | Comment]:
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
    body: CreateCommentRequest,
) -> Response[Any | Comment]:
    """Add a comment to an incident

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (CreateCommentRequest): Payload for adding a comment to an incident.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | Comment]
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
    body: CreateCommentRequest,
) -> Any | Comment | None:
    """Add a comment to an incident

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (CreateCommentRequest): Payload for adding a comment to an incident.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | Comment
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
    body: CreateCommentRequest,
) -> Response[Any | Comment]:
    """Add a comment to an incident

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (CreateCommentRequest): Payload for adding a comment to an incident.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | Comment]
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
    body: CreateCommentRequest,
) -> Any | Comment | None:
    """Add a comment to an incident

    Args:
        incident_id (UUID):  Example: 018e2c5f-1234-7abc-8def-000000000001.
        body (CreateCommentRequest): Payload for adding a comment to an incident.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | Comment
    """

    return (
        await asyncio_detailed(
            incident_id=incident_id,
            client=client,
            body=body,
        )
    ).parsed
