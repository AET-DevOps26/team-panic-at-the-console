from http import HTTPStatus
from typing import Any

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.error_response import ErrorResponse
from ...models.update_profile_request import UpdateProfileRequest
from ...models.user import User
from ...types import Response


def _get_kwargs(
    *,
    body: UpdateProfileRequest,
) -> dict[str, Any]:
    headers: dict[str, Any] = {}

    _kwargs: dict[str, Any] = {
        "method": "patch",
        "url": "/users/me",
    }

    _kwargs["json"] = body.to_dict()

    headers["Content-Type"] = "application/json"

    _kwargs["headers"] = headers
    return _kwargs


def _parse_response(*, client: AuthenticatedClient | Client, response: httpx.Response) -> ErrorResponse | User | None:
    if response.status_code == 200:
        response_200 = User.from_dict(response.json())

        return response_200

    if response.status_code == 400:
        response_400 = ErrorResponse.from_dict(response.json())

        return response_400

    if response.status_code == 401:
        response_401 = ErrorResponse.from_dict(response.json())

        return response_401

    if response.status_code == 409:
        response_409 = ErrorResponse.from_dict(response.json())

        return response_409

    if client.raise_on_unexpected_status:
        raise errors.UnexpectedStatus(response.status_code, response.content)
    else:
        return None


def _build_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Response[ErrorResponse | User]:
    return Response(
        status_code=HTTPStatus(response.status_code),
        content=response.content,
        headers=response.headers,
        parsed=_parse_response(client=client, response=response),
    )


def sync_detailed(
    *,
    client: AuthenticatedClient | Client,
    body: UpdateProfileRequest,
) -> Response[ErrorResponse | User]:
    """Update the authenticated user profile

     Partial update of the caller's own profile. Requires a valid `session` cookie (see
    `GET /users/me`). Changing `email` additionally requires `currentPassword`; changing
    only `displayName` does not. The session cookie stays valid after the update.

    Args:
        body (UpdateProfileRequest): Partial profile update; at least one of `email` or
            `displayName` must be present. `currentPassword` is required when `email` is present.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[ErrorResponse | User]
    """

    kwargs = _get_kwargs(
        body=body,
    )

    response = client.get_httpx_client().request(
        **kwargs,
    )

    return _build_response(client=client, response=response)


def sync(
    *,
    client: AuthenticatedClient | Client,
    body: UpdateProfileRequest,
) -> ErrorResponse | User | None:
    """Update the authenticated user profile

     Partial update of the caller's own profile. Requires a valid `session` cookie (see
    `GET /users/me`). Changing `email` additionally requires `currentPassword`; changing
    only `displayName` does not. The session cookie stays valid after the update.

    Args:
        body (UpdateProfileRequest): Partial profile update; at least one of `email` or
            `displayName` must be present. `currentPassword` is required when `email` is present.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        ErrorResponse | User
    """

    return sync_detailed(
        client=client,
        body=body,
    ).parsed


async def asyncio_detailed(
    *,
    client: AuthenticatedClient | Client,
    body: UpdateProfileRequest,
) -> Response[ErrorResponse | User]:
    """Update the authenticated user profile

     Partial update of the caller's own profile. Requires a valid `session` cookie (see
    `GET /users/me`). Changing `email` additionally requires `currentPassword`; changing
    only `displayName` does not. The session cookie stays valid after the update.

    Args:
        body (UpdateProfileRequest): Partial profile update; at least one of `email` or
            `displayName` must be present. `currentPassword` is required when `email` is present.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[ErrorResponse | User]
    """

    kwargs = _get_kwargs(
        body=body,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    *,
    client: AuthenticatedClient | Client,
    body: UpdateProfileRequest,
) -> ErrorResponse | User | None:
    """Update the authenticated user profile

     Partial update of the caller's own profile. Requires a valid `session` cookie (see
    `GET /users/me`). Changing `email` additionally requires `currentPassword`; changing
    only `displayName` does not. The session cookie stays valid after the update.

    Args:
        body (UpdateProfileRequest): Partial profile update; at least one of `email` or
            `displayName` must be present. `currentPassword` is required when `email` is present.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        ErrorResponse | User
    """

    return (
        await asyncio_detailed(
            client=client,
            body=body,
        )
    ).parsed
