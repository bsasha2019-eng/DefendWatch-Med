# Testing Guide: DefendWatch Med

## 1. Запуск бекенд-тестів (Backend Tests)

Бекенд використовує фреймворк FastAPI, який сумісний із `pytest` та бібліотекою `httpx` (`TestClient`).

### 1.1. Встановлення тестових залежностей
```powershell
pip install pytest httpx
```

### 1.2. Команда запуску всіх тестів бекенду
З кореневої папки або з директорії `backend/`:
```powershell
pytest backend/ -v
```

### 1.3. Ручна перевірка API через curl / PowerShell
- **Перевірка кореневого статусу:**
  ```powershell
  curl.exe -i http://127.0.0.1:8000/
  ```
- **Перевірка ендпоінту здоров'я:**
  ```powershell
  curl.exe -i http://127.0.0.1:8000/health
  ```
- **Перевірка аналізу травми:**
  ```powershell
  curl.exe -X POST http://127.0.0.1:8000/api/ai-guide `
    -H "Content-Type: application/json" `
    -d "{\"description\": \"сильна кровотеча з ноги\"}"
  ```
- **Перевірка валідації (очікується HTTP 400):**
  ```powershell
  curl.exe -X POST http://127.0.0.1:8000/api/ai-guide `
    -H "Content-Type: application/json" `
    -d "{\"description\": \"d\"}"
  ```

---

## 2. Запуск тестів Android-додатку (Mobile Tests)

### 2.1. Модульні тести (Unit Tests)
Перевірка бізнес-логіки, парсингу моделей та сервісів без запуску емулятора:
```powershell
# Запуск юніт-тестів
./gradlew test

# Запуск юніт-тестів конкретної конфігурації
./gradlew testDebugUnitTest
```

### 2.2. Інструментальні тести (UI & Instrumentation Tests)
Тестування Composable-компонентів на підключеному емуляторі або реальному пристрої:
```powershell
./gradlew connectedAndroidTest
```

---

## 3. Діагностика мережевого з'єднання (End-to-End Checklist)

Перед запуском тестів переконайтеся:
1. **Бекенд активний:**
   ```powershell
   python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
   ```
2. **Прокинуто порт до емулятора:**
   ```powershell
   adb reverse tcp:8000 tcp:8000
   ```
   *(Команда повинна вивести `8000`).*
3. **Емулятор підключений:**
   ```powershell
   adb devices
   ```
