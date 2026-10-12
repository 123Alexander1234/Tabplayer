FROM python:3.10-slim

RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN pip install --no-cache-dir fastapi uvicorn python-multipart yt-dlp

COPY main.py /app/main.py

EXPOSE 8080

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080"]
