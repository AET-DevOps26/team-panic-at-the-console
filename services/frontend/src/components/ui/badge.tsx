import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";
import type { components } from "@/api/schema";

const badgeVariants = cva("inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2", {
  variants: {
    variant: {
      default: "border-transparent bg-primary text-primary-foreground hover:bg-primary/80",
      secondary: "border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80",
      destructive: "border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80",
      outline: "text-foreground",
      sev1: "border-transparent bg-red-600 text-white",
      sev2: "border-transparent bg-orange-500 text-white",
      sev3: "border-transparent bg-yellow-500 text-white",
      sev4: "border-transparent bg-green-600 text-white",
      open: "border-transparent bg-blue-600 text-white",
      investigating: "border-transparent bg-yellow-600 text-white",
      resolved: "border-transparent bg-green-600 text-white",
    },
  },
  defaultVariants: {
    variant: "default",
  },
});

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}

type Severity = components["schemas"]["Severity"];
type IncidentStatus = components["schemas"]["IncidentStatus"];

export function SeverityBadge({ severity }: { severity: Severity }) {
  const variantMap: Record<Severity, "sev1" | "sev2" | "sev3" | "sev4"> = {
    SEV1: "sev1",
    SEV2: "sev2",
    SEV3: "sev3",
    SEV4: "sev4",
  };
  return <Badge variant={variantMap[severity]}>{severity}</Badge>;
}

export function StatusBadge({ status }: { status: IncidentStatus }) {
  const variantMap: Record<IncidentStatus, "open" | "investigating" | "resolved"> = {
    open: "open",
    investigating: "investigating",
    resolved: "resolved",
  };
  const label: Record<IncidentStatus, string> = {
    open: "Open",
    investigating: "Investigating",
    resolved: "Resolved",
  };
  return <Badge variant={variantMap[status]}>{label[status]}</Badge>;
}

export { Badge, badgeVariants };
