import type { ComponentPropsWithRef } from "react";
import { cn } from "@/lib/cn";

export function Textarea({ className, ...props }: ComponentPropsWithRef<"textarea">) {
  return (
    <textarea
      className={cn(
        "w-full rounded-control border border-border bg-surface-2 px-3.5 py-2.5 text-sm text-text placeholder:text-text-dim",
        className,
      )}
      {...props}
    />
  );
}
