from app.llm import get_client


def test_client_has_no_sdk_retries_and_a_short_connect_timeout():
    # Los reintentos son cosa de app/trainer.py (con el motivo del fallo); duplicarlos aquí
    # solo alargaba la espera, sobre todo cuando no hay ni proveedor al que conectar.
    client = get_client()

    assert client.max_retries == 0
    assert client.timeout.connect == 3.0
    assert client.timeout.read == 90.0
