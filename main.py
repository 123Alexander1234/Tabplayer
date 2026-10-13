import os
import tempfile
from fastapi import FastAPI, UploadFile, File, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

try:
    from yandex_music import Client as YandexClient
except ImportError:
    YandexClient = None

app = FastAPI(title="TabPlayer Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

YANDEX_TOKEN = os.environ.get("YANDEX_TOKEN", "")


class YandexTranscribeRequest(BaseModel):
    track_id: str


@app.get("/")
def root():
    return {
        "status": "ok",
        "message": "TabPlayer server is running",
        "yandex_available": YandexClient is not None,
        "yandex_token_set": bool(YANDEX_TOKEN)
    }


@app.get("/yandex_search")
def yandex_search(q: str = Query(..., min_length=2)):
    if not YandexClient:
        return {"results": [], "error": "yandex-music library not installed"}
    if not YANDEX_TOKEN:
        return {"results": [], "error": "YANDEX_TOKEN not set"}
    try:
        client = YandexClient(YANDEX_TOKEN).init()
        search_result = client.search(q)
        items = []
        if search_result.tracks and search_result.tracks.results:
            for track in search_result.tracks.results[:10]:
                artists = ", ".join(a.name for a in track.artists) if track.artists else "Unknown"
                items.append({
                    "track_id": str(track.id),
                    "title": track.title,
                    "artists": artists,
                    "duration": track.duration_ms or 0
                })
        return {"results": items}
    except Exception as e:
        return {"results": [], "error": str(e)}


@app.post("/yandex_transcribe")
async def yandex_transcribe(req: YandexTranscribeRequest):
    if not YandexClient:
        return {"error": "yandex-music library not installed"}
    if not YANDEX_TOKEN:
        return {"error": "YANDEX_TOKEN not set"}
    try:
        client = YandexClient(YANDEX_TOKEN).init()
        track_id_int = int(req.track_id)
        tracks = client.tracks([track_id_int])
        if not tracks:
            return {"error": f"Track {req.track_id} not found"}
        track = tracks[0]

        download_info = track.get_download_info()
        if not download_info:
            return {"error": "No download info (subscription required?)"}

        # Выбираем лучшее качество (устойчиво к разным версиям библиотеки)
        def bitrate_of(d):
            return getattr(d, 'bitrate_in_kbps', None) or getattr(d, 'bitrate', 0) or 0

        best = sorted(download_info, key=bitrate_of, reverse=True)[0]
        direct_link = best.get_direct_link()

        # Скачиваем файл во временное хранилище
        with tempfile.NamedTemporaryFile(delete=False, suffix=".mp3") as f:
            response = client.request.get(direct_link)
            f.write(response.content)
            saved_path = f.name

        file_size = os.path.getsize(saved_path)

        artists = ", ".join(a.name for a in track.artists) if track.artists else ""
        title = f"{track.title} — {artists}" if artists else track.title

        # TODO: Здесь позже будет Demucs + Basic Pitch
        # Сейчас возвращаем заглушку, но файл уже скачан в saved_path
        return make_demo_tabs(f"{title} (size={file_size}b)")
    except Exception as e:
        import traceback
        return {
            "error": f"{type(e).__name__}: {str(e)}",
            "trace": traceback.format_exc()[-500:]
        }


@app.post("/transcribe")
async def transcribe(audio: UploadFile = File(...)):
    with tempfile.NamedTemporaryFile(delete=False, suffix=".mp3") as f:
        f.write(await audio.read())
    return make_demo_tabs(audio.filename or "Unknown track")


def make_demo_tabs(title: str):
    return {
        "title": title,
        "bpm": 120,
        "tuning": ["E", "A", "D", "G"],
        "notes": [
            {"timeMs": 0,    "string": 4, "fret": 3, "durationMs": 250},
            {"timeMs": 250,  "string": 4, "fret": 3, "durationMs": 250},
            {"timeMs": 500,  "string": 3, "fret": 5, "durationMs": 250},
            {"timeMs": 750,  "string": 3, "fret": 5, "durationMs": 250},
            {"timeMs": 1000, "string": 3, "fret": 2, "durationMs": 250},
            {"timeMs": 1250, "string": 3, "fret": 2, "durationMs": 250},
            {"timeMs": 1500, "string": 4, "fret": 3, "durationMs": 250},
            {"timeMs": 1750, "string": 4, "fret": 3, "durationMs": 250}
        ]
    }
