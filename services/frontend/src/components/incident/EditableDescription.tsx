import { useState } from "react";
import { Pencil } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useUpdateIncidentDescription, type Incident } from "@/api/queries";

/**
 * Inline-editable incident description shown in the detail page header.
 * Saving an empty textarea clears the description.
 */
export function EditableDescription({ incident }: { incident: Incident }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState("");
  const updateDescription = useUpdateIncidentDescription(incident.id);

  const startEditing = () => {
    setDraft(incident.description ?? "");
    setEditing(true);
  };

  const save = () => {
    updateDescription.mutate(
      { description: draft.trim() },
      { onSuccess: () => setEditing(false) },
    );
  };

  if (!editing) {
    return (
      <div className="group mt-2 flex items-start gap-2">
        {incident.description ? (
          <p className="text-sm text-muted-foreground whitespace-pre-wrap">{incident.description}</p>
        ) : (
          <p className="text-sm text-muted-foreground italic">No description</p>
        )}
        <button
          type="button"
          onClick={startEditing}
          aria-label="Edit description"
          className="opacity-0 group-hover:opacity-100 focus-visible:opacity-100 transition-opacity text-muted-foreground hover:text-foreground shrink-0 mt-0.5"
        >
          <Pencil className="h-3.5 w-3.5" />
        </button>
      </div>
    );
  }

  return (
    <div className="mt-2 space-y-2">
      <Textarea
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        placeholder="Describe the incident…"
        rows={3}
        maxLength={4000}
        autoFocus
      />
      <div className="flex items-center gap-2">
        <Button size="sm" onClick={save} disabled={updateDescription.isPending}>
          {updateDescription.isPending ? "Saving…" : "Save"}
        </Button>
        <Button size="sm" variant="ghost" onClick={() => setEditing(false)} disabled={updateDescription.isPending}>
          Cancel
        </Button>
        {updateDescription.isError && <p className="text-sm text-destructive">Failed to save description.</p>}
      </div>
    </div>
  );
}
