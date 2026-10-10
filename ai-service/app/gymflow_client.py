"""Cliente HTTP hacia el propio backend de GymFlow (no hacia el LLM).

Lo usan las herramientas del asistente para consultar el horario, ver reservas, reservar y
cancelar, siempre con el token del socio que está hablando — nunca con uno propio del
servicio (LOGIC.md §7: "usa el token del socio autenticado, nunca uno propio").
"""
from __future__ import annotations

import httpx

from app.config import settings

TIMEOUT = httpx.Timeout(connect=3.0, read=15.0, write=10.0, pool=3.0)


class GymflowApiError(Exception):
    """El backend respondió con un error de negocio (clase completa, reserva ya cancelada...)."""


class GymflowUnavailableError(Exception):
    """No se pudo ni hablar con el backend (red, timeout...)."""


def _auth(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _detail(response: httpx.Response) -> str:
    try:
        body = response.json()
        if isinstance(body, dict) and isinstance(body.get("detail"), str):
            return body["detail"]
    except ValueError:
        pass
    return "No se pudo completar la solicitud."


class GymflowClient:
    def __init__(self, base_url: str) -> None:
        self._http = httpx.Client(base_url=base_url, timeout=TIMEOUT)

    def list_schedule(
        self,
        discipline: str | None = None,
        date_from: str | None = None,
        date_to: str | None = None,
    ) -> list[dict]:
        params = {}
        if discipline:
            params["discipline"] = discipline
        if date_from:
            params["from"] = date_from
        if date_to:
            params["to"] = date_to
        response = self._request("GET", "/api/v1/public/schedule", params=params)
        return response.json()

    def my_bookings(self, member_token: str) -> list[dict]:
        response = self._request("GET", "/api/v1/me/bookings", headers=_auth(member_token))
        return response.json()

    def book_session(self, member_token: str, session_id: str) -> None:
        self._request(
            "POST", "/api/v1/me/bookings", json={"sessionId": session_id}, headers=_auth(member_token)
        )

    def cancel_booking(self, member_token: str, booking_id: str) -> None:
        self._request("DELETE", f"/api/v1/me/bookings/{booking_id}", headers=_auth(member_token))

    def _request(self, method: str, path: str, **kwargs) -> httpx.Response:
        try:
            response = self._http.request(method, path, **kwargs)
        except httpx.HTTPError as exc:
            raise GymflowUnavailableError(str(exc)) from exc
        if response.is_error:
            raise GymflowApiError(_detail(response))
        return response


def get_gymflow_client() -> GymflowClient:
    return GymflowClient(settings.backend_base_url)
