import { useHealth } from "@/api/queries";
import { cn } from "@/lib/utils";

/** Small dot + label reflecting the API gateway health check. */
export function GatewayStatus() {
  const { isPending, isError } = useHealth();

  const label = isPending ? "Checking API…" : isError ? "API unreachable" : "API connected";
  const dot = isPending ? "animate-pulse bg-slate-500" : isError ? "bg-red-500" : "bg-emerald-500";

  return (
    <p role="status" className="flex items-center gap-2">
      <span aria-hidden className={cn("h-2 w-2 shrink-0 rounded-full", dot)} />
      {label}
    </p>
  );
}
