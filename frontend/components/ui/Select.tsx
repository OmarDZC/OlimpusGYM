import type { SelectHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        "w-full rounded-control border border-border bg-surface-2 px-3.5 py-2.5 text-sm text-text",
        className,
      )}
      {...props}
    >
      {children}
    </select>
  );
}
