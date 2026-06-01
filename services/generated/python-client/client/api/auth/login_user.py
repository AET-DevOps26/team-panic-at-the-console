from http import HTTPStatus
from typing import Any

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.error_response import ErrorResponse
from ...models.login_request import LoginRequest
from ...models.user import User
from ...types import Response


def _get_kwargs(
    *,
    body: LoginRequest,
) -> dict[str, Any]:
    headers: dict[str, Any] = {}

    _kwargs: dict[str, Any] = {
        "method": "post",
        "url": "/auth/login",
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
    body: LoginRequest,
) -> Response[ErrorResponse | User]:
    """Authenticate and start a session

     On success, user-service sets an httpOnly `session` cookie containing a signed JWT
    (`SameSite=Strict`). The gateway validates this cookie and injects `X-User-Id` /
    `X-User-Role` on downstream requests. Do not send credentials on subsequent API calls.

    Args:
        body (LoginRequest):

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
    body: LoginRequest,
) -> ErrorResponse | User | None:
    """Authenticate and start a session

     On success, user-service sets an httpOnly `session` cookie containing a signed JWT
    (`SameSite=Strict`). The gateway validates this cookie and injects `X-User-Id` /
    `X-User-Role` on downstream requests. Do not send credentials on subsequent API calls.

    Args:
        body (LoginRequest):

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
    body: LoginRequest,
) -> Response[ErrorResponse | User]:
    """Authenticate and start a session

     On success, user-service sets an httpOnly `session` cookie containing a signed JWT
    (`SameSite=Strict`). The gateway validates this cookie and injects `X-User-Id` /
    `X-User-Role` on downstream requests. Do not send credentials on subsequent API calls.

    Args:
        body (LoginRequest):

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
    body: LoginRequest,
) -> ErrorResponse | User | None:
    """Authenticate and start a session

     On success, user-service sets an httpOnly `session` cookie containing a signed JWT
    (`SameSite=Strict`). The gateway validates this cookie and injects `X-User-Id` /
    `X-User-Role` on downstream requests. Do not send credentials on subsequent API calls.

    Args:
        body (LoginRequest):

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
