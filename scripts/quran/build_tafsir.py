#!/usr/bin/env python3
"""Bundles al-Tafsir al-Muyassar, so the commonest commentary opens with no connection at all.

al-Muyassar is the King Fahd Complex's own short commentary, written for the general reader: a few
plain sentences an ayah. It is read from the Quran.com API (resource 16), stripped of markup, and
written one passage per ayah. The other commentaries are fetched by the app a surah at a time.

Usage:
    python3 scripts/quran/build_tafsir.py [--cache DIR]

Writes:
    app/src/main/assets/tafsir-muyassar.json
"""
import argparse
import html
import json
import pathlib
import re
import sys
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
TEXT = ROOT / "app/src/main/assets/quran-ar.json"
ASSET = ROOT / "app/src/main/assets/tafsir-muyassar.json"
API = "https://api.quran.com/api/v4/tafsirs/16/by_chapter/{surah}?per_page=300"
USER_AGENT = "bilal-build-script"


def fetch(surah: int, cache: pathlib.Path) -> dict:
    path = cache / f"{surah:03d}.json"
    if not path.exists():
        request = urllib.request.Request(API.format(surah=surah), headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=60) as response:
            path.write_bytes(response.read())
        time.sleep(0.05)
    return json.loads(path.read_text(encoding="utf-8"))


def plain(text: str) -> str:
    text = re.sub(r"<br\s*/?>|</p>", "\n", text)
    text = re.sub(r"<[^>]+>", "", text)
    return html.unescape(text).strip()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", default=".cache/tafsir-muyassar", type=pathlib.Path)
    arguments = parser.parse_args()
    cache = arguments.cache if arguments.cache.is_absolute() else ROOT / arguments.cache
    cache.mkdir(parents=True, exist_ok=True)

    counts = [len(s["verses"]) for s in json.loads(TEXT.read_text(encoding="utf-8"))["surahs"]]
    surahs = []
    for number, count in enumerate(counts, start=1):
        passages = [""] * count
        for entry in fetch(number, cache)["tafsirs"]:
            surah, ayah = (int(part) for part in entry["verse_key"].split(":"))
            if surah != number or not 1 <= ayah <= count:
                print(f"  FAIL {entry['verse_key']} is not an ayah of surah {number}")
                return 1
            passages[ayah - 1] = plain(entry["text"])
        if not passages[0]:
            print(f"  FAIL surah {number} has no passage for its first ayah")
            return 1
        surahs.append(passages)
    written = sum(1 for s in surahs for p in s if p)
    # al-Muyassar explains some runs of ayahs together: the run's first ayah carries the passage and the
    # rest are empty, so fewer passages than ayahs is expected. Far fewer means a broken feed.
    if written < 5000:
        print(f"  FAIL only {written} passages")
        return 1
    table = {
        "id": "muyassar",
        "name": "التفسير الميسر",
        "author": "نخبة من العلماء، مجمع الملك فهد لطباعة المصحف الشريف",
        "source": "https://api.quran.com/api/v4/tafsirs/16",
        "license": "King Fahd Glorious Quran Printing Complex; distributed free of charge",
        "surahs": surahs,
    }
    body = json.dumps(table, ensure_ascii=False, separators=(",", ":")) + "\n"
    ASSET.write_text(body, encoding="utf-8")
    print(f"wrote {ASSET.relative_to(ROOT)}: {written} passages, {len(body.encode()) // 1024} KB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
