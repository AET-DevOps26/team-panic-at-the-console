"""Smoke tests for /metrics + custom histograms.

We avoid asserting exact values across tests (the prometheus_client default
registry is process-global; observations accumulate). Instead we read a metric
sample before and after an action and assert the delta.
"""

from __future__ import annotations

import pytest

from genai_service.metrics import (
    ai_generation_seconds,
    ai_generations_total,
    time_generation,
)


def _counter_value(task: str, outcome: str) -> float:
    return ai_generations_total.labels(type=task, outcome=outcome)._value.get()


def _histogram_count(task: str) -> float:
    return ai_generation_seconds.labels(type=task)._sum.get()


def test_metrics_endpoint_returns_prometheus_format(client):
    resp = client.get("/metrics")
    assert resp.status_code == 200
    assert "text/plain" in resp.headers["content-type"]
    body = resp.text
    assert "http_requests_total" in body
    assert "ai_generation_seconds" in body
    assert "ai_generations_total" in body


def test_metrics_endpoint_records_http_requests(client):
    for _ in range(3):
        assert client.get("/health").status_code == 200

    body = client.get("/metrics").text
    matching = [
        line
        for line in body.splitlines()
        if line.startswith("http_requests_total{") and not line.endswith(" 0.0")
    ]
    assert matching, "expected at least one non-zero http_requests_total sample"


def test_time_generation_records_success():
    before_count = _counter_value("summary", "success")
    before_sum = _histogram_count("summary")

    with time_generation("summary"):
        pass

    assert _counter_value("summary", "success") == pytest.approx(before_count + 1)
    assert _histogram_count("summary") >= before_sum


def test_time_generation_records_error_outcome_and_reraises():
    before = _counter_value("postmortem", "error")

    with pytest.raises(RuntimeError, match="boom"):
        with time_generation("postmortem"):
            raise RuntimeError("boom")

    assert _counter_value("postmortem", "error") == pytest.approx(before + 1)


def test_metrics_excluded_from_openapi(client):
    """/metrics is observability-only and should not pollute the OpenAPI surface."""
    schema = client.get("/openapi.json").json()
    assert "/metrics" not in schema["paths"]
