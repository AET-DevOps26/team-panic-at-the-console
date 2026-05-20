import os

import pytest
from fastapi.testclient import TestClient

# Disable NATS by default for all tests: no broker available, and we don't want
# lifespan to block on a connect timeout. Individual tests opt back in via fixtures.
os.environ.setdefault("NATS_ENABLED", "false")


@pytest.fixture()
def client():
    from genai_service.main import app

    with TestClient(app) as c:
        yield c
