# Comments are immutable: no edit or delete

Once a comment is posted to an Incident, it cannot be edited or deleted. The Analysis Object Model originally included `Comment.edit()` and `Comment.delete()` methods; these were removed.

Incident comments are part of the audit trail. Allowing edits would create ambiguity about what was actually said during an incident and undermine the reliability of the timeline for post-incident review. Immutability is also consistent with the append-only Event Log: a comment is an event, and events are not mutated. This simplifies `incident-service` (no edit/delete endpoints, no edit history to track) and `event-service` (no `comment.edited` event type needed).
