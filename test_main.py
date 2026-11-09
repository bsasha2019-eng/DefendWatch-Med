import pytest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient

try:
    from backend.main import app, history_storage
except ImportError:
    from main import app, history_storage

client = TestClient(app)

@pytest.fixture(autouse=True)
def run_before_and_after_tests():
    # Перед кожним тестом очищуємо історію
    history_storage.clear()
    yield
    # Після кожного тесту також очищуємо історію
    history_storage.clear()

def test_root_endpoint():
    """Тест перевірки головного кореневого ендпоінту /"""
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "history" in data
    assert data["history"] == "/api/history"

def test_health_check():
    """Тест перевірки ендпоінту /health"""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "DefendWatch Med Backend"}

def test_validation_empty_or_short_description():
    """Тест валідації: опис коротший за 3 символи повинен повертати 400"""
    response = client.post("/api/ai-guide", json={"description": "d"})
    assert response.status_code == 400
    assert "Опис занадто короткий" in response.json()["detail"]

    response_empty = client.post("/api/ai-guide", json={"description": "   "})
    assert response_empty.status_code == 400

@patch("google.generativeai.GenerativeModel")
def test_ai_guide_success_and_history_saving(mock_model_class):
    """
    Тест успішного отримання поради та автоматичного збереження в історію (/api/history)
    """
    # Мокаємо відповідь від Google Gemini, щоб тест був швидким і не залежав від зовнішньої мережі
    mock_model_instance = MagicMock()
    mock_model_instance.generate_content.return_value = MagicMock(
        text="Категорія: Травма\nПорада: Зафіксуйте ушкоджену кінцівку шиною. Прикладіть холод до місця удару."
    )
    mock_model_class.return_value = mock_model_instance

    # 1. Відправляємо запит до /api/ai-guide
    request_payload = {"description": "підозра на перелом ноги"}
    response = client.post("/api/ai-guide", json=request_payload)
    
    assert response.status_code == 200
    data = response.json()
    assert data["category"] == "Травма"
    assert "Зафіксуйте ушкоджену кінцівку" in data["advice"]

    # 2. Перевіряємо, що запис з'явився у /api/history
    history_response = client.get("/api/history")
    assert history_response.status_code == 200
    history_items = history_response.json()
    assert len(history_items) == 1
    
    saved_item = history_items[0]
    assert saved_item["description"] == "підозра на перелом ноги"
    assert saved_item["category"] == "Травма"
    assert "Зафіксуйте ушкоджену кінцівку" in saved_item["advice"]
    assert "id" in saved_item
    assert "timestamp" in saved_item

@patch("google.generativeai.GenerativeModel")
def test_clear_history(mock_model_class):
    """Тест очищення історії через DELETE /api/history"""
    # Налаштовуємо мок
    mock_model_instance = MagicMock()
    mock_model_instance.generate_content.return_value = MagicMock(
        text="Категорія: Опік\nПорада: Охолодіть місце опіку чистою водою."
    )
    mock_model_class.return_value = mock_model_instance

    # Додаємо запис
    client.post("/api/ai-guide", json={"description": "опік кисті руки"})
    
    # Переконуємось, що запис є
    assert len(client.get("/api/history").json()) == 1

    # Викликаємо очищення історії
    delete_response = client.delete("/api/history")
    assert delete_response.status_code == 200
    assert delete_response.json()["status"] == "ok"

    # Переконуємось, що історія тепер порожня
    history_after = client.get("/api/history")
    assert history_after.status_code == 200
    assert history_after.json() == []
