"""Clientes falsos para los tests: imitan solo la parte del SDK de OpenAI y del backend de
GymFlow que se usa de verdad, sin red real.
"""

from __future__ import annotations

import json
from collections.abc import Callable


class _FakeMessage:
    def __init__(self, content: str) -> None:
        self.content = content


class _FakeChoice:
    def __init__(self, content: str) -> None:
        self.message = _FakeMessage(content)


class _FakeCompletion:
    def __init__(self, content: str) -> None:
        self.choices = [_FakeChoice(content)]


class _FakeCompletions:
    def __init__(self, respond: Callable[[int], str]) -> None:
        self._respond = respond
        self.calls = 0

    def create(self, **_kwargs):
        self.calls += 1
        return _FakeCompletion(self._respond(self.calls))


class _FakeChat:
    def __init__(self, completions: _FakeCompletions) -> None:
        self.completions = completions


class FakeOpenAIClient:
    """`respond(call_number) -> str`: contenido que "dice" el modelo en esa llamada (1, 2, 3...)."""

    def __init__(self, respond: Callable[[int], str]) -> None:
        self.chat = _FakeChat(_FakeCompletions(respond))

    @property
    def calls(self) -> int:
        return self.chat.completions.calls


class FailingOpenAIClient:
    """Simula que no se puede hablar con el proveedor (red caída, 401, timeout...)."""

    class _Completions:
        def create(self, **_kwargs):
            raise ConnectionError("no se pudo conectar con el proveedor de LLM")

    class _Chat:
        def __init__(self) -> None:
            self.completions = FailingOpenAIClient._Completions()

    def __init__(self) -> None:
        self.chat = FailingOpenAIClient._Chat()


# ---- Tool calling con guion, para el asistente (Trozo 6) ----


class _FakeToolCallFunction:
    def __init__(self, name: str, arguments: dict) -> None:
        self.name = name
        self.arguments = json.dumps(arguments, ensure_ascii=False)


class _FakeToolCall:
    def __init__(self, call_id: str, name: str, arguments: dict) -> None:
        self.id = call_id
        self.type = "function"
        self.function = _FakeToolCallFunction(name, arguments)


class _FakeAssistantMessage:
    def __init__(self, content: str | None, tool_calls: list[_FakeToolCall] | None) -> None:
        self.content = content
        self.tool_calls = tool_calls


def text(content: str) -> tuple[str, str]:
    """Una respuesta final del modelo: sin más llamadas a herramientas en ese turno."""
    return ("text", content)


def tool_call(name: str, **arguments) -> tuple[str, list[tuple[str, dict]]]:
    """Una respuesta del modelo que llama a una única herramienta."""
    return ("tool_calls", [(name, arguments)])


class _ScriptedCompletions:
    def __init__(self, outer: "ScriptedToolCallingClient") -> None:
        self._outer = outer

    def create(self, **_kwargs):
        return self._outer._next()


class _ScriptedChat:
    def __init__(self, outer: "ScriptedToolCallingClient") -> None:
        self.completions = _ScriptedCompletions(outer)


class ScriptedToolCallingClient:
    """Cliente falso con una respuesta ya escrita por llamada: `text(...)` o `tool_call(...)`.

    Simula el ida y vuelta del tool calling sin hablar nunca con un proveedor real.
    """

    def __init__(self, script: list[tuple]) -> None:
        self._script = script
        self.calls = 0
        self.chat = _ScriptedChat(self)

    def _next(self):
        if self.calls >= len(self._script):
            raise AssertionError("El guion del LLM falso se quedó corto para este test")
        kind, payload = self._script[self.calls]
        self.calls += 1
        if kind == "text":
            message = _FakeAssistantMessage(content=payload, tool_calls=None)
        else:
            tool_calls = [_FakeToolCall(f"call_{i}", name, args) for i, (name, args) in enumerate(payload)]
            message = _FakeAssistantMessage(content=None, tool_calls=tool_calls)
        choice = type("Choice", (), {"message": message})()
        return type("Completion", (), {"choices": [choice]})()


class FakeGymflowClient:
    """Imita `app.gymflow_client.GymflowClient` en memoria: sin red, con los datos que le des."""

    def __init__(self, sessions: list[dict] | None = None, bookings_by_token: dict[str, list[dict]] | None = None) -> None:
        self.sessions = sessions or []
        self.bookings_by_token: dict[str, list[dict]] = bookings_by_token or {}
        self.book_calls: list[tuple[str, str]] = []
        self.cancel_calls: list[tuple[str, str]] = []

    def list_schedule(self, discipline: str | None = None) -> list[dict]:
        if discipline:
            return [s for s in self.sessions if s.get("discipline") == discipline]
        return list(self.sessions)

    def my_bookings(self, member_token: str) -> list[dict]:
        return list(self.bookings_by_token.get(member_token, []))

    def book_session(self, member_token: str, session_id: str) -> None:
        self.book_calls.append((member_token, session_id))
        session = next((s for s in self.sessions if s.get("id") == session_id), None)
        bookings = self.bookings_by_token.setdefault(member_token, [])
        bookings.append(
            {
                "id": f"booking-{session_id}",
                "sessionId": session_id,
                "sessionName": session.get("name") if session else "Clase",
                "startsAt": session.get("startsAt") if session else None,
                "status": "BOOKED",
                "waitlistPosition": None,
            }
        )

    def cancel_booking(self, member_token: str, booking_id: str) -> None:
        self.cancel_calls.append((member_token, booking_id))
        bookings = self.bookings_by_token.get(member_token, [])
        self.bookings_by_token[member_token] = [b for b in bookings if b.get("id") != booking_id]
