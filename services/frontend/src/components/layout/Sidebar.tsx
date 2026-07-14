import { Link, NavLink, useNavigate } from "react-router-dom";
import { AlertTriangle, ExternalLink, Settings, Webhook, LayoutDashboard, LogOut } from "lucide-react";
import { useCurrentUser, useLogout } from "@/api/queries";
import { appConfig } from "@/lib/appConfig";
import { cn } from "@/lib/utils";
import { Separator } from "@/components/ui/separator";
import { GatewayStatus } from "@/components/layout/GatewayStatus";

const linkedNavItems = [
  { to: "/incidents", label: "Incidents", icon: AlertTriangle },
  { to: "/sources", label: "Sources", icon: Webhook },
  { to: "/settings", label: "Settings", icon: Settings },
];

export default function Sidebar() {
  const navigate = useNavigate();
  const { data: user } = useCurrentUser();
  const logout = useLogout();

  async function handleSignOut() {
    try {
      await logout.mutateAsync();
    } finally {
      // The cookie is httpOnly, so the only sensible recovery from a failed
      // logout call is still to leave the app.
      navigate("/login", { replace: true });
    }
  }

  return (
    <aside className="flex h-full w-60 flex-col bg-slate-900 text-slate-100">
      {/* Brand */}
      <Link to="/" className="flex items-center gap-2.5 px-5 py-4 border-b border-slate-700 transition-colors hover:bg-slate-800">
        <div className="flex h-8 w-8 items-center justify-center rounded-md bg-red-600">
          <LayoutDashboard className="h-4 w-4 text-white" />
        </div>
        <div>
          <p className="text-sm font-semibold leading-tight">Incident Platform</p>
          <p className="text-xs text-slate-400 leading-tight">Operations</p>
        </div>
      </Link>

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
      </nav>

      {/* Bottom: user area */}
      <Separator className="bg-slate-700" />
      <div className="px-3 py-3 space-y-1">
        {user && (
          <div className="px-3 py-1">
            <p className="text-sm font-medium text-slate-100 truncate">{user.displayName}</p>
            <p className="text-xs text-slate-400 truncate">{user.email}</p>
          </div>
        )}
        <button
          onClick={handleSignOut}
          disabled={logout.isPending}
          className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm text-slate-300 hover:bg-slate-800 hover:text-white transition-colors disabled:opacity-50"
        >
          <LogOut className="h-4 w-4" />
          {logout.isPending ? "Signing out…" : "Sign out"}
        </button>
      </div>
      <Separator className="bg-slate-700" />

      {/* Deployment info */}
      <div className="px-6 py-3 space-y-1 text-xs text-slate-500">
        <GatewayStatus />
        {(appConfig.prometheusUrl || appConfig.grafanaUrl) && (
          <div className="flex items-center gap-3">
            {appConfig.prometheusUrl && (
              <a href={appConfig.prometheusUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 transition-colors hover:text-slate-300">
                Prometheus
                <ExternalLink className="h-3 w-3" />
              </a>
            )}
            {appConfig.grafanaUrl && (
              <a href={appConfig.grafanaUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 transition-colors hover:text-slate-300">
                Grafana
                <ExternalLink className="h-3 w-3" />
              </a>
            )}
          </div>
        )}
        {appConfig.commitUrl ? (
          <a
            href={appConfig.commitUrl}
            target="_blank"
            rel="noreferrer"
            className="font-mono transition-colors hover:text-slate-300"
            title={`View commit ${appConfig.commitSha} on GitHub`}
          >
            build {appConfig.commitSha.slice(0, 7)}
          </a>
        ) : (
          <p className="font-mono" title={`Deployed from commit ${appConfig.commitSha}`}>
            build {appConfig.commitSha.slice(0, 7)}
          </p>
        )}
      </div>
    </aside>
  );
}
