from http import HTTPStatus
from typing import Any, cast

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.create_webhook_source_request import CreateWebhookSourceRequest
from ...models.error_response import ErrorResponse
from ...models.webhook_source_with_secret import WebhookSourceWithSecret
from ...types import Response


def _get_kwargs(
    *,
    body: CreateWebhookSourceRequest,
) -> dict[str, Any]:
    headers: dict[str, Any] = {}

    _kwargs: dict[str, Any] = {
        "method": "post",
        "url": "/webhook-sources",
    }

    _kwargs["json"] = body.to_dict()

    headers["Content-Type"] = "application/json"

    _kwargs["headers"] = headers
    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Any | ErrorResponse | WebhookSourceWithSecret | None:
    if response.status_code == 201:
        response_201 = WebhookSourceWithSecret.from_dict(response.json())

        return response_201

    if response.status_code == 400:
        response_400 = ErrorResponse.from_dict(response.json())

        return response_400

    if response.status_code == 401:
        response_401 = cast(Any, None)
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
) -> Response[Any | ErrorResponse | WebhookSourceWithSecret]:
    return Response(
        status_code=HTTPStatus(response.status_code),
        content=response.content,
        headers=response.headers,
        parsed=_parse_response(client=client, response=response),
    )


def sync_detailed(
    *,
    client: AuthenticatedClient | Client,
    body: CreateWebhookSourceRequest,
) -> Response[Any | ErrorResponse | WebhookSourceWithSecret]:
    """Register a webhook source and generate its HMAC secret

     The generated secret is returned only in this response; configure it on the sending system (e.g. as
    the GitHub webhook secret). Deliveries for a registered source must be signed with it (`X-Hub-
    Signature-256`).

    Args:
        body (CreateWebhookSourceRequest):

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | ErrorResponse | WebhookSourceWithSecret]
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
    body: CreateWebhookSourceRequest,
) -> Any | ErrorResponse | WebhookSourceWithSecret | None:
    """Register a webhook source and generate its HMAC secret

     The generated secret is returned only in this response; configure it on the sending system (e.g. as
    the GitHub webhook secret). Deliveries for a registered source must be signed with it (`X-Hub-
    Signature-256`).

    Args:
        body (CreateWebhookSourceRequest):

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | ErrorResponse | WebhookSourceWithSecret
    """

    return sync_detailed(
        client=client,
        body=body,
    ).parsed


async def asyncio_detailed(
    *,
    client: AuthenticatedClient | Client,
    body: CreateWebhookSourceRequest,
) -> Response[Any | ErrorResponse | WebhookSourceWithSecret]:
    """Register a webhook source and generate its HMAC secret

     The generated secret is returned only in this response; configure it on the sending system (e.g. as
    the GitHub webhook secret). Deliveries for a registered source must be signed with it (`X-Hub-
    Signature-256`).

    Args:
        body (CreateWebhookSourceRequest):

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | ErrorResponse | WebhookSourceWithSecret]
    """

    kwargs = _get_kwargs(
        body=body,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    *,
    client: AuthenticatedClient | Client,
    body: CreateWebhookSourceRequest,
) -> Any | ErrorResponse | WebhookSourceWithSecret | None:
    """Register a webhook source and generate its HMAC secret

     The generated secret is returned only in this response; configure it on the sending system (e.g. as
    the GitHub webhook secret). Deliveries for a registered source must be signed with it (`X-Hub-
    Signature-256`).

    Args:
        body (CreateWebhookSourceRequest):

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | ErrorResponse | WebhookSourceWithSecret
    """

    return (
        await asyncio_detailed(
            client=client,
            body=body,
        )
    ).parsed
