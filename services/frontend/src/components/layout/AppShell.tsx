import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import { NotificationBell } from "./NotificationBell";
import { useIncidentStream } from "@/api/stream";

export default function AppShell() {
  useIncidentStream();
  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar />
      <main className="flex flex-1 flex-col overflow-hidden">
        <div className="flex items-center justify-end border-b px-4 py-1.5">
          <NotificationBell />
        </div>
        <div className="flex-1 overflow-y-auto">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
