# genai-service is stateless: owns no database

`genai-service` does not have its own database. It subscribes to NATS events, fetches incident data from `incident-service` via REST, calls Ollama, and PATCHes the results (Summary, Solutions, Postmortem) back to `incident-service`. `incident-service` owns and persists all AI-generated content.

The natural alternative was for `genai-service` to own a `genai` database and store results there, with the frontend making a separate REST call to fetch AI data alongside incident data. We rejected this because AI-generated content is semantically part of the Incident: it answers questions about that specific Incident and has no value without it. Storing it separately forces client-side aggregation, adds a database to operate and back up, and complicates incident data completeness checks. Making `genai-service` stateless reduces it to a pure compute service with no persistence concerns.
