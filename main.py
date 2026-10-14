from fastapi import FastAPI, UploadFile, File, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import os
import json
import uuid
import httpx
from pathlib import Path
from datetime import datetime

app = FastAPI(title="TabPlayer Light API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Папка для хранения JSON с табами
DATA_DIR = Path("/data/tabs")
DATA_DIR.mkdir(parents=True, exist_ok=True)

# Адрес GPU-сервера (берётся из переменной окружения)
GPU_SERVER_URL = os.environ.get("GPU_SERVER_URL", "")


@app.get("/")
def root():
    return {
        "status": "ok",
        "message": "TabPlayer Light API is running",
        "gpu_server_configured": bool(GPU_SERVER_URL),
        "gpu_server_url": GPU_SERVER_URL[:30] + "..." if GPU_SERVER_URL else None,
    }


@app.get("/health")
def health():
    """Проверка доступности GPU-сервера."""
    if not GPU_SERVER_URL:
        return {"gpu_available": False, "reason": "GPU_SERVER_URL not set"}
    try:
        r = httpx.get(f"{GPU_SERVER_URL}/", timeout=10)
        return {"gpu_available": r.status_code == 200, "gpu_response": r.json()}
    except Exception as e:
        return {"gpu_available": False, "error": str(e)}


@app.post("/analyze")
async def analyze(audio: UploadFile = File(...)):
    """
    Принимает MP3, пересылает на GPU-сервер, сохраняет JSON с табами.
    Возвращает JSON с табами.
    """
    if not GPU_SERVER_URL:
        raise HTTPException(
            status_code=503,
            detail="GPU_SERVER_URL не настроен. Запустите GPU-сервер на vast.ai "
                   "и укажите его публичный адрес в переменной окружения.",
        )

    # Читаем MP3
    audio_bytes = await audio.read()
    if len(audio_bytes) == 0:
        raise HTTPException(status_code=400, detail="Пустой файл")

    # Отправляем на GPU-сервер
    try:
        async with httpx.AsyncClient(timeout=600.0) as client:
            files = {"audio": (audio.filename or "track.mp3", audio_bytes, "audio/mpeg")}
            response = await client.post(f"{GPU_SERVER_URL}/analyze", files=files)

        if response.status_code != 200:
            raise HTTPException(
                status_code=502,
                detail=f"GPU server error: {response.status_code} — {response.text[:500]}",
            )

        result = response.json()

    except httpx.TimeoutException:
        raise HTTPException(status_code=504, detail="GPU server timeout")
    except httpx.RequestError as e:
        raise HTTPException(status_code=502, detail=f"GPU server unreachable: {e}")

    # Сохраняем JSON с табами
    track_id = str(uuid.uuid4())[:8]
    result["track_id"] = track_id
    result["analyzed_at"] = datetime.utcnow().isoformat()

    tab_path = DATA_DIR / f"{track_id}.json"
    with open(tab_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False)

    return result


@app.get("/tabs/{track_id}")
def get_tabs(track_id: str):
    """Возвращает сохранённые табы по ID."""
    tab_path = DATA_DIR / f"{track_id}.json"
    if not tab_path.exists():
        raise HTTPException(status_code=404, detail="Табы не найдены")

    with open(tab_path, "r", encoding="utf-8") as f:
        return json.load(f)


@app.get("/tabs")
def list_tabs():
    """Список всех сохранённых треков."""
    tracks = []
    for f in sorted(DATA_DIR.glob("*.json"), key=os.path.getmtime, reverse=True):
        try:
            with open(f, "r", encoding="utf-8") as file:
                data = json.load(file)
            tracks.append({
                "track_id": f.stem,
                "title": data.get("title", "Unknown"),
                "bpm": data.get("bpm"),
                "key": data.get("key"),
                "analyzed_at": data.get("analyzed_at"),
            })
        except Exception:
            continue
    return {"tracks": tracks[:50]}


@app.delete("/tabs/{track_id}")
def delete_tabs(track_id: str):
    """Удаляет сохранённые табы."""
    tab_path = DATA_DIR / f"{track_id}.json"
    if not tab_path.exists():
        raise HTTPException(status_code=404, detail="Табы не найдены")
    tab_path.unlink()
    return {"status": "deleted", "track_id": track_id}
