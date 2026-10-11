"""Generación de la rutina semanal con el LLM (LOGIC.md §7).

Pide JSON al modelo, lo valida con Pydantic y, si no es válido, reintenta UNA vez más (2
intentos en total — antes eran 3: con proveedores de cupo de salida pequeño, como Groq en
capa gratuita, cada intento pide de nuevo una rutina entera y el tercero ya no cabía en el
cupo por minuto, ver docs/PROGRESS.md) devolviendo al modelo el motivo exacto, corto, por el
que la respuesta anterior no sirve. Un fallo al *hablar* con el proveedor (red, 401, 429...)
se reintenta con `app/llm_retry.py` (con respaldo opcional) antes de darse por vencido: un 429
que agota los reintentos se trata como "saturado" (429 también hacia el socio, mensaje
distinto de "no disponible"); cualquier otro fallo de comunicación, como "no disponible"
(503). Una respuesta que, tras el reintento de validación, sigue sin ser un plan válido se
trata como "no se pudo generar" (502). Nunca llama a un proveedor real en los tests: el
cliente se recibe como parámetro (ver `app/main.py`, que lo inyecta con `Depends`).
"""

from __future__ import annotations

import json
import logging

from pydantic import ValidationError

from app.llm_retry import call_with_retry_and_fallback, is_rate_limited
from app.schemas import Goal, Level, PlanRequest, WeeklyPlan

logger = logging.getLogger(__name__)

MAX_ATTEMPTS = 2
# Una rutina con muchos días/ejercicios ya pide varios cientos de tokens de salida; sin un
# tope, el modelo puede alargarse (notas largas, más ejercicios de la cuenta) y agotar cupos
# de salida por minuto muy pequeños (visto de verdad con Groq: límite de 1000 tokens/min en el
# modelo gratuito, y una rutina de 5 días ya pedía más de 1000 solo para el primer intento).
# Un tope fijo cortaba a mitad de JSON las rutinas de más días (visto de verdad: "Expecting
# value" al parsear, JSON incompleto) y encima gastaba el reintento sin necesidad; por eso
# escala con los días pedidos, con margen de sobra sobre lo que de verdad hace falta.
BASE_OUTPUT_TOKENS = 150
OUTPUT_TOKENS_PER_DAY = 260


def _max_output_tokens(days_per_week: int) -> int:
    return BASE_OUTPUT_TOKENS + OUTPUT_TOKENS_PER_DAY * days_per_week

GOAL_ES = {
    Goal.HYPERTROPHY: "ganar volumen muscular (hipertrofia)",
    Goal.STRENGTH: "ganar fuerza",
    Goal.FAT_LOSS: "perder grasa",
    Goal.GENERAL: "salud y forma física general",
}

LEVEL_ES = {
    Level.BEGINNER: "principiante",
    Level.INTERMEDIATE: "intermedio",
    Level.ADVANCED: "avanzado",
}

SYSTEM_PROMPT = """Eres el entrenador personal de OlympusGYM. Diseñas rutinas de entrenamiento \
de fuerza en español sencillo, sin jerga técnica, para socios reales de un gimnasio.

Devuelve SIEMPRE y ÚNICAMENTE un JSON (sin texto antes ni después, sin bloques de código) con \
esta forma exacta:
{
  "summary": "resumen breve del enfoque de la semana, en español llano",
  "days": [
    {
      "day": 1,
      "focus": "qué se entrena ese día, en español llano",
      "exercises": [
        {
          "name": "nombre del ejercicio",
          "sets": 3,
          "reps": "8-12",
          "rest_seconds": 90,
          "notes": "indicación breve opcional, o null"
        }
      ]
    }
  ]
}

Reglas:
- Exactamente un día por cada día de entrenamiento pedido, numerados 1, 2, 3... sin repetir número.
- Usa solo ejercicios que se puedan hacer con el material disponible indicado.
- Si hay limitaciones o lesiones, evita o adapta los ejercicios que las agraven; dilo en "notes" \
cuando sustituyas algo por eso.
- El volumen (series y ejercicios por día) debe ser razonable para el nivel indicado: menos y más \
simple para principiante, más para avanzado.
- "notes": como mucho una frase muy corta, o null si no aporta nada; nunca un párrafo.
- Texto siempre en español llano: nada de jerga sin explicar ni palabras sueltas en inglés. Sin \
texto de relleno: solo lo que pide el JSON.
"""


