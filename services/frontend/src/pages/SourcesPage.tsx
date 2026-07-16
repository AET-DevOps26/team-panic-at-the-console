import { useState } from "react";
import { AlertTriangle, Check, Copy, KeyRound, Plus, RefreshCw, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import {
  ApiError,
  useCreateWebhookSource,
  useDeleteWebhookSource,
  useExternalEvent,
  useExternalEvents,
  useRotateWebhookSourceSecret,
  useWebhookSources,
  type WebhookSource,
  type WebhookSourceWithSecret,
} from "@/api/queries";
import { formatDateTime, formatRelativeTime } from "@/lib/utils";

const SLUG_PATTERN = /^[a-z0-9][a-z0-9_-]{0,63}$/;

/**
 * External payload URL for a source. The compose edge proxy and the Helm
 * ingress both route `/webhooks/*` on the same host as the frontend, so the
 * page origin is the URL senders must use in every deployment.
 */
function payloadUrl(slug: string): string {
  return `${window.location.origin}/webhooks/${slug}`;
}

/**
 * navigator.clipboard only exists in secure contexts; the Azure VM serves the
 * app over plain http://<ip>:8080, so fall back to a hidden textarea and
 * execCommand("copy") there.
 */
async function copyToClipboard(value: string): Promise<boolean> {
  if (window.isSecureContext && navigator.clipboard) {
    await navigator.clipboard.writeText(value);
    return true;
  }
  const textarea = document.createElement("textarea");
  textarea.value = value;
  textarea.style.position = "fixed";
  textarea.style.opacity = "0";
  document.body.appendChild(textarea);
  textarea.select();
  try {
    return document.execCommand("copy");
  } finally {
    textarea.remove();
  }
}

function CopyButton({ value, title = "Copy" }: { value: string; title?: string }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    if (!(await copyToClipboard(value))) return;
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  return (
    <Button variant="ghost" size="icon" className="h-7 w-7 shrink-0" onClick={copy} title={title}>
      {copied ? <Check className="h-3.5 w-3.5 text-green-600" /> : <Copy className="h-3.5 w-3.5" />}
    </Button>
  );
}

function CopyableValue({ value, mono = true }: { value: string; mono?: boolean }) {
  return (
    <div className="flex items-center gap-1 rounded-md border bg-muted/50 pl-3 pr-1 py-1">
      <code className={`flex-1 overflow-x-auto whitespace-nowrap pb-0.5 text-xs [scrollbar-width:thin] ${mono ? "font-mono" : ""}`}>{value}</code>
      <CopyButton value={value} />
    </div>
  );
}

/** The one place a generated secret is shown; it cannot be retrieved again. */
function SecretReveal({ result }: { result: WebhookSourceWithSecret }) {
  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label>Payload URL</Label>
        <CopyableValue value={payloadUrl(result.slug)} />
      </div>
      <div className="space-y-2">
        <Label>Secret</Label>
        <CopyableValue value={result.secret} />
        <p className="flex items-start gap-1.5 text-xs text-amber-600">
          <AlertTriangle className="h-3.5 w-3.5 shrink-0 mt-0.5" />
          Store this secret now: it is shown only once. Configure it on the sending system; every delivery for
          this source must be signed with it (X-Hub-Signature-256).
        </p>
      </div>
    </div>
  );
}

