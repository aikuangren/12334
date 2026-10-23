#!/usr/bin/env python3
import argparse
import binascii
import json
import os
import re
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SEED_FILE = ROOT / "src/main/java/com/fiftytalk/carddraw/config/WordCardSeedData.java"
AUDIO_DIR = ROOT / "uploads/card-draw/audio/v1"


def seed_words():
    source = SEED_FILE.read_text(encoding="utf-8")
    words = re.findall(r'new WordCardSeed\("([^"]+)"', source)
    return sorted(set(words), key=str.lower)


def missing_words(words):
    existing = {path.stem for path in AUDIO_DIR.glob("*.mp3")}
    return [word for word in words if word not in existing]


def request_audio(api_key, endpoint, model, voice_id, word):
    payload = {
        "model": model,
        "text": word,
        "stream": False,
        "language_boost": "English",
        "output_format": "hex",
        "voice_setting": {
            "voice_id": voice_id,
            "speed": 0.92,
            "vol": 1,
            "pitch": 0
        },
        "audio_setting": {
            "sample_rate": 32000,
            "bitrate": 128000,
            "format": "mp3",
            "channel": 1
        }
    }
    request = urllib.request.Request(
        endpoint,
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=90) as response:
        body = json.loads(response.read().decode("utf-8"))
    status_code = body.get("base_resp", {}).get("status_code")
    if status_code not in (0, None):
        raise RuntimeError(body.get("base_resp", {}).get("status_msg") or json.dumps(body, ensure_ascii=False))
    audio_hex = body.get("data", {}).get("audio")
    if not audio_hex:
        raise RuntimeError("MiniMax response did not contain audio data")
    return binascii.unhexlify(audio_hex)


def write_audio(word, audio):
    AUDIO_DIR.mkdir(parents=True, exist_ok=True)
    target = AUDIO_DIR / f"{word}.mp3"
    fd, tmp_name = tempfile.mkstemp(prefix=f".{word}.", suffix=".tmp", dir=AUDIO_DIR)
    with os.fdopen(fd, "wb") as tmp:
        tmp.write(audio)
    Path(tmp_name).replace(target)
    return target


def main():
    parser = argparse.ArgumentParser(description="Generate missing word-card audio with MiniMax T2A.")
    parser.add_argument("--all", action="store_true", help="Regenerate all seed words instead of missing files only.")
    parser.add_argument("--dry-run", action="store_true", help="Print the target word list without generating audio.")
    parser.add_argument("--limit", type=int, default=0, help="Generate at most N files.")
    parser.add_argument("--endpoint", default=os.environ.get("MINIMAX_TTS_ENDPOINT", "https://api.minimax.io/v1/t2a_v2"))
    parser.add_argument("--model", default=os.environ.get("MINIMAX_TTS_MODEL", "speech-2.8-turbo"))
    parser.add_argument("--voice-id", default=os.environ.get("MINIMAX_VOICE_ID", "English_expressive_narrator"))
    args = parser.parse_args()

    words = seed_words()
    targets = words if args.all else missing_words(words)
    if args.limit > 0:
        targets = targets[:args.limit]

    print(f"target_count={len(targets)}")
    for word in targets:
        print(word)
    if args.dry_run or not targets:
        return 0

    api_key = os.environ.get("MINIMAX_API_KEY")
    if not api_key:
        print("MINIMAX_API_KEY is required. Export it in your terminal before running this script.", file=sys.stderr)
        return 2

    for index, word in enumerate(targets, start=1):
        try:
            audio = request_audio(api_key, args.endpoint, args.model, args.voice_id, word)
            target = write_audio(word, audio)
            print(f"[{index}/{len(targets)}] generated {target.relative_to(ROOT)}")
            time.sleep(0.25)
        except (urllib.error.HTTPError, urllib.error.URLError, RuntimeError, TimeoutError) as exc:
            print(f"[{index}/{len(targets)}] failed {word}: {exc}", file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
