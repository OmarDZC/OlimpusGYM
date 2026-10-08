"use client";

import Link from "next/link";
import { ChevronDown } from "lucide-react";
import { useEffect, useId, useRef, useState, type KeyboardEvent } from "react";
import { cn } from "@/lib/cn";

export type DropdownItem = {
  label: string;
  href: string;
};

/**
 * Menú desplegable accesible: se abre con clic o teclado (Enter/flecha abajo),
 * se navega con flechas arriba/abajo, Inicio/Fin, y se cierra con Escape o
 * al perder el foco. Ver DESIGN.md §4: "accesibles con teclado y con clic".
 */
export function Dropdown({ label, items }: { label: string; items: DropdownItem[] }) {
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const rootRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const itemRefs = useRef<Array<HTMLAnchorElement | null>>([]);
  const menuId = useId();

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
        setActiveIndex(-1);
      }
    }
    document.addEventListener("mousedown", onClickOutside);
    return () => document.removeEventListener("mousedown", onClickOutside);
  }, []);

  useEffect(() => {
    if (open && activeIndex >= 0) {
      itemRefs.current[activeIndex]?.focus();
    }
  }, [open, activeIndex]);

  function openMenu(focusFirst: boolean) {
    setOpen(true);
    setActiveIndex(focusFirst ? 0 : items.length - 1);
  }

  function closeMenu(returnFocus = false) {
    setOpen(false);
    setActiveIndex(-1);
    if (returnFocus) buttonRef.current?.focus();
  }

  function onButtonKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === "ArrowDown" || event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      openMenu(true);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      openMenu(false);
    }
  }

  function onMenuKeyDown(event: KeyboardEvent<HTMLUListElement>) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex((i) => (i + 1) % items.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((i) => (i - 1 + items.length) % items.length);
    } else if (event.key === "Home") {
      event.preventDefault();
      setActiveIndex(0);
    } else if (event.key === "End") {
      event.preventDefault();
      setActiveIndex(items.length - 1);
    } else if (event.key === "Escape") {
      event.preventDefault();
      closeMenu(true);
    } else if (event.key === "Tab") {
      closeMenu();
    }
  }

  return (
    <div className="relative" ref={rootRef}>
      <button
        ref={buttonRef}
        type="button"
        aria-haspopup="true"
        aria-expanded={open}
        aria-controls={menuId}
        onClick={() => (open ? closeMenu() : openMenu(true))}
        onKeyDown={onButtonKeyDown}
        className="flex items-center gap-1 rounded-control px-3 py-2 text-sm font-medium text-text-muted transition-colors hover:text-text"
      >
        {label}
        <ChevronDown
          size={16}
          aria-hidden="true"
          className={cn("transition-transform", open && "rotate-180")}
        />
      </button>

      {open && (
        <ul
          id={menuId}
          role="menu"
          aria-label={label}
          onKeyDown={onMenuKeyDown}
          className="absolute left-0 top-full z-50 mt-2 w-56 rounded-card border border-border bg-surface p-1.5"
        >
          {items.map((item, index) => (
            <li key={item.href} role="none">
              <Link
                href={item.href}
                role="menuitem"
                tabIndex={-1}
                ref={(el) => {
                  itemRefs.current[index] = el;
                }}
                onClick={() => closeMenu()}
                className="block rounded-control px-3 py-2 text-sm text-text-muted hover:bg-surface-2 hover:text-text focus:bg-surface-2 focus:text-text focus:outline-none"
              >
                {item.label}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
