import { Navigate, Outlet } from "react-router-dom";
import { useCurrentUser } from "@/api/queries";

/**
 * Layout route that gates its children on a valid session: while the session
 * check runs it renders a quiet placeholder, and a 401 redirects to /login.
 */
export default function RequireAuth() {
  const { data: user, isPending } = useCurrentUser();

  if (isPending) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <p className="text-sm text-muted-foreground">Checking session…</p>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
