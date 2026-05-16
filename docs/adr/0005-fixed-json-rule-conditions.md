# Rule conditions use a fixed JSON field-matcher schema, not an expression language

Rules are stored as JSON objects with a `conditions` array of `{field, operator, value}` matchers (AND-ed together) and an `action` object (`{createIncident, severity}`). The rule engine evaluates these against normalized External Event fields. There is no expression language (no CEL, SpEL, or similar).

The alternative was a full expression language giving users arbitrary condition logic. We rejected this because: (1) the acceptance criteria only require matching on event fields like type and branch — arbitrary logic is not needed; (2) expression languages require sandboxing, a parser, and significantly more test surface; (3) a fixed schema is directly UI-renderable (dropdowns, not a text box). The fixed schema covers all realistic rule patterns for CI webhook sources. If more complex conditions are needed later, the schema can be extended without breaking existing rules.
