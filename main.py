import os
import time
import traceback
import google.generativeai as genai
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv

env_path = os.path.join(os.path.dirname(__file__), ".env")
load_dotenv(dotenv_path=env_path)

API_KEY = os.getenv("GEMINI_API_KEY")

if not API_KEY:
    print("УВАГА: GEMINI_API_KEY не знайдено!")
else:
    # transport='rest' використовує швидкий HTTP замість важкого gRPC
    genai.configure(api_key=API_KEY, transport="rest")

app = FastAPI(title="DefendWatch Med API")

class MedicalRequest(BaseModel):
    description: str

class MedicalResponse(BaseModel):
    category: str
    advice: str

@app.get("/")
def read_root():
    return {
        "status": "ok",
        "service": "DefendWatch Med Backend",
        "docs": "/docs"
    }

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "DefendWatch Med Backend"}

@app.post("/api/ai-guide", response_model=MedicalResponse)
def get_ai_medical_advice(request: MedicalRequest):
    print(f"\n==========================================")
    print(f"-> Отримано запит: '{request.description}'")
    if not request.description or len(request.description.strip()) < 3:
        raise HTTPException(status_code=400, detail="Опис занадто короткий або порожній.")

    start_time = time.time()
    
    prompt = f"""
    Ти - тактичний медичний асистент. Тобі надійшов опис ситуації: "{request.description}".
    Поверни відповідь СУВОРО у форматі:
    Категорія: [Одне слово, наприклад: Кровотеча, Опік, Травма]
    Порада: [Коротка інструкція з першої допомоги на 2-3 речення. Без діагнозів!]
    """

    # Швидкі моделі для швидкої тактичної допомоги (без тривалого 'thinking' затримки)
    models_to_try = [
        "gemini-flash-latest",
        "gemini-2.5-flash-lite",
        "gemini-3.5-flash-lite",
        "gemini-3.8-flash",
    ]

    response = None
    last_error = None

    for model_name in models_to_try:
        try:
            print(f"Звертаємось до моделі {model_name}...")
            model = genai.GenerativeModel(
                model_name=model_name,
                generation_config={
                    "temperature": 0.2,
                    "max_output_tokens": 200,
                }
            )
            # Таймаут 10 секунд на модель, щоб не блокувати додаток
            response = model.generate_content(prompt, request_options={"timeout": 10})
            elapsed = time.time() - start_time
            print(f"<- Успішна відповідь від {model_name} за {elapsed:.2f} сек!")
            break
        except Exception as e:
            print(f"Модель {model_name} повернула помилку або таймаут: {e}")
            last_error = e

    if not response:
        print(f"ПОМИЛКА: Жодна модель не відповіла. Останній виняток: {last_error}")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Помилка ШІ: {str(last_error)}")

    try:
        text = response.text
        print(f"Текст відповіді:\n{text}")
        
        category = "Невідомо"
        advice = text
        if "Категорія:" in text and "Порада:" in text:
            parts = text.split("Порада:")
            category = parts[0].replace("Категорія:", "").strip()
            advice = parts[1].strip()

        return MedicalResponse(category=category, advice=advice)
    
    except Exception as e:
        print(f"Помилка парсингу відповіді: {e}")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Помилка парсингу: {str(e)}")
