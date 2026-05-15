import pytest
from fastapi.testclient import TestClient


@pytest.fixture()
def client():
    from genai_service.main import app

    with TestClient(app) as c:
        yield c
