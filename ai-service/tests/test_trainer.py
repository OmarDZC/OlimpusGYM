import json
import logging

from fastapi.testclient import TestClient

from app.config import settings
from app.llm import get_client
from app.main import app
from tests.fakes import (
    FailingOpenAIClient,
    FailingOpenAIClientWithStatus,
    FakeOpenAIClient,
    ScriptedToolCallingClient,
    fail,
    make_status_error,
    text,
)

client = TestClient(app)

REQUEST = {
    "goal": "hypertrophy",
    "level": "beginner",
    "days_per_week": 2,
    "equipment": ["gimnasio completo"],
    "limitations": [],
}

VALID_PLAN = {
    "summary": "Dos días de cuerpo completo para empezar con buena técnica.",
    "days": [
        {
            "day": 1,
            "focus": "Cuerpo completo A",
            "exercises": [
                {"name": "Sentadilla goblet", "sets": 3, "reps": "10-12", "rest_seconds": 90, "notes": None},
                {"name": "Remo con mancuerna", "sets": 3, "reps": "10-12", "rest_seconds": 90, "notes": None},
            ],
        },
        {
            "day": 2,
            "focus": "Cuerpo completo B",
            "exercises": [
                {"name": "Press de banca", "sets": 3, "reps": "8-10", "rest_seconds": 90, "notes": None},
                {"name": "Peso muerto rumano", "sets": 3, "reps": "8-10", "rest_seconds": 120, "notes": None},
            ],
        },
    ],
}


def _override(fake_client) -> None:
    app.dependency_overrides[get_client] = lambda: fake_client


def teardown_function() -> None:
    app.dependency_overrides.clear()


def test_plan_generated_on_first_try():
    fake = FakeOpenAIClient(lambda call: json.dumps(VALID_PLAN))
    _override(fake)

    r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 200
    body = r.json()
    assert body["summary"] == VALID_PLAN["summary"]
    assert len(body["days"]) == 2
    assert fake.calls == 1


def test_plan_retries_after_invalid_response_and_then_succeeds():
    def respond(call: int) -> str:
        if call == 1:
            return "esto no es JSON en absoluto"
        return json.dumps(VALID_PLAN)

    fake = FakeOpenAIClient(respond)
    _override(fake)

    r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 200
    assert r.json()["summary"] == VALID_PLAN["summary"]
    # Se corrigió a la segunda: demuestra que sí hubo un reintento, no que acertó a la primera.
    assert fake.calls == 2


def test_plan_fails_definitively_after_max_retries():
    fake = FakeOpenAIClient(lambda call: "{\"esto\": \"no cumple el esquema\"}")
    _override(fake)

    r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 502
    assert fake.calls == 2  # 1 intento + 1 reintento (Trozo 9e: antes eran 2 reintentos), ni uno más


def test_plan_wrong_day_count_is_treated_as_invalid():
    # Cumple el esquema de Pydantic pero no pidió 2 días: debe tratarse igual que un JSON inválido.
    one_day_plan = {**VALID_PLAN, "days": VALID_PLAN["days"][:1]}
    fake = FakeOpenAIClient(lambda call: json.dumps(one_day_plan))
    _override(fake)

    r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 502
    assert fake.calls == 2


def test_plan_503_when_llm_unreachable():
    _override(FailingOpenAIClient())

    r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 503
    assert "no está disponible" in r.json()["detail"]


def test_provider_failure_is_logged_without_the_key(caplog):
    _override(FailingOpenAIClientWithStatus(status_code=401, message="Invalid API Key"))

    with caplog.at_level(logging.ERROR, logger="app.main"):
        r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 503
    assert "_FakeProviderStatusError" in caplog.text
    assert "401" in caplog.text
    assert "Bearer" not in caplog.text
    assert "sk-totally-fake-secret-value" not in caplog.text


# ---- 429 del proveedor, distinto de "no disponible", y respaldo opcional (Trozo 9e) ----


def test_429_agotando_reintentos_sin_respaldo_da_el_mensaje_de_saturacion(monkeypatch):
    monkeypatch.setattr("app.llm_retry.time_module.sleep", lambda _s: None)
    monkeypatch.setattr(settings, "llm_fallback_base_url", "")
    llm = ScriptedToolCallingClient([fail(make_status_error(429)) for _ in range(3)])
    _override(llm)

    r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 429
    assert "saturado" in r.json()["detail"].lower()
    assert "no está disponible" not in r.json()["detail"]


def test_429_agotando_reintentos_con_respaldo_configurado_genera_la_rutina_ahi(monkeypatch):
    monkeypatch.setattr("app.llm_retry.time_module.sleep", lambda _s: None)
    monkeypatch.setattr(settings, "llm_fallback_base_url", "http://fallback.test/v1")
    monkeypatch.setattr(settings, "llm_fallback_model", "modelo-de-respaldo")
    primary = ScriptedToolCallingClient([fail(make_status_error(429)) for _ in range(3)])
    fallback = ScriptedToolCallingClient([text(json.dumps(VALID_PLAN))])
    monkeypatch.setattr("app.llm_retry.fallback_client", lambda: fallback)
    _override(primary)

    r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 200
    assert r.json()["summary"] == VALID_PLAN["summary"]
    assert primary.calls == 3
    assert fallback.calls == 1


def test_max_tokens_se_limita_segun_los_dias_pedidos():
    # REQUEST pide 2 días; un tope fijo para cualquier número de días cortaba a mitad de JSON
    # las rutinas de más días (visto de verdad con 5 días: "Expecting value" al parsear).
    llm = ScriptedToolCallingClient([text(json.dumps(VALID_PLAN))])
    _override(llm)

    r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 200
    sent_kwargs = llm.received_calls[-1]
    assert sent_kwargs.get("max_tokens") == 150 + 260 * REQUEST["days_per_week"]
