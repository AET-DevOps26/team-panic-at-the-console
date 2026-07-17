from http import HTTPStatus
from typing import Any, cast
from urllib.parse import quote
from uuid import UUID

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.error_response import ErrorResponse
from ...models.rule import Rule
from ...models.rule_input import RuleInput
from ...types import Response


def _get_kwargs(
    rule_id: UUID,
    *,
    body: RuleInput,
) -> dict[str, Any]:
    headers: dict[str, Any] = {}

    _kwargs: dict[str, Any] = {
        "method": "put",
        "url": "/rules/{rule_id}".format(
            rule_id=quote(str(rule_id), safe=""),
        ),
    }

    _kwargs["json"] = body.to_dict()

    headers["Content-Type"] = "application/json"

    _kwargs["headers"] = headers
    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Any | ErrorResponse | Rule | None:
    if response.status_code == 200:
        response_200 = Rule.from_dict(response.json())

        return response_200

    if response.status_code == 400:
        response_400 = ErrorResponse.from_dict(response.json())

        return response_400

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
) -> Response[Any | ErrorResponse | Rule]:
    return Response(
        status_code=HTTPStatus(response.status_code),
        content=response.content,
        headers=response.headers,
        parsed=_parse_response(client=client, response=response),
    )


def sync_detailed(
    rule_id: UUID,
    *,
    client: AuthenticatedClient | Client,
    body: RuleInput,
) -> Response[Any | ErrorResponse | Rule]:
    """Replace a rule's definition

    Args:
        rule_id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000f1.
        body (RuleInput): Definition used to create or replace a rule.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | ErrorResponse | Rule]
    """

    kwargs = _get_kwargs(
        rule_id=rule_id,
        body=body,
    )

    response = client.get_httpx_client().request(
        **kwargs,
    )

    return _build_response(client=client, response=response)


def sync(
    rule_id: UUID,
    *,
    client: AuthenticatedClient | Client,
    body: RuleInput,
) -> Any | ErrorResponse | Rule | None:
    """Replace a rule's definition

    Args:
        rule_id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000f1.
        body (RuleInput): Definition used to create or replace a rule.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | ErrorResponse | Rule
    """

    return sync_detailed(
        rule_id=rule_id,
        client=client,
        body=body,
    ).parsed


async def asyncio_detailed(
    rule_id: UUID,
    *,
    client: AuthenticatedClient | Client,
    body: RuleInput,
) -> Response[Any | ErrorResponse | Rule]:
    """Replace a rule's definition

    Args:
        rule_id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000f1.
        body (RuleInput): Definition used to create or replace a rule.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Any | ErrorResponse | Rule]
    """

    kwargs = _get_kwargs(
        rule_id=rule_id,
        body=body,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    rule_id: UUID,
    *,
    client: AuthenticatedClient | Client,
    body: RuleInput,
) -> Any | ErrorResponse | Rule | None:
    """Replace a rule's definition

    Args:
        rule_id (UUID):  Example: 018e2c5f-1234-7abc-8def-0000000000f1.
        body (RuleInput): Definition used to create or replace a rule.

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Any | ErrorResponse | Rule
    """

    return (
        await asyncio_detailed(
            rule_id=rule_id,
            client=client,
            body=body,
        )
    ).parsed
