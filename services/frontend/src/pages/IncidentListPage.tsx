import { useState } from "react";
import { Link } from "react-router-dom";
import { Plus, Search, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { SeverityBadge, StatusBadge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useIncidents, useCreateIncident, type Incident, type IncidentStatus, type Severity } from "@/api/queries";
import { formatRelativeTime } from "@/lib/utils";


function CreateIncidentDialog() {
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [severity, setSeverity] = useState<Severity>("SEV3");
  const createIncident = useCreateIncident();

  async function handleCreate() {
    await createIncident.mutateAsync({ title, description: description || null, severity });
    setOpen(false);
    setTitle("");
    setDescription("");
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>
          <Plus className="h-4 w-4" />
          New Incident
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Create Incident</DialogTitle>
          <DialogDescription>Manually open a new incident. It will start in Open state.</DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-2">
            <Label htmlFor="title">Title *</Label>
            <Input id="title" placeholder="Brief description of the issue" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="desc">Description</Label>
            <Textarea id="desc" placeholder="Additional context (optional)" value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label>Initial severity</Label>
            <Select value={severity} onValueChange={(v) => setSeverity(v as Severity)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="SEV1">SEV1 — Critical</SelectItem>
                <SelectItem value="SEV2">SEV2 — High</SelectItem>
                <SelectItem value="SEV3">SEV3 — Medium</SelectItem>
                <SelectItem value="SEV4">SEV4 — Low</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button onClick={handleCreate} disabled={!title.trim() || createIncident.isPending}>
            {createIncident.isPending ? "Creating…" : "Create"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default function IncidentListPage() {
  const { data: incidents, isLoading, refetch } = useIncidents();
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<IncidentStatus | "all">("all");
  const [severityFilter, setSeverityFilter] = useState<Severity | "all">("all");

  const rows = (incidents ?? []).filter((inc) => {
    const matchesSearch = inc.title.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === "all" || inc.status === statusFilter;
    const matchesSeverity = severityFilter === "all" || inc.severity === severityFilter;
    return matchesSearch && matchesStatus && matchesSeverity;
  });

  return (
    <div className="flex flex-col h-full">
      {/* Page header */}
      <header className="border-b bg-white px-6 py-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">Incidents</h1>
          <p className="text-sm text-muted-foreground">Track and manage active incidents</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => refetch()} title="Refresh">
            <RefreshCw className="h-4 w-4" />
          </Button>
          <CreateIncidentDialog />
        </div>
      </header>

      {/* Filters */}
      <div className="border-b bg-white px-6 py-3 flex items-center gap-3">
        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input className="pl-9" placeholder="Search incidents…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <Select value={statusFilter} onValueChange={(v) => setStatusFilter(v as IncidentStatus | "all")}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder="All statuses" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            <SelectItem value="open">Open</SelectItem>
            <SelectItem value="investigating">Investigating</SelectItem>
            <SelectItem value="resolved">Resolved</SelectItem>
          </SelectContent>
        </Select>
        <Select value={severityFilter} onValueChange={(v) => setSeverityFilter(v as Severity | "all")}>
          <SelectTrigger className="w-36">
            <SelectValue placeholder="All severities" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All severities</SelectItem>
            <SelectItem value="SEV1">SEV1</SelectItem>
            <SelectItem value="SEV2">SEV2</SelectItem>
            <SelectItem value="SEV3">SEV3</SelectItem>
            <SelectItem value="SEV4">SEV4</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Table */}
      <div className="flex-1 overflow-auto bg-white">
        {isLoading ? (
          <div className="p-6 space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Title</TableHead>
                <TableHead className="w-24">Severity</TableHead>
                <TableHead className="w-36">Status</TableHead>
                <TableHead className="w-36">Created</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} className="text-center py-12 text-muted-foreground">
                    No incidents match your filters.
                  </TableCell>
                </TableRow>
              ) : (
                rows.map((incident) => (
                  <TableRow key={incident.id} className="cursor-pointer">
                    <TableCell>
                      <Link to={`/incidents/${incident.id}`} className="font-medium hover:underline">
                        {incident.title}
                      </Link>
                      {incident.description && <p className="text-xs text-muted-foreground mt-0.5 line-clamp-1">{incident.description}</p>}
                    </TableCell>
                    <TableCell>
                      <SeverityBadge severity={incident.severity} />
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={incident.status} />
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm">{formatRelativeTime(incident.createdAt)}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        )}
      </div>

    </div>
  );
}
