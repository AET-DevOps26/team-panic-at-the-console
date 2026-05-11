# NATS JetStream for cross-service side effects

All side effects triggered by an Incident state change (event log writes, in-app notifications, genai generation, SSE fan-out) are delivered via NATS JetStream pub/sub rather than synchronous REST calls. `incident-service` publishes a thin event (`{incidentId, timestamp}`) and subscribers consume independently.

The alternative was synchronous REST fan-out from `incident-service` to each downstream service. We rejected this because it couples `incident-service` to the existence and availability of every subscriber, turns a write operation into a multi-hop synchronous chain, and makes adding new subscribers require modifying `incident-service`. NATS JetStream gives us persistence (events survive restarts), replay, and zero coupling at the cost of added infrastructure and eventual consistency.

REST is still used for query/response flows (frontend → gateway → service data) and for the genai PATCH-back to `incident-service`.
