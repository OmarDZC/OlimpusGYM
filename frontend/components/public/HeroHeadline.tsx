"use client";

import { useEffect, useState } from "react";

const WORDS = ["OLIMPO", "HIERRO", "RÉCORD"];

/**
 * Titular del hero que cambia de palabra resaltada (Trozo 7b, DESIGN.md §6). Con
 * `prefers-reduced-motion` activo, el intervalo ni se programa: la palabra se queda fija en
 * la primera, sin parpadeos ni texto que cambie solo.
 */
export function HeroHeadline() {
  const [index, setIndex] = useState(0);

  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const id = setInterval(() => setIndex((i) => (i + 1) % WORDS.length), 3000);
    return () => clearInterval(id);
  }, []);

  return (
    <h1 className="font-display text-5xl font-extrabold uppercase leading-none tracking-tight sm:text-7xl">
      Forja tu propio{" "}
      <span key={index} className="inline-block text-accent-text motion-safe:animate-[word-fade-in_0.3s_ease-out]">
        {WORDS[index]}
      </span>
    </h1>
  );
}
