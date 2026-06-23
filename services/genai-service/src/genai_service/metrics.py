"""Prometheus metrics for genai-service.

The /metrics endpoint is auto-mounted by `prometheus-fastapi-instrumentator`
(see main.py). HTTP request metrics (`http_requests_total`,
`http_request_duration_seconds`) come for free from the instrumentator.

Application-level metrics cover LLM generation (per task and provider), Logos→Ollama
fallback, and NATS consumer health.
"""

from __future__ import annotations

import time
from collections.abc import Callable, Iterator
from contextlib import contextmanager

from prometheus_client import Counter, Gauge, Histogram

ProviderLabel = str | Callable[[], str]

# Buckets chosen for CPU-bound inference (ADR-0003): typical calls land
# at 5-30s, slow ones at 60-90s. Finer buckets in the common range, broader
# above so the histogram stays cheap.
_GENERATION_BUCKETS = (0.5, 1, 2, 5, 10, 20, 30, 45, 60, 90, 120)


ai_generation_seconds = Histogram(
    "ai_generation_seconds",
    "Wall time of one AI task (LLM call + incident-service PATCH), by PromptTask and provider.",
    labelnames=("type", "provider"),
    buckets=_GENERATION_BUCKETS,
)


ai_generations_total = Counter(
    "ai_generations_total",
    "AI tasks completed (LLM + PATCH), by PromptTask, provider, and outcome.",
    labelnames=("type", "provider", "outcome"),
)


llm_fallback_total = Counter(
    "llm_fallback_total",
    "LLM calls that fell back from the primary provider to the backup.",
    labelnames=("from_provider", "to_provider"),
)


nats_messages_total = Counter(
    "nats_messages_total",
    "NATS messages received by the genai-service consumer.",
    labelnames=("subject", "outcome"),
)


nats_consumer_connected = Gauge(
    "nats_consumer_connected",
    "1 when subscribed to incident.* subjects, 0 otherwise.",
)


def record_generation(
    task: str, provider: str, outcome: str, elapsed_seconds: float
) -> None:
    ai_generation_seconds.labels(type=task, provider=provider).observe(elapsed_seconds)
    ai_generations_total.labels(type=task, provider=provider, outcome=outcome).inc()


def set_nats_consumer_connected(connected: bool) -> None:
    nats_consumer_connected.set(1 if connected else 0)


def record_nats_message(subject: str, outcome: str) -> None:
    nats_messages_total.labels(subject=subject, outcome=outcome).inc()


def init_prometheus_labelsets() -> None:
    """Register label combinations at 0 so Grafana shows healthy state before first use."""
    llm_fallback_total.labels(from_provider="logos", to_provider="ollama").inc(0)


def _resolve_provider(provider: ProviderLabel) -> str:
    return provider() if callable(provider) else provider


@contextmanager
def time_generation(task: str, provider: ProviderLabel) -> Iterator[None]:
    """Record duration + outcome of one end-to-end AI task (LLM + PATCH).

    Pass a callable for `provider` when the label is only known after the LLM
    call (e.g. `FallbackLLMClient.last_provider_used`).
    """
    start = time.perf_counter()
    outcome = "success"
    try:
        yield
    except Exception:
        outcome = "error"
        raise
    finally:
        record_generation(
            task, _resolve_provider(provider), outcome, time.perf_counter() - start
        )
