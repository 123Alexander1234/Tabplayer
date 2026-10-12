from fastapi import FastAPI, UploadFile, File, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import tempfile
import subprocess
import json

app = FastAPI(title="TabPlayer Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class TranscribeRequest(BaseModel):
    video_id: str

@app.get("/")
def root():
    return {"status": "ok", "message": "TabPlayer server is running"}

@app.get("/search")
def search(q: str = Query(..., min_length=2)):
    try:
        result = subprocess.run(
            ["yt-dlp", f"ytsearch10:{q}", "--dump-json", "--flat-playlist", "--no-warnings"],
            capture_output=True, text=True, timeout=60
        )
        items = []
        for line in result.stdout.strip().split("\n"):
            if not line:
                continue
            data = json.loads(line)
            items.append({
                "video_id": data.get("id"),
                "title": data.get("title"),
                "uploader": data.get("uploader") or data.get("channel"),
                "duration": data.get("duration"),
                "thumbnail": data.get("thumbnail")
            })
        return {"results": items}
    except Exception as e:
        return {"results": [], "error": str(e)}

@app.post("/transcribe_url")
async def transcribe_url(req: TranscribeRequest):
    return make_demo_tabs(f"YouTube: {req.video_id}")

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
