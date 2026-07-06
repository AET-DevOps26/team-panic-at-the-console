import ReactMarkdown from "react-markdown";
import remarkBreaks from "remark-breaks";
import remarkGfm from "remark-gfm";
import { cn } from "@/lib/utils";

const block = "mt-2 first:mt-0";

// Renders LLM output as GitHub-flavored markdown, sized to match the text-sm
// panels it sits in. remark-breaks keeps single newlines visible as line
// breaks, which the previous whitespace-pre-line rendering relied on. Headings
// are flattened to one compact size: LLMs pick arbitrary levels, and inside a
// small panel h1 vs h3 is noise.
export function Markdown({ children, className }: { children: string; className?: string }) {
  const heading = ({ node, ...props }: React.ComponentProps<"h3"> & { node?: unknown }) => <h3 className={cn(block, "mt-3 text-sm font-semibold")} {...props} />;
  return (
    <div className={cn("text-sm leading-relaxed break-words", className)}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkBreaks]}
        components={{
          h1: heading,
          h2: heading,
          h3: heading,
          h4: heading,
          h5: heading,
          h6: heading,
          p: ({ node, ...props }) => <p className={block} {...props} />,
          ul: ({ node, ...props }) => <ul className={cn(block, "list-disc pl-5 space-y-1")} {...props} />,
          ol: ({ node, ...props }) => <ol className={cn(block, "list-decimal pl-5 space-y-1")} {...props} />,
          a: ({ node, ...props }) => <a className="underline underline-offset-2 hover:text-foreground" target="_blank" rel="noreferrer" {...props} />,
          pre: ({ node, ...props }) => <pre className={cn(block, "overflow-x-auto rounded bg-muted p-3 text-xs")} {...props} />,
          code: ({ node, ...props }) => <code className="rounded bg-muted px-1 py-0.5 font-mono text-[0.85em]" {...props} />,
          blockquote: ({ node, ...props }) => <blockquote className={cn(block, "border-l-2 pl-3 italic text-muted-foreground")} {...props} />,
          table: ({ node, ...props }) => <table className={cn(block, "w-full border-collapse text-xs")} {...props} />,
          th: ({ node, ...props }) => <th className="border px-2 py-1 text-left font-medium" {...props} />,
          td: ({ node, ...props }) => <td className="border px-2 py-1" {...props} />,
          hr: ({ node, ...props }) => <hr className="my-3" {...props} />,
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  );
}
