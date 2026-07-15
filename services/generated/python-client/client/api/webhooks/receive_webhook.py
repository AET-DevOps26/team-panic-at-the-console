from http import HTTPStatus
from typing import Any
from urllib.parse import quote

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.error_response import ErrorResponse
from ...models.receive_webhook_body import ReceiveWebhookBody
from ...models.webhook_receipt import WebhookReceipt
from ...types import UNSET, Response, Unset


def _get_kwargs(
    source: str,
    *,
    body: ReceiveWebhookBody,
    x_hub_signature_256: str | Unset = UNSET,
    x_git_hub_event: str | Unset = UNSET,
    x_git_hub_delivery: str | Unset = UNSET,
    x_event_type: str | Unset = UNSET,
    x_delivery_id: str | Unset = UNSET,
) -> dict[str, Any]:
    headers: dict[str, Any] = {}
    if not isinstance(x_hub_signature_256, Unset):
        headers["X-Hub-Signature-256"] = x_hub_signature_256

    if not isinstance(x_git_hub_event, Unset):
        headers["X-GitHub-Event"] = x_git_hub_event

    if not isinstance(x_git_hub_delivery, Unset):
        headers["X-GitHub-Delivery"] = x_git_hub_delivery

    if not isinstance(x_event_type, Unset):
        headers["X-Event-Type"] = x_event_type

    if not isinstance(x_delivery_id, Unset):
        headers["X-Delivery-Id"] = x_delivery_id

    _kwargs: dict[str, Any] = {
        "method": "post",
        "url": "/webhooks/{source}".format(
            source=quote(str(source), safe=""),
        ),
    }

    _kwargs["json"] = body.to_dict()

    headers["Content-Type"] = "application/json"

    _kwargs["headers"] = headers
    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> ErrorResponse | WebhookReceipt | None:
    if response.status_code == 202:
        response_202 = WebhookReceipt.from_dict(response.json())

        return response_202

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
) -> Response[ErrorResponse | WebhookReceipt]:
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
    body: ReceiveWebhookBody,
    x_hub_signature_256: str | Unset = UNSET,
    x_git_hub_event: str | Unset = UNSET,
    x_git_hub_delivery: str | Unset = UNSET,
    x_event_type: str | Unset = UNSET,
    x_delivery_id: str | Unset = UNSET,
) -> Response[ErrorResponse | WebhookReceipt]:
    """Ingest a webhook from an external system

     Persists the payload verbatim as an External Event (ADR 0008) and publishes
    `external.event.received` to NATS for the rule engine. Sources with a configured secret must sign
    the raw request body (`X-Hub-Signature-256`, GitHub convention); with
    `WEBHOOK_REQUIRE_SIGNATURE=true` sources without a secret are rejected. Redeliveries carrying an
    already-seen delivery id are acknowledged with the original event id (`duplicate: true`) and not re-
    published.

    Args:
        source (str):  Example: github.
        x_hub_signature_256 (str | Unset):
        x_git_hub_event (str | Unset):
        x_git_hub_delivery (str | Unset):
        x_event_type (str | Unset):
        x_delivery_id (str | Unset):
        body (ReceiveWebhookBody): Raw webhook payload; persisted verbatim.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[ErrorResponse | WebhookReceipt]
    """

    kwargs = _get_kwargs(
        source=source,
        body=body,
        x_hub_signature_256=x_hub_signature_256,
        x_git_hub_event=x_git_hub_event,
        x_git_hub_delivery=x_git_hub_delivery,
        x_event_type=x_event_type,
        x_delivery_id=x_delivery_id,
    )

    response = client.get_httpx_client().request(
        **kwargs,
    )

    return _build_response(client=client, response=response)


