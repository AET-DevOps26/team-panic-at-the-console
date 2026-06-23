import { NavLink } from "react-router-dom";
import { AlertTriangle, Settings, Webhook, LayoutDashboard, LogOut } from "lucide-react";
import { cn } from "@/lib/utils";
import { Separator } from "@/components/ui/separator";

const linkedNavItems = [{ to: "/incidents", label: "Incidents", icon: AlertTriangle }];

const disabledNavItems = [
  { label: "Sources", icon: Webhook },
  { label: "Settings", icon: Settings },
];

export default function Sidebar() {
  return (
    <aside className="flex h-full w-60 flex-col bg-slate-900 text-slate-100">
      {/* Brand */}
      <div className="flex items-center gap-2.5 px-5 py-4 border-b border-slate-700">
        <div className="flex h-8 w-8 items-center justify-center rounded-md bg-red-600">
          <LayoutDashboard className="h-4 w-4 text-white" />
        </div>
        <div>
          <p className="text-sm font-semibold leading-tight">Incident Platform</p>
          <p className="text-xs text-slate-400 leading-tight">Operations</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        <p className="px-2 pb-1 text-xs font-semibold uppercase tracking-wider text-slate-500">Main</p>
        {linkedNavItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn("flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors", isActive ? "bg-slate-700 text-white" : "text-slate-300 hover:bg-slate-800 hover:text-white")
            }
          >
            <Icon className="h-4 w-4 shrink-0" />
            {label}
          </NavLink>
        ))}
        {disabledNavItems.map(({ label, icon: Icon }) => (
          <button
            key={label}
            disabled
            className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-slate-300 opacity-40 cursor-not-allowed"
          >
            <Icon className="h-4 w-4 shrink-0" />
            {label}
          </button>
        ))}
      </nav>

      {/* Bottom: user area */}
      <Separator className="bg-slate-700" />
      <div className="px-3 py-3">
        <button className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm text-slate-300 hover:bg-slate-800 hover:text-white transition-colors">
          <LogOut className="h-4 w-4" />
          Sign out
        </button>
      </div>
    </aside>
  );
}
