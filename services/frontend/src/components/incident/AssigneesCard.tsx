import { useState } from "react";
import { UserMinus, UserPlus } from "lucide-react";
import { useAssignIncident, useUsers, type Incident, type User } from "@/api/queries";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

/** Assigned responders with a directory-backed picker (PATCH /incidents/{id}/assign). */
export function AssigneesCard({ incident }: { incident: Incident }) {
  const { data: users } = useUsers();
  const assign = useAssignIncident(incident.id);
  const assignedIds = incident.assignedUserIds ?? [];
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState<string[]>(assignedIds);

  const byId = new Map((users ?? []).map((u) => [u.id, u]));
  const nameOf = (id: string) => byId.get(id)?.displayName ?? "Unknown user";

  function toggle(userId: string) {
    setSelected((current) =>
      current.includes(userId) ? current.filter((id) => id !== userId) : [...current, userId]
    );
  }

  async function save() {
    await assign.mutateAsync({ userIds: selected });
    setOpen(false);
  }

  async function remove(userId: string) {
    await assign.mutateAsync({ userIds: assignedIds.filter((id) => id !== userId) });
  }

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base">Assignees</CardTitle>
      </CardHeader>
      <CardContent>
        {assignedIds.length === 0 ? (
          <p className="text-sm text-muted-foreground">No assignees yet.</p>
        ) : (
          <ul className="space-y-2">
            {assignedIds.map((userId) => (
              <li key={userId} className="flex items-center justify-between gap-2 text-sm">
                <span className="truncate">{nameOf(userId)}</span>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-7 px-2 text-muted-foreground"
                  aria-label={`Unassign ${nameOf(userId)}`}
                  disabled={assign.isPending}
                  onClick={() => remove(userId)}
                >
                  <UserMinus className="h-3.5 w-3.5" />
                </Button>
              </li>
            ))}
          </ul>
        )}

        <Dialog
          open={open}
          onOpenChange={(next) => {
            setOpen(next);
            if (next) setSelected(assignedIds);
          }}
        >
          <DialogTrigger asChild>
            <Button variant="outline" size="sm" className="mt-3 w-full">
              <UserPlus className="mr-2 h-3.5 w-3.5" />
              Assign users
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-sm">
            <DialogHeader>
              <DialogTitle>Assign responders</DialogTitle>
              <DialogDescription>Select who is working on this incident.</DialogDescription>
            </DialogHeader>
            <div className="max-h-64 space-y-1 overflow-y-auto">
              {(users ?? []).map((user: User) => (
                <button
                  key={user.id}
                  type="button"
                  onClick={() => toggle(user.id)}
                  className={cn(
                    "flex w-full items-center justify-between rounded-md px-3 py-2 text-left text-sm transition-colors hover:bg-accent",
                    selected.includes(user.id) && "bg-accent"
                  )}
                >
                  <span>
                    <span className="block font-medium">{user.displayName}</span>
                    <span className="block text-xs text-muted-foreground">{user.email}</span>
                  </span>
                  {selected.includes(user.id) && <span className="text-xs font-medium text-primary">Assigned</span>}
                </button>
              ))}
              {users && users.length === 0 && (
                <p className="py-4 text-center text-sm text-muted-foreground">No users found.</p>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" size="sm" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button size="sm" onClick={save} disabled={assign.isPending}>
                {assign.isPending ? "Saving…" : "Save"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </CardContent>
    </Card>
  );
}
