import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function NotFoundPage({ title = "Page not found", description = "The page you're looking for doesn't exist.", className }: { title?: string; description?: string; className?: string }) {
  return (
    <div className={cn("flex min-h-screen flex-col items-center justify-center gap-4 text-center", className)}>
      <p className="text-6xl font-bold text-muted-foreground/30">404</p>
      <h1 className="text-2xl font-semibold">{title}</h1>
      <p className="text-muted-foreground">{description}</p>
      <Button asChild>
        <Link to="/incidents">Back to Incidents</Link>
      </Button>
    </div>
  );
}
