import { useState } from "react";
import { Plus, Trash2, Pencil, GitBranch } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import {
  ApiError,
  useCreateRule,
  useDeleteRule,
  useRules,
  useUpdateRule,
  useWebhookSources,
  type Rule,
  type RuleInput,
  type RuleOperator,
  type Severity,
} from "@/api/queries";

/** Radix Select forbids an empty-string item value, so "any source" needs a sentinel. */
const ANY_SOURCE = "__any__";

const OPERATORS: { value: RuleOperator; label: string; needsValue: boolean }[] = [
  { value: "equals", label: "equals", needsValue: true },
  { value: "not_equals", label: "does not equal", needsValue: true },
  { value: "contains", label: "contains", needsValue: true },
  { value: "not_contains", label: "does not contain", needsValue: true },
  { value: "matches", label: "matches regex", needsValue: true },
  { value: "in", label: "in list (comma-sep.)", needsValue: true },
  { value: "exists", label: "exists", needsValue: false },
  { value: "not_exists", label: "does not exist", needsValue: false },
];

const SEVERITIES: Severity[] = ["SEV1", "SEV2", "SEV3", "SEV4"];

function operatorNeedsValue(op: RuleOperator): boolean {
  return OPERATORS.find((o) => o.value === op)?.needsValue ?? true;
}

type ConditionDraft = { field: string; operator: RuleOperator; value: string };
type MetaDraft = { label: string; field: string };
type RuleDraft = {
  name: string;
  enabled: boolean;
  priority: number;
  source: string;
  severity: Severity;
  titleTemplate: string;
  descriptionTemplate: string;
  dedupKeyTemplate: string;
  conditions: ConditionDraft[];
  metadataFields: MetaDraft[];
};

function emptyDraft(): RuleDraft {
  return {
    name: "",
    enabled: true,
    priority: 100,
    source: "",
    severity: "SEV2",
    titleTemplate: "",
    descriptionTemplate: "",
    dedupKeyTemplate: "",
    conditions: [{ field: "eventType", operator: "equals", value: "" }],
    metadataFields: [],
  };
}

function toDraft(rule: Rule): RuleDraft {
  return {
    name: rule.name,
    enabled: rule.enabled,
    priority: rule.priority,
    source: rule.source ?? "",
    severity: rule.severity,
    titleTemplate: rule.titleTemplate,
    descriptionTemplate: rule.descriptionTemplate ?? "",
    dedupKeyTemplate: rule.dedupKeyTemplate ?? "",
    conditions: (rule.conditions ?? []).map((c) => ({ field: c.field, operator: c.operator, value: c.value ?? "" })),
    metadataFields: (rule.metadataFields ?? []).map((m) => ({ label: m.label, field: m.field })),
  };
}

function toInput(draft: RuleDraft): RuleInput {
  return {
    name: draft.name.trim(),
    enabled: draft.enabled,
    priority: draft.priority,
    source: draft.source.trim() || undefined,
    severity: draft.severity,
    titleTemplate: draft.titleTemplate.trim(),
    descriptionTemplate: draft.descriptionTemplate.trim() || undefined,
    dedupKeyTemplate: draft.dedupKeyTemplate.trim() || undefined,
    conditions: draft.conditions.map((c) => ({
      field: c.field.trim(),
      operator: c.operator,
      value: operatorNeedsValue(c.operator) ? c.value : undefined,
    })),
    metadataFields: draft.metadataFields.map((m) => ({ label: m.label.trim(), field: m.field.trim() })),
  };
}

function validate(draft: RuleDraft): string | null {
  if (!draft.name.trim()) return "Give the rule a name.";
  if (!draft.titleTemplate.trim()) return "The incident title template is required.";
  for (const c of draft.conditions) {
    if (!c.field.trim()) return "Every condition needs a field path.";
    if (operatorNeedsValue(c.operator) && !c.value.trim()) return `Operator "${c.operator}" needs a value.`;
  }
  for (const m of draft.metadataFields) {
    if (!m.label.trim() || !m.field.trim()) return "Every metadata field needs a label and a field path.";
  }
  return null;
}

