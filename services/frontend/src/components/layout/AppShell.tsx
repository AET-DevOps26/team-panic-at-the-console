import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import { useIncidentStream } from "@/api/stream";

export default function AppShell() {
  useIncidentStream();
  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar />
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
}
