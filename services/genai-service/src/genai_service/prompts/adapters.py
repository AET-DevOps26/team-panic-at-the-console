from collections.abc import Iterable, Sequence
from typing import Any

from pydantic import ValidationError

from genai_service.prompts.models import Event, Incident


def _to_mapping(obj: Any) -> dict:
    """Try to convert various client model shapes to a plain dict suitable for Pydantic validation."""
    if obj is None:
        return {}
    # pydantic v2 models
    if hasattr(obj, "model_dump"):
        return obj.model_dump(by_alias=True)
    # older pydantic or dataclass-like
    # openapi-python-client / attrs generated models expose `to_dict()`
    if hasattr(obj, "to_dict"):
        try:
            return obj.to_dict()
        except TypeError:
            pass
    if hasattr(obj, "dict"):
        try:
            return obj.dict()
        except TypeError:
            pass
    # plain mapping
    if isinstance(obj, dict):
        return obj
    # fallback: use __dict__ if available
    if hasattr(obj, "__dict__"):
        return vars(obj)
    # last resort: build from attributes (public, non-callable)
    out = {}
    for name in dir(obj):
        if name.startswith("_"):
            continue
        try:
            val = getattr(obj, name)
        except Exception:
            continue
        if callable(val):
            continue
        out[name] = val
    return out


def incident_from_client(obj: Any) -> Incident:
    """Convert a generated client incident object or mapping into a prompt `Incident`.

    Accepts dicts, pydantic models, dataclasses, or simple objects.
    """
    return model_from_client(Incident, obj)


def events_from_client(seq: Iterable[Any]) -> list[Event]:
    """Convert an iterable of event-like objects into a list of prompt `Event` models."""
    return models_from_client(Event, seq)


def _snake_to_camel(s: str) -> str:
    parts = s.split("_")
    return parts[0] + "".join(p.title() for p in parts[1:])


def model_from_client(model_cls: type, obj: Any):
    """Generic: convert obj (dict, pydantic model, dataclass, plain object)
    into an instance of `model_cls`.

    Strategy:
    - Produce a plain mapping from `obj`.
    - Try `model_cls.model_validate(mapping)` (accepts alias or names depending on model config).
    - On ValidationError, retry with keys transformed to camelCase (common OpenAPI aliasing).
    - Let Pydantic handle nested conversions (lists/dicts) where possible.
    """
    data = _to_mapping(obj)
    try:
        return model_cls.model_validate(data)
    except ValidationError:
        # Try a camel-cased keymap fallback
        alt = {(_snake_to_camel(k) if "_" in k else k): v for k, v in data.items()}
        return model_cls.model_validate(alt)


def models_from_client(model_cls: type, seq: Sequence[Any]) -> list:
    """Convert a sequence of objects into a list of `model_cls` instances."""
    out = []
    for item in seq or []:
        out.append(model_from_client(model_cls, item))
    return out