function RuleDialog({ existing, trigger }: { existing?: Rule; trigger: React.ReactNode }) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<RuleDraft>(emptyDraft);
  const [error, setError] = useState<string | null>(null);
  const createRule = useCreateRule();
  const updateRule = useUpdateRule();
  const { data: sources } = useWebhookSources();
  const pending = createRule.isPending || updateRule.isPending;

  // Registered sources, plus the rule's own source if it is no longer
  // registered, so editing an existing rule never silently drops its scope.
  const registered = (sources ?? []).map((s) => s.slug);
  const registeredSet = new Set(registered);
  const sourceOptions = [...registered];
  if (draft.source && !registeredSet.has(draft.source)) sourceOptions.push(draft.source);

  function reset(nextOpen: boolean) {
    setOpen(nextOpen);
    if (nextOpen) {
      setDraft(existing ? toDraft(existing) : emptyDraft());
      setError(null);
    }
  }

  function patch(next: Partial<RuleDraft>) {
    setDraft((d) => ({ ...d, ...next }));
  }

  function setCondition(index: number, next: Partial<ConditionDraft>) {
    setDraft((d) => ({
      ...d,
      conditions: d.conditions.map((c, i) => (i === index ? { ...c, ...next } : c)),
    }));
  }

  function setMeta(index: number, next: Partial<MetaDraft>) {
    setDraft((d) => ({
      ...d,
      metadataFields: d.metadataFields.map((m, i) => (i === index ? { ...m, ...next } : m)),
    }));
  }

  async function handleSave() {
    const validationError = validate(draft);
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    try {
      if (existing) {
        await updateRule.mutateAsync({ id: existing.id, body: toInput(draft) });
      } else {
        await createRule.mutateAsync(toInput(draft));
      }
      setOpen(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : err instanceof Error ? err.message : "Failed to save rule");
    }
  }

  return (
    <Dialog open={open} onOpenChange={reset}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent className="sm:max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{existing ? "Edit rule" : "New rule"}</DialogTitle>
          <DialogDescription>
            When an event matches every condition, an incident is created from the templates below. Field paths are
            dotted, rooted at <code className="font-mono text-xs">source</code>, <code className="font-mono text-xs">eventType</code> and{" "}
            <code className="font-mono text-xs">payload</code> (the raw webhook body).
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5 py-1">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="ruleName">Name</Label>
              <Input id="ruleName" value={draft.name} onChange={(e) => patch({ name: e.target.value })} placeholder="e.g. GitHub CI failures" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="ruleSource">Source</Label>
              <Select
                value={draft.source === "" ? ANY_SOURCE : draft.source}
                onValueChange={(v) => patch({ source: v === ANY_SOURCE ? "" : v })}
              >
                <SelectTrigger id="ruleSource">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ANY_SOURCE}>Any source</SelectItem>
                  {sourceOptions.map((slug) => (
                    <SelectItem key={slug} value={slug}>
                      {slug}
                      {registeredSet.has(slug) ? "" : " (not registered)"}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="ruleSeverity">Severity</Label>
              <Select value={draft.severity} onValueChange={(v) => patch({ severity: v as Severity })}>
                <SelectTrigger id="ruleSeverity">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SEVERITIES.map((s) => (
                    <SelectItem key={s} value={s}>{s}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="rulePriority">Priority</Label>
              <Input
                id="rulePriority"
                type="number"
                value={draft.priority}
                onChange={(e) => patch({ priority: Number(e.target.value) || 0 })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="ruleEnabled">Enabled</Label>
              <label className="flex h-9 items-center gap-2 text-sm">
                <input
                  id="ruleEnabled"
                  type="checkbox"
                  className="h-4 w-4"
                  checked={draft.enabled}
                  onChange={(e) => patch({ enabled: e.target.checked })}
                />
                {draft.enabled ? "Active" : "Disabled"}
              </label>
            </div>
          </div>

          {/* Conditions */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Conditions (all must match)</Label>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => patch({ conditions: [...draft.conditions, { field: "", operator: "equals", value: "" }] })}
              >
                <Plus className="h-3.5 w-3.5" /> Add condition
              </Button>
            </div>
            {draft.conditions.length === 0 && (
              <p className="text-xs text-muted-foreground">No conditions: this rule matches every event from the source.</p>
            )}
            {draft.conditions.map((c, i) => (
              <div key={i} className="flex items-center gap-2">
                <Input
                  className="flex-1 font-mono text-xs"
                  value={c.field}
                  onChange={(e) => setCondition(i, { field: e.target.value })}
                  placeholder="payload.workflow_run.conclusion"
                />
                <Select value={c.operator} onValueChange={(v) => setCondition(i, { operator: v as RuleOperator })}>
                  <SelectTrigger className="w-44 shrink-0">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {OPERATORS.map((o) => (
                      <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Input
                  className="w-40 shrink-0 font-mono text-xs"
                  value={c.value}
                  disabled={!operatorNeedsValue(c.operator)}
                  onChange={(e) => setCondition(i, { value: e.target.value })}
                  placeholder={operatorNeedsValue(c.operator) ? "value" : "—"}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 shrink-0 text-destructive"
                  onClick={() => patch({ conditions: draft.conditions.filter((_, idx) => idx !== i) })}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </div>

          {/* Incident templates */}
          <div className="space-y-2">
            <Label htmlFor="ruleTitle">Incident title</Label>
            <Input
              id="ruleTitle"
              className="font-mono text-xs"
              value={draft.titleTemplate}
              onChange={(e) => patch({ titleTemplate: e.target.value })}
              placeholder="CI failure: {{payload.workflow_run.name}}"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="ruleDescription">Description (optional, Markdown)</Label>
            <Textarea
              id="ruleDescription"
              className="font-mono text-xs"
              rows={2}
              value={draft.descriptionTemplate}
              onChange={(e) => patch({ descriptionTemplate: e.target.value })}
              placeholder="Workflow **{{payload.workflow_run.name}}** failed on `{{payload.workflow_run.head_branch}}`."
            />
          </div>

          {/* Metadata */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Metadata (appended to the description)</Label>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => patch({ metadataFields: [...draft.metadataFields, { label: "", field: "" }] })}
              >
                <Plus className="h-3.5 w-3.5" /> Add field
              </Button>
            </div>
            {draft.metadataFields.map((m, i) => (
              <div key={i} className="flex items-center gap-2">
                <Input
                  className="w-48 shrink-0"
                  value={m.label}
                  onChange={(e) => setMeta(i, { label: e.target.value })}
                  placeholder="Repository"
                />
                <Input
                  className="flex-1 font-mono text-xs"
                  value={m.field}
                  onChange={(e) => setMeta(i, { field: e.target.value })}
                  placeholder="payload.repository.full_name"
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 shrink-0 text-destructive"
                  onClick={() => patch({ metadataFields: draft.metadataFields.filter((_, idx) => idx !== i) })}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </div>

          <div className="space-y-2">
            <Label htmlFor="ruleDedup">Deduplication key (optional)</Label>
            <Input
              id="ruleDedup"
              className="font-mono text-xs"
              value={draft.dedupKeyTemplate}
              onChange={(e) => patch({ dedupKeyTemplate: e.target.value })}
              placeholder="{{payload.workflow_run.id}}"
            />
            <p className="text-xs text-muted-foreground">
              At most one incident is created per rule and key: keeps a whole pipeline run to a single incident.
            </p>
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
          <Button onClick={handleSave} disabled={pending}>
            {pending ? "Saving…" : existing ? "Save changes" : "Create rule"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function DeleteRuleDialog({ rule }: { rule: Rule }) {
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const deleteRule = useDeleteRule();

  async function handleDelete() {
    setError(null);
    try {
      await deleteRule.mutateAsync(rule.id);
      setOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete rule");
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive hover:text-destructive" title="Delete rule">
          <Trash2 className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Delete rule “{rule.name}”?</DialogTitle>
          <DialogDescription>New events will no longer be matched by this rule. Existing incidents are kept.</DialogDescription>
        </DialogHeader>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="destructive" onClick={handleDelete} disabled={deleteRule.isPending}>
            {deleteRule.isPending ? "Deleting…" : "Delete rule"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function conditionSummary(rule: Rule): string {
  const conditions = rule.conditions ?? [];
  if (conditions.length === 0) return "any event";
  return conditions
    .map((c) => `${c.field} ${c.operator}${operatorNeedsValue(c.operator) ? ` ${c.value ?? ""}` : ""}`)
    .join(" · ");
}

function RulesCard() {
  const { data: rules, isLoading } = useRules();

  return (
    <Card>
      <CardHeader className="pb-4 flex flex-row items-start justify-between space-y-0">
        <div className="space-y-1.5">
          <CardTitle className="text-lg">Rules</CardTitle>
          <CardDescription>
            Evaluated top to bottom by priority; the first matching rule creates the incident.
          </CardDescription>
        </div>
        <RuleDialog
          trigger={
            <Button>
              <Plus className="h-4 w-4" /> Add rule
            </Button>
          }
        />
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-24 w-full" />
        ) : (rules ?? []).length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            No rules yet. Add one to turn matching webhook events into incidents.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-16">Priority</TableHead>
                <TableHead>Name</TableHead>
                <TableHead className="w-28">Source</TableHead>
                <TableHead>Match</TableHead>
                <TableHead className="w-20">Severity</TableHead>
                <TableHead className="w-24">Status</TableHead>
                <TableHead className="w-24"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(rules ?? []).map((rule) => (
                <TableRow key={rule.id}>
                  <TableCell className="text-sm text-muted-foreground">{rule.priority}</TableCell>
                  <TableCell className="font-medium">{rule.name}</TableCell>
                  <TableCell className="font-mono text-xs">{rule.source ?? "any"}</TableCell>
                  <TableCell className="max-w-xs truncate font-mono text-xs text-muted-foreground" title={conditionSummary(rule)}>
                    {conditionSummary(rule)}
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{rule.severity}</Badge>
                  </TableCell>
                  <TableCell>
                    {rule.enabled ? (
                      <Badge variant="outline" className="text-green-700 border-green-300">enabled</Badge>
                    ) : (
                      <Badge variant="outline" className="text-muted-foreground">disabled</Badge>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center justify-end">
                      <RuleDialog
                        existing={rule}
                        trigger={
                          <Button variant="ghost" size="icon" className="h-8 w-8" title="Edit rule">
                            <Pencil className="h-4 w-4" />
                          </Button>
                        }
                      />
                      <DeleteRuleDialog rule={rule} />
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

function HelpCard() {
  return (
    <Card>
      <CardHeader className="pb-4">
        <CardTitle className="text-lg flex items-center gap-2">
          <GitBranch className="h-4 w-4" /> Writing rules
        </CardTitle>
        <CardDescription>Field paths and templates both use the same dotted notation.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-2 text-sm text-muted-foreground">
        <p>
          The event is exposed as <code className="font-mono text-xs">source</code> (the slug),{" "}
          <code className="font-mono text-xs">eventType</code> (normalised type, e.g.{" "}
          <code className="font-mono text-xs">ci_failure</code>), and <code className="font-mono text-xs">payload</code>{" "}
          (the raw webhook body). Address nested fields with dots, e.g.{" "}
          <code className="font-mono text-xs">payload.workflow_run.conclusion</code>; arrays by index, e.g.{" "}
          <code className="font-mono text-xs">payload.commits.0.id</code>.
        </p>
        <p>
          Titles, descriptions and the dedup key render <code className="font-mono text-xs">{"{{path}}"}</code>{" "}
          placeholders; a missing path renders empty. Inspect a real payload under{" "}
          <span className="font-medium">Sources → Received events</span> to find the field names you need.
        </p>
      </CardContent>
    </Card>
  );
}

export default function RulesPage() {
  return (
    <div className="flex flex-col h-full">
      <header className="border-b bg-white px-6 py-4">
        <h1 className="text-xl font-semibold">Rules</h1>
        <p className="text-sm text-muted-foreground">
          Decide which webhook events become incidents, and what those incidents say
        </p>
      </header>

      <div className="flex-1 overflow-y-auto p-6">
        <div className="max-w-5xl space-y-6">
          <RulesCard />
          <HelpCard />
        </div>
      </div>
    </div>
  );
}
