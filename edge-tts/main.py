import io
import logging
import re

from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel
from gtts import gTTS

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("gtts-tts")


def clean_text(text: str) -> str:
    text = re.sub(r"\*\*(.+?)\*\*", r"\1", text)
    text = re.sub(r"\*(.+?)\*", r"\1", text)
    text = re.sub(r"#{1,6}\s*", "", text)
    text = re.sub(r"\n\s*[-*]\s+", ". ", text)
    text = re.sub(r"\n\s*\d+\.\s+", ". ", text)
    text = text.replace("\\n", " ").replace("\n", " ").replace("\r", " ")
    text = re.sub(r"[*_{}\[\]|>`~\\]", "", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text

app = FastAPI(title="gTTS Service")


class TtsRequest(BaseModel):
    model: str = "gtts"
    input: str
    voice: str = "pt"


@app.get("/health")
def health():
    return {"status": "ok", "service": "gtts"}


@app.get("/v1/audio/voices")
def voices():
    return {
        "voices": [
            {"id": "pt", "language": "pt-BR", "gender": "Female", "description": "Português (Brasil)"},
            {"id": "pt-br", "language": "pt-BR", "gender": "Female", "description": "Português Brasil"},
            {"id": "en", "language": "en-US", "gender": "Female", "description": "English (US)"},
            {"id": "en-us", "language": "en-US", "gender": "Female", "description": "English US"},
        ]
    }


@app.post("/v1/audio/speech")
def synthesize(req: TtsRequest):
    log.info("Synthesizing: voice=%s, text_len=%d", req.voice, len(req.input))

    try:
        cleaned = clean_text(req.input)
        log.info("Cleaned text: %d -> %d chars", len(req.input), len(cleaned))

        lang = "pt" if "pt" in req.voice.lower() else "en"
        tld = "com.br" if "pt" in req.voice.lower() else "com"
        tts = gTTS(text=cleaned, lang=lang, tld=tld, slow=False)

        buf = io.BytesIO()
        tts.write_to_fp(buf)
        audio_bytes = buf.getvalue()

        if not audio_bytes:
            raise HTTPException(status_code=500, detail="No audio generated")

        log.info("Generated %d bytes of audio (lang=%s)", len(audio_bytes), lang)
        return Response(content=audio_bytes, media_type="audio/mpeg")
    except Exception as e:
        log.exception("Failed to synthesize speech")
        raise HTTPException(status_code=500, detail=str(e))
