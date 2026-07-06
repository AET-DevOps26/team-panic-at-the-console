import { useParams, Link } from "react-router-dom";
import { useState } from "react";
import { ArrowLeft, RefreshCw, Clock, Loader2, MessageSquare, AlertTriangle, Zap } from "lucide-react";
import { EditableDescription } from "@/components/incident/EditableDescription";
import { Markdown } from "@/components/Markdown";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { SeverityBadge, StatusBadge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import type React from "react";
import NotFoundPage from "@/pages/NotFoundPage";
import { isNotFound, useIncident, useIncidentEvents, useComments, useAddComment, useRegeneratePostmortem, useRegenerateSeverity, useRegenerateSolutions, useRegenerateSummary, useSetIncidentSeverity, useUpdateIncidentStatus, type Incident, type IncidentEvent, type IncidentStatus, type Severity, type Comment } from "@/api/queries";
import { cn, formatDateTime, formatRelativeTime } from "@/lib/utils";
import { isAutoGenerating, solutionsToMarkdown, useIntervalRerender, REGEN_WATCH_MS } from "@/lib/genai";


function TimelineItem({ event }: { event: IncidentEvent }) {
  const iconClass = "h-2 w-2 rounded-full mt-1.5 shrink-0";
  const dotColor = event.type === "incident_created" ? "bg-blue-500" : event.type === "status_changed" ? "bg-yellow-500" : "bg-slate-400";
  return (
    <div className="flex gap-3">
      <div className="flex flex-col items-center">
        <div className={`${iconClass} ${dotColor}`} />
        <div className="w-px flex-1 bg-border mt-1" />
      </div>
      <div className="pb-4 min-w-0">
        <p className="text-sm">{event.description}</p>
        <p className="text-xs text-muted-foreground mt-0.5">{formatDateTime(event.timestamp)}</p>
      </div>
    </div>
  );
}

type AiField = "summary" | "severitySuggestion" | "solutions" | "postmortem";

// Tracks which AI fields are currently being generated. Two trigger paths:
// explicit Regenerate clicks (a field counts as generating until its value
// changes or REGEN_WATCH_MS passes), and backend auto-generation (creation
// fills summary/severity/solutions, resolution fills the postmortem), which is
// inferred from a still-null field close to its trigger timestamp. Results
// arrive via the SSE stream (useIncidentStream in AppShell); the interval
// re-render only makes the time-window checks expire when no result ever
// arrives. Lives at page level (not in the AI tab) because Radix unmounts
// inactive tab content, which would drop the regen state.
function useGenaiProgress(incidentId: string, incident: Incident | undefined) {
  const mutations = {
    summary: useRegenerateSummary(incidentId),
    severitySuggestion: useRegenerateSeverity(incidentId),
    solutions: useRegenerateSolutions(incidentId),
    postmortem: useRegeneratePostmortem(incidentId),
  };

  // Per-field value at request time + deadline for explicit regenerations.
  const [regens, setRegens] = useState<Partial<Record<AiField, { prev: string | null; until: number }>>>({});

  const now = Date.now();
  const value = (field: AiField) => incident?.[field] ?? null;

  const regenActive = (field: AiField) => {
    const entry = regens[field];
    return !!entry && !mutations[field].isError && now < entry.until && value(field) === entry.prev;
  };

  const generating: Record<AiField, boolean> = {
    summary: regenActive("summary") || isAutoGenerating(incident?.createdAt, incident?.summary, now),
    severitySuggestion: regenActive("severitySuggestion") || isAutoGenerating(incident?.createdAt, incident?.severitySuggestion, now),
    solutions: regenActive("solutions") || isAutoGenerating(incident?.createdAt, incident?.solutions, now),
    postmortem: regenActive("postmortem") || isAutoGenerating(incident?.resolvedAt, incident?.postmortem, now),
  };
  const anyGenerating = Object.values(generating).some(Boolean);

  useIntervalRerender(anyGenerating);

  const regenerate = (field: AiField) => {
    setRegens((r) => ({ ...r, [field]: { prev: value(field), until: Date.now() + REGEN_WATCH_MS } }));
    mutations[field].mutate();
  };

  return { generating, anyGenerating, regenerate };
}

function AiPanel({ title, onRegenerate, generating, generatedAt, children }: { title: string; onRegenerate: () => void; generating: boolean; generatedAt?: string | null; children: React.ReactNode }) {
  return (
    <div className="rounded-lg border p-4 space-y-2">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm font-medium">{title}</p>
        <div className="flex items-center gap-2">
          {generatedAt && (
            <span className="text-xs text-muted-foreground/70" title={formatDateTime(generatedAt)}>
              Updated {formatRelativeTime(generatedAt)}
            </span>
          )}
          <Button variant="ghost" size="sm" onClick={onRegenerate} disabled={generating}>
            {generating ? <Loader2 className="h-3 w-3 animate-spin" /> : <RefreshCw className="h-3 w-3" />}
            <span className="ml-1 text-xs">{generating ? "Generating…" : "Regenerate"}</span>
          </Button>
        </div>
      </div>
      {children}
    </div>
  );
}

function GeneratingIndicator({ label }: { label: string }) {
  return (
    <div className="flex items-center gap-2 text-sm text-muted-foreground">
      <Loader2 className="h-3.5 w-3.5 animate-spin shrink-0" />
      <span className="italic">{label}</span>
    </div>
  );
}

function AiSection({ incident, generating, onRegenerate }: { incident: Incident; generating: Record<AiField, boolean>; onRegenerate: (field: AiField) => void }) {
  const solutionsMarkdown = incident.solutions ? solutionsToMarkdown(incident.solutions) : "";

  return (
    <div className="space-y-4">
      <AiPanel title="Summary" onRegenerate={() => onRegenerate("summary")} generating={generating.summary} generatedAt={incident.summaryGeneratedAt}>
        {incident.summary ? (
          <Markdown className={cn(generating.summary && "opacity-50")}>{incident.summary}</Markdown>
        ) : generating.summary ? (
          <GeneratingIndicator label="Generating summary…" />
        ) : (
          <p className="text-sm text-muted-foreground italic">No summary yet. Click Regenerate to trigger AI analysis.</p>
        )}
      </AiPanel>

      <AiPanel title="Severity suggestion" onRegenerate={() => onRegenerate("severitySuggestion")} generating={generating.severitySuggestion} generatedAt={incident.severitySuggestionGeneratedAt}>
        {incident.severitySuggestion ? (
          <Markdown className={cn(generating.severitySuggestion && "opacity-50")}>{incident.severitySuggestion}</Markdown>
        ) : generating.severitySuggestion ? (
          <GeneratingIndicator label="Generating suggestion…" />
        ) : (
          <p className="text-sm text-muted-foreground italic">AI-suggested severity will appear here after generation.</p>
        )}
      </AiPanel>

      <AiPanel title="Solution suggestions" onRegenerate={() => onRegenerate("solutions")} generating={generating.solutions} generatedAt={incident.solutionsGeneratedAt}>
        {solutionsMarkdown ? (
          <Markdown className={cn(generating.solutions && "opacity-50")}>{solutionsMarkdown}</Markdown>
        ) : generating.solutions ? (
          <GeneratingIndicator label="Generating suggestions…" />
        ) : (
          <p className="text-sm text-muted-foreground italic">Remediation steps will appear here after generation.</p>
        )}
      </AiPanel>

      {incident.status === "resolved" && (
        <AiPanel title="Postmortem draft" onRegenerate={() => onRegenerate("postmortem")} generating={generating.postmortem} generatedAt={incident.postmortemGeneratedAt}>
          {incident.postmortem ? (
            <Markdown className={cn(generating.postmortem && "opacity-50")}>{incident.postmortem}</Markdown>
          ) : generating.postmortem ? (
            <GeneratingIndicator label="Generating postmortem…" />
          ) : (
            <p className="text-sm text-muted-foreground italic">Root cause, timeline, and action items will appear after generation.</p>
          )}
        </AiPanel>
      )}
    </div>
  );
}

const STATUS_OPTIONS: { value: IncidentStatus; label: string }[] = [
  { value: "open", label: "Open" },
  { value: "investigating", label: "Investigating" },
  { value: "resolved", label: "Resolved" },
];

const SEVERITY_OPTIONS: Severity[] = ["SEV1", "SEV2", "SEV3", "SEV4"];

function ActionsCard({ incident }: { incident: Incident }) {
  const updateStatus = useUpdateIncidentStatus(incident.id);
  const setSeverity = useSetIncidentSeverity(incident.id);

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base">Actions</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-1.5">
          <Label htmlFor="incident-status">Status</Label>
          <Select value={incident.status} onValueChange={(value) => value !== incident.status && updateStatus.mutate({ status: value as IncidentStatus })} disabled={updateStatus.isPending}>
            <SelectTrigger id="incident-status" className="h-9">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {STATUS_OPTIONS.map(({ value, label }) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {updateStatus.isError && <p className="text-xs text-red-600">Failed to update status. Please try again.</p>}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="incident-severity">Severity</Label>
          <Select value={incident.severity} onValueChange={(value) => value !== incident.severity && setSeverity.mutate({ severity: value as Severity })} disabled={setSeverity.isPending}>
            <SelectTrigger id="incident-severity" className="h-9">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {SEVERITY_OPTIONS.map((sev) => (
                <SelectItem key={sev} value={sev}>
                  {sev}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {setSeverity.isError && <p className="text-xs text-red-600">Failed to update severity. Please try again.</p>}
        </div>
      </CardContent>
    </Card>
  );
}

export default function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: incident, isLoading, isError, error, refetch } = useIncident(id ?? "");
  const { data: events, isLoading: eventsLoading } = useIncidentEvents(id ?? "");
  const { data: comments, isLoading: commentsLoading } = useComments(id ?? "");
  const addComment = useAddComment(id ?? "");
  const [commentText, setCommentText] = useState("");
  const genai = useGenaiProgress(id ?? "", incident);

  if (isLoading) {
    return (
      <div className="p-6 space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-4 w-48" />
        <div className="grid grid-cols-3 gap-6 mt-6">
          <div className="col-span-2 space-y-3">
            <Skeleton className="h-48 w-full" />
          </div>
          <div className="space-y-3">
            <Skeleton className="h-32 w-full" />
            <Skeleton className="h-32 w-full" />
          </div>
        </div>
      </div>
    );
  }

  if (isError || !incident) {
    if (isNotFound(error)) {
      return <NotFoundPage className="min-h-full" title="Incident not found" description="No incident with this ID exists. It may have been deleted." />;
    }
    return (
      <div className="flex min-h-full flex-col items-center justify-center gap-4 p-6 text-center">
        <AlertTriangle className="h-10 w-10 text-muted-foreground/40" />
        <h1 className="text-xl font-semibold">Failed to load incident</h1>
        <p className="text-sm text-muted-foreground">Something went wrong while fetching this incident. Check your connection and try again.</p>
        <Button variant="outline" onClick={() => void refetch()}>
          Try again
        </Button>
      </div>
    );
  }

  const evts = events ?? [];

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <header className="border-b bg-white px-6 py-4">
        <div className="flex items-center gap-2 text-sm text-muted-foreground mb-3">
          <Link to="/incidents" className="flex items-center gap-1 hover:text-foreground transition-colors">
            <ArrowLeft className="h-3.5 w-3.5" />
            Incidents
          </Link>
          <span>/</span>
          <span className="text-foreground font-medium truncate max-w-xs">{incident.title}</span>
        </div>
        <div className="flex items-start justify-between gap-4">
          <h1 className="text-xl font-semibold min-w-0">{incident.title}</h1>
          <div className="flex items-center gap-2 shrink-0">
            <SeverityBadge severity={incident.severity} />
            <StatusBadge status={incident.status} />
          </div>
        </div>
        <EditableDescription incident={incident} />
      </header>

      {/* Body: 2-column layout */}
      <div className="flex-1 overflow-auto">
        <div className="grid grid-cols-3 gap-6 p-6 min-h-full">
          {/* Main column (2/3) */}
          <div className="col-span-2 space-y-4">
            <Tabs defaultValue="timeline">
              <TabsList>
                <TabsTrigger value="timeline">
                  <Clock className="h-3.5 w-3.5 mr-1.5" />
                  Timeline
                </TabsTrigger>
                <TabsTrigger value="comments">
                  <MessageSquare className="h-3.5 w-3.5 mr-1.5" />
                  Comments
                </TabsTrigger>
                <TabsTrigger value="ai">
                  {genai.anyGenerating ? <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" /> : <Zap className="h-3.5 w-3.5 mr-1.5" />}
                  AI Insights
                </TabsTrigger>
              </TabsList>

              <TabsContent value="timeline" className="mt-4">
                <Card>
                  <CardContent className="pt-6">
                    {eventsLoading ? (
                      <div className="space-y-3">
                        {Array.from({ length: 4 }).map((_, i) => (
                          <Skeleton key={i} className="h-10 w-full" />
                        ))}
                      </div>
                    ) : evts.length === 0 ? (
                      <p className="text-sm text-muted-foreground text-center py-6">No events recorded yet.</p>
                    ) : (
                      <div className="space-y-0">
                        {evts.map((ev, i) => (
                          <TimelineItem key={i} event={ev} />
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="comments" className="mt-4 space-y-4">
                <Card>
                  <CardContent className="pt-6">
                    {commentsLoading ? (
                      <div className="space-y-3">
                        {Array.from({ length: 3 }).map((_, i) => (
                          <Skeleton key={i} className="h-12 w-full" />
                        ))}
                      </div>
                    ) : !comments || comments.length === 0 ? (
                      <p className="text-sm text-muted-foreground text-center py-6">No comments yet. Be the first to add context.</p>
                    ) : (
                      <div className="space-y-4">
                        {comments.map((c: Comment) => (
                          <div key={c.id} className="space-y-1">
                            <p className="text-xs text-muted-foreground">{formatDateTime(c.createdAt)}</p>
                            <p className="text-sm">{c.text}</p>
                            <Separator />
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader className="pb-3">
                    <CardTitle className="text-base">Add a comment</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div className="space-y-1.5">
                      <Label htmlFor="comment">Comment</Label>
                      <Textarea id="comment" placeholder="What have you found? What did you try?" rows={3} value={commentText} onChange={(e) => setCommentText(e.target.value)} />
                    </div>
                    <Button
                      size="sm"
                      disabled={!commentText.trim() || addComment.isPending}
                      onClick={async () => {
                        await addComment.mutateAsync({ text: commentText });
                        setCommentText("");
                      }}
                    >
                      {addComment.isPending ? "Posting…" : "Post comment"}
                    </Button>
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="ai" className="mt-4">
                <AiSection incident={incident} generating={genai.generating} onRegenerate={genai.regenerate} />
              </TabsContent>
            </Tabs>
          </div>

          {/* Sidebar column (1/3) */}
          <div className="space-y-4">
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base">Details</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Status</span>
                  <StatusBadge status={incident.status} />
                </div>
                <Separator />
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Severity</span>
                  <SeverityBadge severity={incident.severity} />
                </div>
                <Separator />
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Created</span>
                  <span className="text-right">{formatRelativeTime(incident.createdAt)}</span>
                </div>
                {incident.resolvedAt && (
                  <>
                    <Separator />
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Resolved</span>
                      <span className="text-right">{formatRelativeTime(incident.resolvedAt)}</span>
                    </div>
                  </>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base">Assignees</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">No assignees yet.</p>
                <Button variant="outline" size="sm" className="mt-3 w-full">
                  Assign user
                </Button>
              </CardContent>
            </Card>

            <ActionsCard incident={incident} />
          </div>
        </div>
      </div>

    </div>
  );
}