class LlmUnavailableError(Exception):
    """El proveedor de LLM no respondió (red, autenticación, lo que sea). El cliente ve un 503."""


class LlmRateLimitedError(LlmUnavailableError):
    """429 agotando los reintentos (y el respaldo, si lo hay): no es que el proveedor esté
    caído, es que hay que esperar un poco. El cliente ve un 429, no un 503."""


class LlmInvalidOutputError(Exception):
    """Tras los reintentos, el modelo no devolvió un plan válido. El cliente ve un 502."""


def _user_prompt(request: PlanRequest) -> str:
    equipment = ", ".join(request.equipment) if request.equipment else "solo peso corporal"
    limitations = ", ".join(request.limitations) if request.limitations else "ninguna"
    return (
        f"Objetivo: {GOAL_ES[request.goal]}.\n"
        f"Nivel: {LEVEL_ES[request.level]}.\n"
        f"Días de entrenamiento a la semana: {request.days_per_week}.\n"
        f"Material disponible: {equipment}.\n"
        f"Limitaciones o lesiones: {limitations}."
    )


def _build_messages(request: PlanRequest, previous_error: str | None) -> list[dict[str, str]]:
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": _user_prompt(request)},
    ]
    if previous_error:
        messages.append(
            {
                "role": "user",
                "content": (
                    f"Tu respuesta anterior no es válida: {previous_error}. Corrígelo y "
                    "devuelve de nuevo solo el JSON completo, con la forma exacta pedida."
                ),
            }
        )
    return messages


def _extract_json(content: str) -> dict:
    text = content.strip()
    if text.startswith("```"):
        # Pese a las instrucciones, algunos modelos envuelven el JSON en un bloque de código.
        text = text.strip("`")
        if text[:4].lower() == "json":
            text = text[4:]
        text = text.strip()
    return json.loads(text)


def _parse_and_validate(content: str, request: PlanRequest) -> WeeklyPlan:
    data = _extract_json(content)
    plan = WeeklyPlan.model_validate(data)
    if len(plan.days) != request.days_per_week:
        raise ValueError(
            f"Se pidieron {request.days_per_week} días de entrenamiento y el plan tiene "
            f"{len(plan.days)}."
        )
    if len({day.day for day in plan.days}) != len(plan.days):
        raise ValueError("Hay días repetidos en el plan.")
    return plan


def generate_plan(client, model: str, request: PlanRequest) -> WeeklyPlan:
    previous_error: str | None = None

    for attempt in range(1, MAX_ATTEMPTS + 1):
        messages = _build_messages(request, previous_error)
        try:
            response = call_with_retry_and_fallback(
                client, model, messages=messages, temperature=0.4, max_tokens=_max_output_tokens(request.days_per_week)
            )
        except Exception as exc:  # cualquier fallo al hablar con el proveedor: red, 401, 429...
            if is_rate_limited(exc):
                raise LlmRateLimitedError(str(exc)) from exc
            raise LlmUnavailableError(str(exc)) from exc

        content = response.choices[0].message.content or ""
        try:
            return _parse_and_validate(content, request)
        except (json.JSONDecodeError, ValidationError, ValueError) as exc:
            previous_error = str(exc)
            logger.warning("Intento %s/%s: plan no válido (%s)", attempt, MAX_ATTEMPTS, previous_error)

    raise LlmInvalidOutputError(previous_error or "El modelo no devolvió un plan válido.")
