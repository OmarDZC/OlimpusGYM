"""Clientes falsos para los tests: imitan solo la parte del SDK de OpenAI y del backend de
GymFlow que se usa de verdad, sin red real.
"""

from __future__ import annotations

import json
from collections.abc import Callable

import httpx
from openai import APIConnectionError, APIStatusError


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


class _FakeProviderStatusError(Exception):
    """Imita una excepción del SDK de OpenAI con `status_code` (p. ej. 401 de una clave
    inválida), incluida una petición con cabecera de autorización colgando del propio error,
    igual que hace el SDK de verdad: así el test de logging comprueba que de verdad no se
    vuelca a los logs, no solo que no había nada que filtrar."""

    def __init__(self, message: str, status_code: int) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.request = type(
            "FakeRequest", (), {"headers": {"Authorization": "Bearer sk-totally-fake-secret-value"}}
        )()


class FailingOpenAIClientWithStatus:
    """Simula un fallo del proveedor con código HTTP conocido (p. ej. 401 de una clave inválida)."""

    class _Completions:
        def __init__(self, status_code: int, message: str) -> None:
            self._status_code = status_code
            self._message = message

        def create(self, **_kwargs):
            raise _FakeProviderStatusError(self._message, self._status_code)

    class _Chat:
        def __init__(self, status_code: int, message: str) -> None:
            self.completions = FailingOpenAIClientWithStatus._Completions(status_code, message)

    def __init__(self, status_code: int = 401, message: str = "Invalid API Key") -> None:
        self.chat = FailingOpenAIClientWithStatus._Chat(status_code, message)


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


def fail(exc: Exception) -> tuple[str, Exception]:
    """Un paso del guion en el que el proveedor falla (para probar el reintento del asistente)."""
    return ("fail", exc)


def _fake_llm_request() -> httpx.Request:
    return httpx.Request("POST", "https://fake-llm.test/chat/completions")


def make_status_error(status_code: int, message: str = "fallo del proveedor", retry_after: str | None = None):
    """Imita una excepción real del SDK de OpenAI con un `status_code` HTTP (429, 500...),
    cabeceras incluidas, para que `_is_retryable`/`_retry_after_seconds` (app/assistant.py) la
    reconozcan igual que a una de verdad."""
    headers = {"retry-after": retry_after} if retry_after else {}
    response = httpx.Response(status_code=status_code, headers=headers, request=_fake_llm_request())
    return APIStatusError(message, response=response, body=None)


def make_connection_error(message: str = "no se pudo conectar con el proveedor de LLM"):
    return APIConnectionError(message=message, request=_fake_llm_request())


class _ScriptedCompletions:
    def __init__(self, outer: "ScriptedToolCallingClient") -> None:
        self._outer = outer

    def create(self, **kwargs):
        self._outer.received_calls.append(kwargs)
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
        self.received_calls: list[dict] = []
        self.chat = _ScriptedChat(self)

    def _next(self):
        if self.calls >= len(self._script):
            raise AssertionError("El guion del LLM falso se quedó corto para este test")
        kind, payload = self._script[self.calls]
        self.calls += 1
        if kind == "fail":
            raise payload
        if kind == "text":
            message = _FakeAssistantMessage(content=payload, tool_calls=None)
        else:
            # El id incluye el número de paso (self.calls, ya incrementado) para que sea único
            # en todo el guion, no solo dentro de esta llamada: dos pasos que llaman a la misma
            # herramienta no deben acabar compartiendo tool_call_id.
            tool_calls = [
                _FakeToolCall(f"call_{self.calls}_{i}", name, args) for i, (name, args) in enumerate(payload)
            ]
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
        self.last_schedule_call: dict | None = None

    def list_schedule(
        self,
        discipline: str | None = None,
        date_from: str | None = None,
        date_to: str | None = None,
    ) -> list[dict]:
        # El filtrado por fecha de verdad lo hace el backend real (ver PublicScheduleController):
        # este doble solo registra con qué from/to se le llamó, para que los tests comprueben que
        # el servicio de IA calcula bien la ventana, sin tener que fijar las fechas de SESSION a
        # "hoy" (rompería test_format_datetime_es_en_espanol_llano_hora_de_madrid, que depende de
        # una fecha fija).
        self.last_schedule_call = {"discipline": discipline, "date_from": date_from, "date_to": date_to}
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
