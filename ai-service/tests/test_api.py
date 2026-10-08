from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_ok():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


def test_plan_validates_input():
    r = client.post("/v1/trainer/plan", json={"goal": "hypertrophy", "level": "beginner", "days_per_week": 99})
    assert r.status_code == 422


def test_plan_not_implemented_yet():
    r = client.post("/v1/trainer/plan", json={"goal": "strength", "level": "intermediate", "days_per_week": 4})
    assert r.status_code == 501