function AddSourceDialog() {
  const [open, setOpen] = useState(false);
  const [slug, setSlug] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<WebhookSourceWithSecret | null>(null);
  const createSource = useCreateWebhookSource();

  const slugValid = SLUG_PATTERN.test(slug);

  function reset(nextOpen: boolean) {
    setOpen(nextOpen);
    if (!nextOpen) {
      setSlug("");
      setError(null);
      setCreated(null);
    }
  }

  async function handleCreate() {
    setError(null);
    try {
      setCreated(await createSource.mutateAsync({ slug }));
    } catch (err) {
      // The gateway proxy strips downstream error bodies, so map by status.
      if (err instanceof ApiError && err.status === 409) {
        setError("A source with this name already exists");
      } else {
        setError(err instanceof Error ? err.message : "Failed to register source");
      }
    }
  }

  return (
    <Dialog open={open} onOpenChange={reset}>
      <DialogTrigger asChild>
        <Button>
          <Plus className="h-4 w-4" />
          Add source
        </Button>
      </DialogTrigger>
      {/* Wide enough that the payload URL and secret fit without scrolling. */}
      <DialogContent className={created ? "sm:max-w-3xl" : "sm:max-w-lg"}>
        {created ? (
          <>
            <DialogHeader>
              <DialogTitle>Source “{created.slug}” registered</DialogTitle>
              <DialogDescription>Point the sending system at the payload URL and give it the secret.</DialogDescription>
            </DialogHeader>
            <SecretReveal result={created} />
            <DialogFooter>
              <Button onClick={() => reset(false)}>I stored the secret</Button>
            </DialogFooter>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>Add source</DialogTitle>
              <DialogDescription>
                Registers a webhook source and generates its signing secret. The name becomes part of the payload
                URL, e.g. <code className="font-mono text-xs">github</code> or <code className="font-mono text-xs">grafana</code>.
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-2">
              <div className="space-y-2">
                <Label htmlFor="sourceSlug">Source name</Label>
                <Input
                  id="sourceSlug"
                  placeholder="e.g. github"
                  value={slug}
                  onChange={(e) => {
                    setSlug(e.target.value.toLowerCase());
                    setError(null);
                  }}
                  maxLength={64}
                />
                <p className="text-xs text-muted-foreground">
                  Lowercase letters, digits, <code className="font-mono">-</code> and <code className="font-mono">_</code>; must start with a letter or digit.
                </p>
                {slugValid && <p className="break-all text-xs text-muted-foreground font-mono">{payloadUrl(slug)}</p>}
              </div>
              {error && <p className="text-sm text-destructive">{error}</p>}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => reset(false)}>
                Cancel
              </Button>
              <Button onClick={handleCreate} disabled={!slugValid || createSource.isPending}>
                {createSource.isPending ? "Registering…" : "Register source"}
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}

function RotateSecretDialog({ slug }: { slug: string }) {
  const [open, setOpen] = useState(false);
  const [rotated, setRotated] = useState<WebhookSourceWithSecret | null>(null);
  const [error, setError] = useState<string | null>(null);
  const rotateSecret = useRotateWebhookSourceSecret();

  function reset(nextOpen: boolean) {
    setOpen(nextOpen);
    if (!nextOpen) {
      setRotated(null);
      setError(null);
    }
  }

  async function handleRotate() {
    setError(null);
    try {
      setRotated(await rotateSecret.mutateAsync(slug));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to rotate secret");
    }
  }

  return (
    <Dialog open={open} onOpenChange={reset}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="icon" className="h-8 w-8" title="Rotate secret">
          <KeyRound className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent className={rotated ? "sm:max-w-3xl" : "sm:max-w-lg"}>
        {rotated ? (
          <>
            <DialogHeader>
              <DialogTitle>New secret for “{slug}”</DialogTitle>
              <DialogDescription>The old secret stopped working. Update the sending system.</DialogDescription>
            </DialogHeader>
            <SecretReveal result={rotated} />
            <DialogFooter>
              <Button onClick={() => reset(false)}>I stored the secret</Button>
            </DialogFooter>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>Rotate secret for “{slug}”?</DialogTitle>
              <DialogDescription>
                A new secret is generated and the current one stops working immediately: deliveries signed with it
                will be rejected until the sender is updated.
              </DialogDescription>
            </DialogHeader>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <DialogFooter>
              <Button variant="outline" onClick={() => reset(false)}>
                Cancel
              </Button>
              <Button onClick={handleRotate} disabled={rotateSecret.isPending}>
                {rotateSecret.isPending ? "Rotating…" : "Rotate secret"}
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}

function DeleteSourceDialog({ slug }: { slug: string }) {
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const deleteSource = useDeleteWebhookSource();

  async function handleDelete() {
    setError(null);
    try {
      await deleteSource.mutateAsync(slug);
      setOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete source");
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive hover:text-destructive" title="Delete source">
          <Trash2 className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Delete source “{slug}”?</DialogTitle>
          <DialogDescription>
            Signed deliveries for this source will be rejected afterwards. Events already received are kept in the
            audit trail below.
          </DialogDescription>
        </DialogHeader>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button variant="destructive" onClick={handleDelete} disabled={deleteSource.isPending}>
            {deleteSource.isPending ? "Deleting…" : "Delete source"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function SourcesCard() {
  const { data: sources, isLoading } = useWebhookSources();

  return (
    <Card>
      <CardHeader className="pb-4 flex flex-row items-start justify-between space-y-0">
        <div className="space-y-1.5">
          <CardTitle className="text-lg">Registered sources</CardTitle>
          <CardDescription>
            Each source gets a payload URL and a signing secret. Deliveries must carry a valid signature.
          </CardDescription>
        </div>
        <AddSourceDialog />
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-24 w-full" />
        ) : (sources ?? []).length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            No sources registered yet. Add one to get a payload URL and secret for your CI, monitoring, or any
            system that can send webhooks.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Source</TableHead>
                <TableHead>Payload URL</TableHead>
                <TableHead className="w-32">Last event</TableHead>
                <TableHead className="w-32">Created</TableHead>
                <TableHead className="w-20"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(sources ?? []).map((source: WebhookSource) => (
                <TableRow key={source.slug}>
                  <TableCell className="font-medium font-mono text-sm">{source.slug}</TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <code className="font-mono text-xs text-muted-foreground">{payloadUrl(source.slug)}</code>
                      <CopyButton value={payloadUrl(source.slug)} title="Copy payload URL" />
                    </div>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground" title={source.lastEventAt ? formatDateTime(source.lastEventAt) : undefined}>
                    {source.lastEventAt ? formatRelativeTime(source.lastEventAt) : "never"}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground" title={formatDateTime(source.createdAt)}>
                    {formatRelativeTime(source.createdAt)}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center justify-end">
                      <RotateSecretDialog slug={source.slug} />
                      <DeleteSourceDialog slug={source.slug} />
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}

function SetupGuideCard() {
  const curlSnippet = `BODY='{"eventType":"alert_fired","service":"checkout"}'
SIG="sha256=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SECRET" -hex | sed 's/^.*= //')"
curl -X POST ${payloadUrl("<source>")} \\
  -H 'Content-Type: application/json' \\
  -H "X-Hub-Signature-256: $SIG" \\
  -H 'X-Delivery-Id: test-1' \\
  -d "$BODY"`;

  return (
    <Card>
      <CardHeader className="pb-4">
        <CardTitle className="text-lg">Connecting a sender</CardTitle>
        <CardDescription>
          Anything that can send an HTTP POST with a JSON body can be a source. Incidents are created when a Rule
          matches the event’s type.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Tabs defaultValue="github">
          <TabsList>
            <TabsTrigger value="github">GitHub</TabsTrigger>
            <TabsTrigger value="generic">Any sender (curl)</TabsTrigger>
          </TabsList>
          <TabsContent value="github" className="space-y-2 text-sm">
            <ol className="list-decimal space-y-1.5 pl-5">
              <li>Register a source above (e.g. <code className="font-mono text-xs">github</code>) and copy its secret.</li>
              <li>
                In the repository: Settings → Webhooks → Add webhook, paste the payload URL, set content type to{" "}
                <code className="font-mono text-xs">application/json</code>, and paste the secret into the Secret field.
              </li>
              <li>Select the events to send, e.g. Workflow runs for CI failures.</li>
              <li>Save: GitHub sends a ping that should appear under Received events below within seconds.</li>
            </ol>
            <p className="text-xs text-muted-foreground">
              Failed workflow runs are normalised to the event type <code className="font-mono">ci_failure</code>,
              successful ones to <code className="font-mono">ci_success</code>; other GitHub events become{" "}
              <code className="font-mono">github.&lt;event&gt;</code>.
            </p>
          </TabsContent>
          <TabsContent value="generic" className="space-y-2 text-sm">
            <p>
              Sign the raw request body with the source secret (HMAC-SHA256, GitHub convention). With{" "}
              <code className="font-mono text-xs">$SECRET</code> set to the secret from registration:
            </p>
            <div className="relative">
              <pre className="overflow-x-auto rounded-md border bg-muted/50 p-3 text-xs font-mono">{curlSnippet}</pre>
              <div className="absolute right-1.5 top-1.5">
                <CopyButton value={curlSnippet} title="Copy example" />
              </div>
            </div>
            <p className="text-xs text-muted-foreground">
              The event type Rules match on is taken from an <code className="font-mono">eventType</code> field in the
              payload, or an <code className="font-mono">X-Event-Type</code> header. Reused{" "}
              <code className="font-mono">X-Delivery-Id</code> values are deduplicated.
            </p>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}

function EventDetailDialog({ eventId, onClose }: { eventId: string | null; onClose: () => void }) {
  const { data: event, isLoading } = useExternalEvent(eventId);

  return (
    <Dialog open={Boolean(eventId)} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>External event</DialogTitle>
          {event && (
            <DialogDescription>
              <code className="font-mono text-xs">{event.source}</code> · {event.eventType} ·{" "}
              {formatDateTime(event.receivedAt)}
            </DialogDescription>
          )}
        </DialogHeader>
        {isLoading || !event ? (
          <Skeleton className="h-48 w-full" />
        ) : (
          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-sm">
              <span className="text-muted-foreground">Event type</span>
              <code className="font-mono text-xs break-all">{event.eventType}</code>
              <span className="text-muted-foreground">Delivery ID</span>
              <code className="font-mono text-xs break-all">{event.deliveryId ?? "–"}</code>
              <span className="text-muted-foreground">Forwarded to rule engine</span>
              <span>{event.publishedAt ? formatDateTime(event.publishedAt) : "pending"}</span>
            </div>
            <div className="space-y-1.5">
              <Label>Raw payload</Label>
              <pre className="max-h-80 overflow-auto rounded-md border bg-muted/50 p-3 text-xs font-mono">
                {JSON.stringify(event.rawPayload, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

const EVENTS_PAGE_SIZE = 25;

function EventsCard() {
  const { data: sources } = useWebhookSources();
  const [sourceFilter, setSourceFilter] = useState<string>("all");
  const [page, setPage] = useState(0);
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const { data, isLoading, refetch, isFetching } = useExternalEvents({
    source: sourceFilter === "all" ? undefined : sourceFilter,
    page,
    size: EVENTS_PAGE_SIZE,
  });

  // Sources that sent events but were never registered (env-secret or
  // unverified) should still be filterable.
  const knownSlugs = new Set((sources ?? []).map((s) => s.slug));
  (data?.items ?? []).forEach((event) => knownSlugs.add(event.source));
  if (sourceFilter !== "all") knownSlugs.add(sourceFilter);

  const totalPages = data ? Math.max(1, Math.ceil(data.total / EVENTS_PAGE_SIZE)) : 1;

  return (
    <Card>
      <CardHeader className="pb-4 flex flex-row items-start justify-between space-y-0">
        <div className="space-y-1.5">
          <CardTitle className="text-lg">Received events</CardTitle>
          <CardDescription>
            Every accepted delivery, newest first: the fastest way to check a webhook is wired up correctly.
          </CardDescription>
        </div>
        <div className="flex items-center gap-2">
          <Select
            value={sourceFilter}
            onValueChange={(value) => {
              setSourceFilter(value);
              setPage(0);
            }}
          >
            <SelectTrigger className="w-40">
              <SelectValue placeholder="All sources" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All sources</SelectItem>
              {[...knownSlugs].sort().map((slug) => (
                <SelectItem key={slug} value={slug}>
                  {slug}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button variant="ghost" size="icon" onClick={() => refetch()} title="Refresh">
            <RefreshCw className={`h-4 w-4 ${isFetching ? "animate-spin" : ""}`} />
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-32 w-full" />
        ) : (data?.items ?? []).length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            No events received yet. Configure a sender above; new deliveries show up here within seconds.
          </p>
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-32">Received</TableHead>
                  <TableHead className="w-32">Source</TableHead>
                  <TableHead>Event type</TableHead>
                  <TableHead className="w-40">Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(data?.items ?? []).map((event) => (
                  <TableRow key={event.id} className="cursor-pointer" onClick={() => setSelectedEventId(event.id)}>
                    <TableCell className="text-sm text-muted-foreground" title={formatDateTime(event.receivedAt)}>
                      {formatRelativeTime(event.receivedAt)}
                    </TableCell>
                    <TableCell className="font-mono text-sm">{event.source}</TableCell>
                    <TableCell className="font-mono text-sm">{event.eventType}</TableCell>
                    <TableCell>
                      {event.publishedAt ? (
                        <Badge variant="outline" className="text-green-700 border-green-300">
                          forwarded
                        </Badge>
                      ) : (
                        <Badge variant="outline" className="text-amber-700 border-amber-300">
                          pending
                        </Badge>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {totalPages > 1 && (
              <div className="flex items-center justify-between pt-3">
                <p className="text-xs text-muted-foreground">
                  Page {page + 1} of {totalPages} · {data?.total} events
                </p>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page + 1 >= totalPages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </CardContent>
      <EventDetailDialog eventId={selectedEventId} onClose={() => setSelectedEventId(null)} />
    </Card>
  );
}

export default function SourcesPage() {
  return (
    <div className="flex flex-col h-full">
      <header className="border-b bg-white px-6 py-4">
        <h1 className="text-xl font-semibold">Sources</h1>
        <p className="text-sm text-muted-foreground">
          Connect CI, monitoring, and other external systems via webhooks
        </p>
      </header>

      <div className="flex-1 overflow-y-auto p-6">
        <div className="max-w-4xl space-y-6">
          <SourcesCard />
          <SetupGuideCard />
          <EventsCard />
        </div>
      </div>
    </div>
  );
}
