# API Contract: DefendWatch Med Backend

- **Базова адреса для емулятора (з `adb reverse`):** `http://127.0.0.1:8000`
- **Базова адреса для браузера на ПК:** `http://127.0.0.1:8000` або `http://localhost:8000`
- **Формат даних:** `application/json`

---

## 1. Ендпоінти (Endpoints)

### 1.1. Root Information
- **URL:** `/`
- **Метод:** `GET`
- **Призначення:** Швидка перевірка доступності сервера та посилання на Swagger UI та історію.
- **Відповідь (200 OK):**
```json
{
  "status": "ok",
  "service": "DefendWatch Med Backend",
  "docs": "/docs",
  "history": "/api/history"
}
```

---

### 1.2. Health Check
- **URL:** `/health`
- **Метод:** `GET`
- **Призначення:** Моніторинг стану бекенду.
- **Відповідь (200 OK):**
```json
{
  "status": "ok",
  "service": "DefendWatch Med Backend"
}
```

---

### 1.3. Отримання медичної поради ШІ (AI Guide)
- **URL:** `/api/ai-guide`
- **Метод:** `POST`
- **Призначення:** Аналіз опису травми за допомогою Gemini, надання первинної інструкції та автоматичне збереження в історію.

#### Тіло запиту (Request Body)
```json
{
  "description": "сильна кровотеча з ноги"
}
```

| Поле | Тип | Обов'язкове | Обмеження | Опис |
|---|---|---|---|---|
| `description` | `string` | Так | `min_length >= 3` | Опис травми, стану потерпілого чи симптомів |

#### Успішна відповідь (200 OK)
```json
{
  "category": "Кровотеча",
  "advice": "Накладіть кровоспинний джгут (турнікет) вище місця поранення. Зафіксуйте точний час накладання. Притисніть рану чистою тканиною до прибуття медиків."
}
```

| Поле | Тип | Опис |
|---|---|---|
| `category` | `string` | Одне або кілька слів категорії (напр.: "Кровотеча", "Опік", "Травма") |
| `advice` | `string` | Текстова порада з 2–3 речень з першої допомоги |

#### Відповіді з помилками (Error Responses)
- **400 Bad Request** (опис коротший за 3 символи або порожній):
```json
{
  "detail": "Опис занадто короткий або порожній."
}
```
- **422 Unprocessable Entity** (некоректний JSON або відсутнє поле `description`):
```json
{
  "detail": [
    {
      "loc": ["body", "description"],
      "msg": "field required",
      "type": "value_error.missing"
    }
  ]
}
```
- **500 Internal Server Error** (помилка на стороні Gemini або парсингу):
```json
{
  "detail": "Помилка ШІ: <опис винятку>"
}
```

---

### 1.4. Отримання історії порад (Get History)
- **URL:** `/api/history`
- **Метод:** `GET`
- **Призначення:** Отримання списку всіх раніше наданих порад у порядку від найновіших до найстаріших.
- **Відповідь (200 OK):**
```json
[
  {
    "id": "c1f7a28e-5b6d-4912-9c3f-7e801b6a1234",
    "timestamp": "2026-11-03 18:24:00",
    "description": "сильна кровотеча з ноги",
    "category": "Кровотеча",
    "advice": "Накладіть кровоспинний джгут вище місця рани."
  }
]
```

---

### 1.5. Очищення історії порад (Clear History)
- **URL:** `/api/history`
- **Метод:** `DELETE`
- **Призначення:** Повне очищення збереженої історії порад.
- **Відповідь (200 OK):**
```json
{
  "status": "ok",
  "message": "Історію порад успішно очищено."
}
```

---

## 2. Моделі даних (Data Models)

### Python (Pydantic)
```python
class MedicalRequest(BaseModel):
    description: str

class MedicalResponse(BaseModel):
    category: str
    advice: str

class HistoryItem(BaseModel):
    id: str
    timestamp: str
    description: str
    category: str
    advice: str
```

### Kotlin (Retrofit / Gson)
```kotlin
data class MedicalRequest(val description: String)
data class MedicalResponse(val category: String, val advice: String)

data class HistoryItem(
    val id: String,
    val timestamp: String,
    val description: String,
    val category: String,
    val advice: String
)
```
