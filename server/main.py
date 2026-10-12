from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
import tempfile

app = FastAPI(title="TabPlayer Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def root():
    return {"status": "ok", "message": "TabPlayer server is running"}

@app.post("/transcribe")
async def transcribe(audio: UploadFile = File(...)):
    with tempfile.NamedTemporaryFile(delete=False, suffix=".mp3") as f:
        f.write(await audio.read())
        saved_path = f.name

    return {
        "title": audio.filename or "Unknown track",
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
