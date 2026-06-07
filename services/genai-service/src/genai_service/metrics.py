"""Prometheus metrics for genai-service.

The /metrics endpoint is auto-mounted by `prometheus-fastapi-instrumentator`
(see main.py). HTTP request metrics (`http_requests_total`,
`http_request_duration_seconds`) come for free from the instrumentator.

What we add on top is application-level signal that mocked tests cannot see:
how long each AI generation takes per `PromptTask`, and how often they
succeed vs error. CONTEXT.md custom metrics: `ai_generation_seconds`
(histogram, label `type`).
"""

from __future__ import annotations

import time
from collections.abc import Iterator
from contextlib import contextmanager

from prometheus_client import Counter, Histogram

# Buckets chosen for CPU-bound inference (ADR-0003): typical calls land
# at 5-30s, slow ones at 60-90s. Finer buckets in the common range, broader
# above so the histogram stays cheap.
_GENERATION_BUCKETS = (0.5, 1, 2, 5, 10, 20, 30, 45, 60, 90, 120)


ai_generation_seconds = Histogram(
    "ai_generation_seconds",
    "Wall time of one LLM generation, by PromptTask.",
    labelnames=("type",),
    buckets=_GENERATION_BUCKETS,
)


ai_generations_total = Counter(
    "ai_generations_total",
    "AI generations completed, by PromptTask and outcome.",
    labelnames=("type", "outcome"),
)


@contextmanager
def time_generation(task: str) -> Iterator[None]:
    """Record duration + outcome of one generation.

    Usage:
        with time_generation("summary"):
            await llm.generate(...)
    """
    start = time.perf_counter()
    outcome = "success"
    try:
        yield
    except BaseException:
        outcome = "error"
        raise
    finally:
        elapsed = time.perf_counter() - start
        ai_generation_seconds.labels(type=task).observe(elapsed)
        ai_generations_total.labels(type=task, outcome=outcome).inc()
