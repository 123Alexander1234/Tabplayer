from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

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

def make_demo_tabs(title: str):
    return {
        "title": title,
        "bpm": 120,
        "tuning": ["E", "A", "D", "G"],
        "notes": [
            {"timeMs": 0,    "string": 4, "fret": 3, "durationMs": 250},
            {"timeMs": 250,  "string": 4, "fret": 3, "durationMs": 250},
            {"timeMs": 500,  "string": 3, "fret": 5, "durationMs": 250},
            {"timeMs": 750,  "string": 3, "fret": 5, "durationMs": 250}
        ]
    }
