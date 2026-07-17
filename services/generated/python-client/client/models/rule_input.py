from __future__ import annotations

from collections.abc import Mapping
from typing import TYPE_CHECKING, Any, TypeVar

from attrs import define as _attrs_define

from ..models.severity import Severity
from ..types import UNSET, Unset

if TYPE_CHECKING:
    from ..models.rule_condition import RuleCondition
    from ..models.rule_metadata_field import RuleMetadataField


T = TypeVar("T", bound="RuleInput")


@_attrs_define
class RuleInput:
    """Definition used to create or replace a rule.

    Attributes:
        name (str): Human-readable rule name. Example: GitHub CI failures.
        severity (Severity): Incident severity. SEV1 is highest impact.
        title_template (str): Incident title; supports `{{dotted.path}}` placeholders. Example: CI failure:
            {{payload.workflow_run.name}}.
        enabled (bool | Unset):  Default: True.
        priority (int | Unset): Lower runs first; the first matching enabled rule wins. Default: 100. Example: 100.
        source (str | Unset): Only evaluate events from this source slug; omit to match any source. Example: github.
        conditions (list[RuleCondition] | Unset): All conditions must match (logical AND). An empty list matches every
            event.
        description_template (str | Unset): Optional leading description text; supports `{{dotted.path}}` placeholders
            (Markdown).
        metadata_fields (list[RuleMetadataField] | Unset): Fields rendered as a labelled Markdown list appended to the
            description.
        dedup_key_template (str | Unset): Placeholder template computing a dedup key; at most one incident is created
            per (rule, key). Empty falls back to the event id, e.g. `{{payload.workflow_run.id}}`. Example:
            {{payload.workflow_run.id}}.
    """

    name: str
    severity: Severity
    title_template: str
    enabled: bool | Unset = True
    priority: int | Unset = 100
    source: str | Unset = UNSET
    conditions: list[RuleCondition] | Unset = UNSET
    description_template: str | Unset = UNSET
    metadata_fields: list[RuleMetadataField] | Unset = UNSET
    dedup_key_template: str | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        name = self.name

        severity = self.severity.value

        title_template = self.title_template

        enabled = self.enabled

        priority = self.priority

        source = self.source

        conditions: list[dict[str, Any]] | Unset = UNSET
        if not isinstance(self.conditions, Unset):
            conditions = []
            for conditions_item_data in self.conditions:
                conditions_item = conditions_item_data.to_dict()
                conditions.append(conditions_item)

        description_template = self.description_template

        metadata_fields: list[dict[str, Any]] | Unset = UNSET
        if not isinstance(self.metadata_fields, Unset):
            metadata_fields = []
            for metadata_fields_item_data in self.metadata_fields:
                metadata_fields_item = metadata_fields_item_data.to_dict()
                metadata_fields.append(metadata_fields_item)

        dedup_key_template = self.dedup_key_template

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "name": name,
                "severity": severity,
                "titleTemplate": title_template,
            }
        )
        if enabled is not UNSET:
            field_dict["enabled"] = enabled
        if priority is not UNSET:
            field_dict["priority"] = priority
        if source is not UNSET:
            field_dict["source"] = source
        if conditions is not UNSET:
            field_dict["conditions"] = conditions
        if description_template is not UNSET:
            field_dict["descriptionTemplate"] = description_template
        if metadata_fields is not UNSET:
            field_dict["metadataFields"] = metadata_fields
        if dedup_key_template is not UNSET:
            field_dict["dedupKeyTemplate"] = dedup_key_template

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        from ..models.rule_condition import RuleCondition
        from ..models.rule_metadata_field import RuleMetadataField

        d = dict(src_dict)
        name = d.pop("name")

        severity = Severity(d.pop("severity"))

        title_template = d.pop("titleTemplate")

        enabled = d.pop("enabled", UNSET)

        priority = d.pop("priority", UNSET)

        source = d.pop("source", UNSET)

        _conditions = d.pop("conditions", UNSET)
        conditions: list[RuleCondition] | Unset = UNSET
        if _conditions is not UNSET:
            conditions = []
            for conditions_item_data in _conditions:
                conditions_item = RuleCondition.from_dict(conditions_item_data)

                conditions.append(conditions_item)

        description_template = d.pop("descriptionTemplate", UNSET)

        _metadata_fields = d.pop("metadataFields", UNSET)
        metadata_fields: list[RuleMetadataField] | Unset = UNSET
        if _metadata_fields is not UNSET:
            metadata_fields = []
            for metadata_fields_item_data in _metadata_fields:
                metadata_fields_item = RuleMetadataField.from_dict(metadata_fields_item_data)

                metadata_fields.append(metadata_fields_item)

        dedup_key_template = d.pop("dedupKeyTemplate", UNSET)

        rule_input = cls(
            name=name,
            severity=severity,
            title_template=title_template,
            enabled=enabled,
            priority=priority,
            source=source,
            conditions=conditions,
            description_template=description_template,
            metadata_fields=metadata_fields,
            dedup_key_template=dedup_key_template,
        )

        return rule_input
