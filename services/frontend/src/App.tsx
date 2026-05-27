import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import AppShell from "@/components/layout/AppShell";
import IncidentListPage from "@/pages/IncidentListPage";
import IncidentDetailPage from "@/pages/IncidentDetailPage";
import LoginPage from "@/pages/LoginPage";
import NotFoundPage from "@/pages/NotFoundPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<AppShell />}>
          <Route index element={<Navigate to="/incidents" replace />} />
          <Route path="/incidents" element={<IncidentListPage />} />
          <Route path="/incidents/:id" element={<IncidentDetailPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
