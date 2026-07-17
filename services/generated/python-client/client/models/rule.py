from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import TYPE_CHECKING, Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define
from dateutil.parser import isoparse

from ..models.severity import Severity
from ..types import UNSET, Unset

if TYPE_CHECKING:
    from ..models.rule_condition import RuleCondition
    from ..models.rule_metadata_field import RuleMetadataField


T = TypeVar("T", bound="Rule")


@_attrs_define
class Rule:
    """A configured incident rule.

    Attributes:
        id (UUID):
        name (str):
        enabled (bool):
        priority (int):
        conditions (list[RuleCondition]):
        severity (Severity): Incident severity. SEV1 is highest impact.
        title_template (str):
        metadata_fields (list[RuleMetadataField]):
        created_at (datetime.datetime):
        updated_at (datetime.datetime):
        source (str | Unset):
        description_template (str | Unset):
        dedup_key_template (str | Unset):
    """

    id: UUID
    name: str
    enabled: bool
    priority: int
    conditions: list[RuleCondition]
    severity: Severity
    title_template: str
    metadata_fields: list[RuleMetadataField]
    created_at: datetime.datetime
    updated_at: datetime.datetime
    source: str | Unset = UNSET
    description_template: str | Unset = UNSET
    dedup_key_template: str | Unset = UNSET

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        name = self.name

        enabled = self.enabled

        priority = self.priority

        conditions = []
        for conditions_item_data in self.conditions:
            conditions_item = conditions_item_data.to_dict()
            conditions.append(conditions_item)

        severity = self.severity.value

        title_template = self.title_template

        metadata_fields = []
        for metadata_fields_item_data in self.metadata_fields:
            metadata_fields_item = metadata_fields_item_data.to_dict()
            metadata_fields.append(metadata_fields_item)

        created_at = self.created_at.isoformat()

        updated_at = self.updated_at.isoformat()

        source = self.source

        description_template = self.description_template

        dedup_key_template = self.dedup_key_template

        field_dict: dict[str, Any] = {}

        field_dict.update(
            {
                "id": id,
                "name": name,
                "enabled": enabled,
                "priority": priority,
                "conditions": conditions,
                "severity": severity,
                "titleTemplate": title_template,
                "metadataFields": metadata_fields,
                "createdAt": created_at,
                "updatedAt": updated_at,
            }
        )
        if source is not UNSET:
            field_dict["source"] = source
        if description_template is not UNSET:
            field_dict["descriptionTemplate"] = description_template
        if dedup_key_template is not UNSET:
            field_dict["dedupKeyTemplate"] = dedup_key_template

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        from ..models.rule_condition import RuleCondition
        from ..models.rule_metadata_field import RuleMetadataField

        d = dict(src_dict)
        id = UUID(d.pop("id"))

        name = d.pop("name")

        enabled = d.pop("enabled")

        priority = d.pop("priority")

        conditions = []
        _conditions = d.pop("conditions")
        for conditions_item_data in _conditions:
            conditions_item = RuleCondition.from_dict(conditions_item_data)

            conditions.append(conditions_item)

        severity = Severity(d.pop("severity"))

        title_template = d.pop("titleTemplate")

        metadata_fields = []
        _metadata_fields = d.pop("metadataFields")
        for metadata_fields_item_data in _metadata_fields:
            metadata_fields_item = RuleMetadataField.from_dict(metadata_fields_item_data)

            metadata_fields.append(metadata_fields_item)

        created_at = isoparse(d.pop("createdAt"))

        updated_at = isoparse(d.pop("updatedAt"))

        source = d.pop("source", UNSET)

        description_template = d.pop("descriptionTemplate", UNSET)

        dedup_key_template = d.pop("dedupKeyTemplate", UNSET)

        rule = cls(
            id=id,
            name=name,
            enabled=enabled,
            priority=priority,
            conditions=conditions,
            severity=severity,
            title_template=title_template,
            metadata_fields=metadata_fields,
            created_at=created_at,
            updated_at=updated_at,
            source=source,
            description_template=description_template,
            dedup_key_template=dedup_key_template,
        )

        return rule
