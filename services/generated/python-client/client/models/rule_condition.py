from __future__ import annotations

from collections.abc import Mapping
from typing import Any, TypeVar

from attrs import define as _attrs_define

from ..models.rule_operator import RuleOperator
from ..types import UNSET, Unset

T = TypeVar("T", bound="RuleCondition")


@_attrs_define
class RuleCondition:
    """A single match condition. The field is a dotted path into the event, rooted at an object exposing `source`,
    `eventType` and `payload` (the raw webhook body), e.g. `payload.workflow_run.conclusion`.

        Attributes:
            field (str): Dotted path into the event, e.g. `payload.workflow_run.conclusion`. Example:
                payload.workflow_run.conclusion.
            operator (RuleOperator): How a condition compares the value at its field path against the condition value.
                `exists`/`not_exists` ignore the value; every other operator compares the field's scalar value as a string.
            value (str | Unset): Comparison value. For `in` this is a comma-separated list. Ignored by
                `exists`/`not_exists`. Example: failure.
    """

    field: str
    operator: RuleOperator
    value: str | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        field = self.field

        operator = self.operator.value

        value = self.value

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "field": field,
                "operator": operator,
            }
        )
        if value is not UNSET:
            field_dict["value"] = value

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        field = d.pop("field")

        operator = RuleOperator(d.pop("operator"))

        value = d.pop("value", UNSET)

        rule_condition = cls(
            field=field,
            operator=operator,
            value=value,
        )

        return rule_condition
