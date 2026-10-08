"""Cliente de LLM falso para los tests: imita solo la parte del SDK de OpenAI que se usa
(`client.chat.completions.create(...).choices[0].message.content`), sin red real.
"""

from __future__ import annotations

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
