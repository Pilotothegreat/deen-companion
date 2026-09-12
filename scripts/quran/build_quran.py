#!/usr/bin/env python3
"""Builds the Quran assets from Tanzil.

The text must reach the screen exactly as Tanzil publishes it: their licence forbids modifying it,
and so does respect for what it is. Earlier versions of this app rewrote hamza sequences at load
time to paper over a font problem, which silently deleted 15,607 vowel marks.

The Arabic comes from Tanzil's XML rather than their plain text, because the plain text glues the
Basmala onto the first ayah of every surah but at-Tawbah. The mushaf writes it as a heading above
the surah, and the translation files keep it out of ayah 1, so the XML is the only form where the
two line up. Its `bismillah` attribute becomes the heading, and is absent exactly where the mushaf
has none: al-Fatihah, where it is ayah 1, and at-Tawbah, which opens without it.

Usage:
    python3 scripts/quran/build_quran.py [--cache DIR]

Writes:
    app/src/main/assets/quran-ar.json            the Uthmani text with surah metadata
    app/src/main/assets/quran-tr-clearquran.json The Clear Quran (Talal Itani)
    app/src/test/resources/quran-hashes.json     per-surah checksums the integrity test asserts

Sources:
    Quran text      https://tanzil.net  (Tanzil Uthmani 1.1, CC BY 3.0, verbatim only)
    Translation     https://tanzil.net/trans/en.itani  (The Clear Quran, CC BY-ND 4.0)
"""
import argparse
import hashlib
import json
import pathlib
import sys
import urllib.request
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app/src/main/assets"
TEST_RESOURCES = ROOT / "app/src/test/resources"

SOURCES = {
    "uthmani.xml": "https://tanzil.net/pub/download/index.php?quranType=uthmani&outType=xml&agree=true&marks=true&sajdah=true",
    "itani.txt": "https://tanzil.net/trans/en.itani",
    "quran-data.xml": "https://tanzil.net/res/text/metadata/quran-data.xml",
}

TEXT_LICENCE = {
    "name": "Tanzil Quran Text (Uthmani, version 1.1)",
    "source": "https://tanzil.net",
    "license": "CC BY 3.0",
    "terms": "Verbatim copies may be distributed with the source indicated; changing the text is not allowed.",
}
TRANSLATION = {
    "id": "clearquran",
    "name": "The Clear Quran",
    "translator": "Talal Itani",
    "language": "en",
    "license": "CC BY-ND 4.0",
    "source": "https://clearquran.com",
    "attribution": "Translation by Talal Itani, ClearQuran.com",
}


def fetch(cache: pathlib.Path, name: str) -> pathlib.Path:
    cache.mkdir(parents=True, exist_ok=True)
    path = cache / name
    if not path.exists():
        print(f"downloading {name}")
        request = urllib.request.Request(SOURCES[name], headers={"User-Agent": "bilal-build-script"})
        with urllib.request.urlopen(request, timeout=120) as response:
            path.write_bytes(response.read())
    return path


def read_arabic(path: pathlib.Path) -> tuple[dict[int, list[str]], dict[int, str]]:
    """Returns the ayahs of each surah and, where the mushaf has one, its Basmala heading."""
    verses: dict[int, list[str]] = {}
    bismillah: dict[int, str] = {}
    for sura in ET.parse(path).getroot().findall("sura"):
        index = int(sura.get("index"))
        ayas = sura.findall("aya")
        verses[index] = [aya.get("text") for aya in ayas]
        heading = ayas[0].get("bismillah")
        if heading:
            bismillah[index] = heading
    return verses, bismillah


def read_translation(path: pathlib.Path) -> dict[tuple[int, int], str]:
    verses: dict[tuple[int, int], str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        parts = line.split("|", 2)
        if len(parts) != 3 or not parts[0].isdigit():
            continue
        verses[(int(parts[0]), int(parts[1]))] = parts[2].strip()
    return verses


def write_json(path: pathlib.Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"wrote {path.relative_to(ROOT)} ({path.stat().st_size / 1024:.0f} KB)")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", default="/tmp/bilal-quran", help="where downloads are kept")
    args = parser.parse_args()
    cache = pathlib.Path(args.cache)

    arabic, bismillah = read_arabic(fetch(cache, "uthmani.xml"))
    english = read_translation(fetch(cache, "itani.txt"))
    suras = ET.parse(fetch(cache, "quran-data.xml")).getroot().find("suras")
    counts = {int(s.get("index")): int(s.get("ayas")) for s in suras}
    order = {int(s.get("index")): int(s.get("order")) for s in suras}
    names = {s["id"]: s for s in json.loads((ROOT / "scripts/quran/surahs.json").read_text(encoding="utf-8"))}

    total = sum(len(v) for v in arabic.values())
    if total != 6236 or len(english) != 6236:
        print(f"expected 6236 ayahs, got {total} Arabic and {len(english)} translated", file=sys.stderr)
        return 1
    # The Basmala is a heading everywhere except al-Fatihah, where it is ayah 1, and at-Tawbah.
    if sorted(set(range(1, 115)) - bismillah.keys()) != [1, 9]:
        print("unexpected Basmala placement in the source text", file=sys.stderr)
        return 1

    surahs, translated, hashes = [], [], {}
    for index in range(1, 115):
        verses = arabic[index]
        if len(verses) != counts[index]:
            print(f"surah {index}: {len(verses)} ayahs, expected {counts[index]}", file=sys.stderr)
            return 1
        meta = names[index]
        surah = {
            "id": index,
            "name": meta["name"],
            "transliteration": meta["transliteration"],
            "type": meta["type"],
            "revelationOrder": order[index],
            "verses": verses,
        }
        if index in bismillah:
            surah["bismillah"] = bismillah[index]
        surahs.append(surah)
        translated.append([english[(index, ayah)] for ayah in range(1, counts[index] + 1)])
        hashes[str(index)] = hashlib.sha256("\n".join(verses).encode("utf-8")).hexdigest()

    write_json(ASSETS / "quran-ar.json", {"source": TEXT_LICENCE, "surahs": surahs})
    write_json(ASSETS / "quran-tr-clearquran.json", {**TRANSLATION, "verses": translated})
    write_json(TEST_RESOURCES / "quran-hashes.json", hashes)
    print(f"{total} ayahs, {len(surahs)} surahs, {len(bismillah)} Basmala headings")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