def sync(
    source: str,
    *,
    client: AuthenticatedClient | Client,
    body: ReceiveWebhookBody,
    x_hub_signature_256: str | Unset = UNSET,
    x_git_hub_event: str | Unset = UNSET,
    x_git_hub_delivery: str | Unset = UNSET,
    x_event_type: str | Unset = UNSET,
    x_delivery_id: str | Unset = UNSET,
) -> ErrorResponse | WebhookReceipt | None:
    """Ingest a webhook from an external system

     Persists the payload verbatim as an External Event (ADR 0008) and publishes
    `external.event.received` to NATS for the rule engine. Sources with a configured secret must sign
    the raw request body (`X-Hub-Signature-256`, GitHub convention); with
    `WEBHOOK_REQUIRE_SIGNATURE=true` sources without a secret are rejected. Redeliveries carrying an
    already-seen delivery id are acknowledged with the original event id (`duplicate: true`) and not re-
    published.

    Args:
        source (str):  Example: github.
        x_hub_signature_256 (str | Unset):
        x_git_hub_event (str | Unset):
        x_git_hub_delivery (str | Unset):
        x_event_type (str | Unset):
        x_delivery_id (str | Unset):
        body (ReceiveWebhookBody): Raw webhook payload; persisted verbatim.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        ErrorResponse | WebhookReceipt
    """

    return sync_detailed(
        source=source,
        client=client,
        body=body,
        x_hub_signature_256=x_hub_signature_256,
        x_git_hub_event=x_git_hub_event,
        x_git_hub_delivery=x_git_hub_delivery,
        x_event_type=x_event_type,
        x_delivery_id=x_delivery_id,
    ).parsed


async def asyncio_detailed(
    source: str,
    *,
    client: AuthenticatedClient | Client,
    body: ReceiveWebhookBody,
    x_hub_signature_256: str | Unset = UNSET,
    x_git_hub_event: str | Unset = UNSET,
    x_git_hub_delivery: str | Unset = UNSET,
    x_event_type: str | Unset = UNSET,
    x_delivery_id: str | Unset = UNSET,
) -> Response[ErrorResponse | WebhookReceipt]:
    """Ingest a webhook from an external system

     Persists the payload verbatim as an External Event (ADR 0008) and publishes
    `external.event.received` to NATS for the rule engine. Sources with a configured secret must sign
    the raw request body (`X-Hub-Signature-256`, GitHub convention); with
    `WEBHOOK_REQUIRE_SIGNATURE=true` sources without a secret are rejected. Redeliveries carrying an
    already-seen delivery id are acknowledged with the original event id (`duplicate: true`) and not re-
    published.

    Args:
        source (str):  Example: github.
        x_hub_signature_256 (str | Unset):
        x_git_hub_event (str | Unset):
        x_git_hub_delivery (str | Unset):
        x_event_type (str | Unset):
        x_delivery_id (str | Unset):
        body (ReceiveWebhookBody): Raw webhook payload; persisted verbatim.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[ErrorResponse | WebhookReceipt]
    """

    kwargs = _get_kwargs(
        source=source,
        body=body,
        x_hub_signature_256=x_hub_signature_256,
        x_git_hub_event=x_git_hub_event,
        x_git_hub_delivery=x_git_hub_delivery,
        x_event_type=x_event_type,
        x_delivery_id=x_delivery_id,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    source: str,
    *,
    client: AuthenticatedClient | Client,
    body: ReceiveWebhookBody,
    x_hub_signature_256: str | Unset = UNSET,
    x_git_hub_event: str | Unset = UNSET,
    x_git_hub_delivery: str | Unset = UNSET,
    x_event_type: str | Unset = UNSET,
    x_delivery_id: str | Unset = UNSET,
) -> ErrorResponse | WebhookReceipt | None:
    """Ingest a webhook from an external system

     Persists the payload verbatim as an External Event (ADR 0008) and publishes
    `external.event.received` to NATS for the rule engine. Sources with a configured secret must sign
    the raw request body (`X-Hub-Signature-256`, GitHub convention); with
    `WEBHOOK_REQUIRE_SIGNATURE=true` sources without a secret are rejected. Redeliveries carrying an
    already-seen delivery id are acknowledged with the original event id (`duplicate: true`) and not re-
    published.

    Args:
        source (str):  Example: github.
        x_hub_signature_256 (str | Unset):
        x_git_hub_event (str | Unset):
        x_git_hub_delivery (str | Unset):
        x_event_type (str | Unset):
        x_delivery_id (str | Unset):
        body (ReceiveWebhookBody): Raw webhook payload; persisted verbatim.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        ErrorResponse | WebhookReceipt
    """

    return (
        await asyncio_detailed(
            source=source,
            client=client,
            body=body,
            x_hub_signature_256=x_hub_signature_256,
            x_git_hub_event=x_git_hub_event,
            x_git_hub_delivery=x_git_hub_delivery,
            x_event_type=x_event_type,
            x_delivery_id=x_delivery_id,
        )
    ).parsed
