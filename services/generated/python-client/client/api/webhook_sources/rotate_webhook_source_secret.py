from http import HTTPStatus
from typing import Any, cast
from urllib.parse import quote

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.webhook_source_with_secret import WebhookSourceWithSecret
from ...types import Response


def _get_kwargs(
    source: str,
) -> dict[str, Any]:
    _kwargs: dict[str, Any] = {
        "method": "post",
        "url": "/webhook-sources/{source}/rotate-secret".format(
            source=quote(str(source), safe=""),
        ),
    }

    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Any | WebhookSourceWithSecret | None:
    if response.status_code == 200:
        response_200 = WebhookSourceWithSecret.from_dict(response.json())

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
) -> Response[Any | WebhookSourceWithSecret]:
    return Response(
        status_code=HTTPStatus(response.status_code),
        content=response.content,
        headers=response.headers,
        parsed=_parse_response(client=client, response=response),
    )


def sync_detailed(
    source: str,
    *,
    client: AuthenticatedClient | Client,
) -> Response[Any | WebhookSourceWithSecret]:
    """Replace the source's secret with a newly generated one

     The old secret stops working immediately; update the sending system with the returned secret, which
    is shown only in this response.

    Args:
        source (str):  Example: github.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | WebhookSourceWithSecret]
    """

    kwargs = _get_kwargs(
        source=source,
    )

    response = client.get_httpx_client().request(
        **kwargs,
    )

    return _build_response(client=client, response=response)


def sync(
    source: str,
    *,
    client: AuthenticatedClient | Client,
) -> Any | WebhookSourceWithSecret | None:
    """Replace the source's secret with a newly generated one

     The old secret stops working immediately; update the sending system with the returned secret, which
    is shown only in this response.

    Args:
        source (str):  Example: github.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | WebhookSourceWithSecret
    """

    return sync_detailed(
        source=source,
        client=client,
    ).parsed


async def asyncio_detailed(
    source: str,
    *,
    client: AuthenticatedClient | Client,
) -> Response[Any | WebhookSourceWithSecret]:
    """Replace the source's secret with a newly generated one

     The old secret stops working immediately; update the sending system with the returned secret, which
    is shown only in this response.

    Args:
        source (str):  Example: github.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | WebhookSourceWithSecret]
    """

    kwargs = _get_kwargs(
        source=source,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    source: str,
    *,
    client: AuthenticatedClient | Client,
) -> Any | WebhookSourceWithSecret | None:
    """Replace the source's secret with a newly generated one

     The old secret stops working immediately; update the sending system with the returned secret, which
    is shown only in this response.

    Args:
        source (str):  Example: github.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | WebhookSourceWithSecret
    """

    return (
        await asyncio_detailed(
            source=source,
            client=client,
        )
    ).parsed
