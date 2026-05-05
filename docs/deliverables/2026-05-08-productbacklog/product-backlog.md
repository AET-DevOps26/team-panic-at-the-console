# Product Backlog (Initial)

| Priority  | User Story                                                                                                                                                                          | Acceptance Criteria                                                                                                                                        |
| --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 🔴 High   | As an Incident Responder, I want to have all the relevant collected context regarding an incident, so that I dont have to search around and potentially loose information and time. | - Incident detail view shows description, status, assignments, timeline, comments<br>- Newly added information is reflected immediately                    |
| 🔴 High   | As an Incident Responder, I want to see a timeline of events for an incident so I understand what happened.                                                                          | - Timeline is backed by an append-only incident event log (event-service)<br>- Timeline displays all events in chronological order<br>- Events include type (status change, comment, assignment)<br>- Each event is timestamped         |
| 🔴 High   | As an Incident Responder, I want to view a list of incidents so I can see active problems.                                                                                          | - Dashboard shows all incidents with key fields (title, status, severity, timestamp)<br>- Empty state shown when no incidents exist                        |
| 🔴 High   | As an Incident Responder, I want to see the status of incidents so that I understand the urgency.                                                                                   | - Status uses the canonical lifecycle (at minimum: open, investigating, resolved)<br>- Each incident displays its current status<br>- Status updates are immediately visible                                                                    |
| 🔴 High   | As an Incident Responder I want to create new incidents manually, so that incidents missed by the system don’t get lost.                                                            | - User can create incident with required fields (title, description)<br>- Validation prevents incomplete submissions<br>- New incidents start in **open** state<br>- Created incident appears in list |
| 🔴 High   | As an Incident Commander I want to update the incident status, so I can track progress.                                                                                             | - Status updates persist correctly<br>- Changes are visible to all users<br>- Status changes are recorded in timeline                                      |
| 🔴 High   | As an Incident Commander, I want to assign people to certain tasks, so that responsibilities are clear.                                                                             | - Users can be assigned to incidents<br>- Assignment is visible in incident view<br>- Assigned users can see their responsibility                          |
| 🔴 High   | As a General Team Member I want to be able to comment, so that my context is considered even when I am not part of the incident team.                                               | - Users can add comments to incidents<br>- Comments appear in timeline<br>- Comments are ordered chronologically                                           |
| 🟠 Medium | As an Incident Responder, I want to be informed appropriately about new automatically created incidents, so that I can check them.                                                  | - Users receive notification when relevant incident is created<br>- Notification links to incident detail view                                             |
| 🟠 Medium | As an Incident Commander I want to get incident summaries so that I can make informed decisions fast.                                                                               | - Incident summary is displayed in detail view<br>- Summary updates when incident data changes                                                             |
| 🟠 Medium | As an Incident Responder, I want severity suggestions so I can prioritize.                                                                                                           | - Severity uses defined levels (e.g., SEV1, SEV2, SEV3, SEV4) with short descriptions<br>- Suggested severity is displayed for incidents<br>- Clearly marked as non-binding suggestion<br>- Each incident has one current severity and any change is recorded in the timeline                                                              |
| 🟠 Medium | As a Incident Responder, I want solution suggestions so I can act faster.                                                                                                           | - Suggested solutions are displayed in incident view<br>- Clearly distinguishable from user input                                                          |
| 🟠 Medium | As an Incident Commander I want to add multiple potential incident sources, so that I dont need different tools or workspaces for all my systems.                                   | - Users can configure external sources (e.g., webhook)<br>- Valid configurations are stored<br>- Incoming events can trigger incidents                     |
| 🟠 Medium | As an Incident Commander, I want severity to escalate when follow-up signals show the situation worsening, so responders prioritize correctly.                                       | - Rules can raise severity (e.g., repeated failure on same service)<br>- Escalation visible in detail view and timeline<br>- If notifications ship, subscribers informed of severity change |
| 🟢 Low    | As a Incident Responder, I want a postmortem draft so documentation is easier.                                                                                                      | - Postmortem draft is available after incident resolution<br>- Includes summary, timeline, and suggested root cause                                        |

# Definition of Done (DoD)

A backlog item is considered done when:

- Feature is implemented and usable via UI or API
- Acceptance criteria are fulfilled
- No critical bugs remain

<br>

# Stakeholders

## Incident Responders (Developers / SREs)

Primary users responsible for investigating and resolving incidents.
They need fast access to all relevant context, clear timelines, and actionable insights to reduce resolution time.

## Incident Commanders

Responsible for coordinating incident response and making decisions.
They require high-level summaries, visibility into progress, and the ability to assign responsibilities and manage status.

## General Team Members

Contributors who are not directly responsible for resolving incidents but can provide useful context.
They need lightweight ways to add information without disrupting the main responders.

<br>

# Prioritization Rationale

- 🔴 High: Core incident workflow
- 🟠 Medium: Enhancements that improve efficiency (AI, aggregation)
- 🟢 Low: Post-incident features and optimizations
