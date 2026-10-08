import json

from fastapi.testclient import TestClient

from app.llm import get_client
from app.main import app
from tests.fakes import FailingOpenAIClient, FakeOpenAIClient

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
    assert fake.calls == 3  # 1 intento + 2 reintentos, ni uno más ni uno menos


def test_plan_wrong_day_count_is_treated_as_invalid():
    # Cumple el esquema de Pydantic pero no pidió 2 días: debe tratarse igual que un JSON inválido.
    one_day_plan = {**VALID_PLAN, "days": VALID_PLAN["days"][:1]}
    fake = FakeOpenAIClient(lambda call: json.dumps(one_day_plan))
    _override(fake)

    r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 502
    assert fake.calls == 3


def test_plan_503_when_llm_unreachable():
    _override(FailingOpenAIClient())

    r = client.post("/v1/trainer/plan", json=REQUEST)

    assert r.status_code == 503
    assert "no está disponible" in r.json()["detail"]
