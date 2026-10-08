from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_ok():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


def test_plan_validates_input():
    # days_per_week fuera de rango (1-7): nunca llega a tocar al LLM, Pydantic lo rechaza antes.
    r = client.post("/v1/trainer/plan", json={"goal": "hypertrophy", "level": "beginner", "days_per_week": 99})
    assert r.status_code == 422
