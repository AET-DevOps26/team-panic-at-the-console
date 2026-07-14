import { useNavigate } from "react-router-dom";
import { Bell } from "lucide-react";
import { useIncidentTitles, useMarkAllNotificationsRead, useMarkNotificationRead, useNotifications } from "@/api/queries";
import { formatRelativeTime } from "@/lib/utils";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import type { Notification } from "@/api/queries";

const RECENT_COUNT = 10;

export function NotificationBell() {
  const navigate = useNavigate();
  const { data } = useNotifications({ size: RECENT_COUNT });
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllNotificationsRead();

  const unreadCount = data?.unreadCount ?? 0;
  const items = data?.items ?? [];

  // INCIDENT_CREATED messages already contain the title, so no lookup needed.
  const incidentTitles = useIncidentTitles(
    items.filter((n) => n.type !== "INCIDENT_CREATED").map((n) => n.incidentId),
  );

  function openNotification(notification: Notification) {
    // Fire-and-forget: navigation should not wait on the read mark.
    if (!notification.read) markRead.mutate(notification.id);
    navigate(`/incidents/${notification.incidentId}`);
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          aria-label={unreadCount > 0 ? `Notifications (${unreadCount} unread)` : "Notifications"}
          className="relative rounded-md p-2 text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
        >
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold leading-none text-white">
              {unreadCount > 9 ? "9+" : unreadCount}
            </span>
          )}
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        <div className="flex items-center justify-between px-2 py-1.5">
          <span className="text-sm font-semibold">Notifications</span>
          <button
            onClick={() => markAllRead.mutate()}
            disabled={markAllRead.isPending || unreadCount === 0}
            className="text-xs text-muted-foreground transition-colors hover:text-foreground disabled:pointer-events-none disabled:opacity-50"
          >
            Mark all read
          </button>
        </div>
        <DropdownMenuSeparator />
        {items.length === 0 ? (
          <p className="px-2 py-6 text-center text-sm text-muted-foreground">No notifications yet.</p>
        ) : (
          items.map((notification) => (
            <DropdownMenuItem key={notification.id} onSelect={() => openNotification(notification)} className="items-start gap-2 py-2">
              <span
                aria-hidden
                className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${notification.read ? "bg-transparent" : "bg-red-600"}`}
              />
              <span className="min-w-0 flex-1 space-y-0.5">
                <span className={`block text-sm leading-snug ${notification.read ? "text-muted-foreground" : "font-medium"}`}>{notification.message}</span>
                {incidentTitles[notification.incidentId] && (
                  <span className="block truncate text-xs text-muted-foreground">{incidentTitles[notification.incidentId]}</span>
                )}
                <span className="block text-xs text-muted-foreground">{formatRelativeTime(notification.createdAt)}</span>
              </span>
            </DropdownMenuItem>
          ))
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
