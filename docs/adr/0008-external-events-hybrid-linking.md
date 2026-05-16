# External events create incidents and also post context comments

External events are always evaluated by the rule engine. Today, rules only support auto-creating incidents, so follow-up webhooks (e.g., "deployment succeeded") would either fail to match or create duplicates. We will use a **hybrid** approach:

- **Rules still auto-create incidents** when they match an external event.
- **Every external event is persisted by webhook-service** for auditability.
- **Webhook-service also posts a comment** onto the **most relevant existing incident** when a clear link can be established (e.g., same `sourceId`, correlation key, or explicit incident reference). The comment includes a short summary and a link to the raw external event payload.
- **If no link is established**, the event is treated as standalone and only rules can create a new incident.

This keeps the existing rule engine simple while still preserving contextual follow-up events on incidents. If automated linking proves unreliable, operators can continue to link manually, and a future RAG-based correlation step can be added without changing the external event contract.

Alternatives considered:

1. **Extend rules to link/update existing incidents**: more powerful but adds complexity to rule evaluation and incident-service update semantics.
2. **Manual linking only**: simplest but loses automatic context and creates blind spots in timelines.
3. **Out of scope**: not acceptable because external event follow-ups are common and would produce duplicates or no context.
