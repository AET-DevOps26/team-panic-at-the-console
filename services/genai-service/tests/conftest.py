import pytest
from fastapi.testclient import TestClient


@pytest.fixture()
def client():
    from genai_service.main import app

    with TestClient(app, raise_server_exceptions=True) as c:
        yield c
