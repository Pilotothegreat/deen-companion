#!/usr/bin/env python3
"""Builds the Quran assets from the King Fahd Complex's own text.

Until 2.1 the Arabic was Tanzil's Uthmani text, drawn with KFGQPC's Uthmanic Hafs font. The two do not
agree on how the Quranic marks are encoded, and the disagreement is visible: in that font Tanzil's
silent-letter circle (U+06DF, nearly four thousand times) is a spacing glyph rather than a mark, so it
drew as a dotted circle standing inside the word, and every sukun came out as that circle instead of
the printed jazm. Rendering the words with HarfBuzz showed it plainly; checking that every codepoint
had a glyph did not, which is how it shipped.

The fix is to draw the font with the text it was made for. KFGQPC's QPC Hafs text, rendered with its
v18 font, puts the silent circle over its letter, sets pause marks on their word, ligates each sajdah
word the way the print does, and draws the numbered ayah rosette itself from a no-break space and the
ayah's digits. The text is used exactly as published; the only thing separated out is that trailing
number, so search, copy and share get the words and the page puts the number back.

The Basmala heading comes from al-Fatihah's first ayah, which is the Basmala, and is written above
every surah except al-Fatihah, where it is ayah 1, and at-Tawbah, which opens without it.

Usage:
    python3 scripts/quran/build_quran.py [--cache DIR]

Writes:
    app/src/main/assets/quran-ar.json            the text with surah metadata
    app/src/main/assets/quran-tr-clearquran.json The Clear Quran (Talal Itani)
    app/src/test/resources/quran-hashes.json     per-surah checksums the integrity test asserts

Sources:
    Quran text      https://api.quran.com/api/v4/quran/verses/qpc_hafs  (KFGQPC QPC Hafs)
    Translation     https://tanzil.net/trans/en.itani  (The Clear Quran, CC BY-ND 4.0)
    Surah metadata  https://tanzil.net/res/text/metadata/quran-data.xml  (CC BY 3.0)
"""
import argparse
import hashlib
import json
import pathlib
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app/src/main/assets"
TEST_RESOURCES = ROOT / "app/src/test/resources"

SOURCES = {
    "qpc_hafs.json": "https://api.quran.com/api/v4/quran/verses/qpc_hafs",
    "itani.txt": "https://tanzil.net/trans/en.itani",
    "quran-data.xml": "https://tanzil.net/res/text/metadata/quran-data.xml",
}

TEXT_LICENCE = {
    "name": "King Fahd Glorious Quran Printing Complex — Uthmanic Hafs (QPC)",
    "source": "https://qurancomplex.gov.sa",
    "license": "KFGQPC",
    "terms": "Published by the King Fahd Glorious Quran Printing Complex for the Madinah Mushaf. "
             "Distributed free of cost and unmodified; changing the text is not allowed.",
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

# The ayah number the text carries at its end: a no-break space and Arabic-Indic digits. One ayah
# (2:72) has an ordinary space there instead, so either is accepted; the page always puts the number
# back after a no-break space, which is what keeps it on the line of the word it closes.
AYAH_NUMBER = re.compile("[  ]([٠-٩]+)$")
ARABIC_INDIC = str.maketrans("٠١٢٣٤٥٦٧٨٩", "0123456789")


def fetch(cache: pathlib.Path, name: str) -> pathlib.Path:
    cache.mkdir(parents=True, exist_ok=True)
    path = cache / name
    if not path.exists():
        print(f"downloading {name}")
        request = urllib.request.Request(SOURCES[name], headers={"User-Agent": "bilal-build-script"})
        with urllib.request.urlopen(request, timeout=180) as response:
            path.write_bytes(response.read())
    return path


def read_arabic(path: pathlib.Path) -> dict[int, list[str]]:
    """The ayahs of each surah, in order, with their trailing number checked and removed."""
    verses: dict[int, dict[int, str]] = {}
    for verse in json.loads(path.read_text(encoding="utf-8"))["verses"]:
        surah, ayah = (int(part) for part in verse["verse_key"].split(":"))
        text = verse["text_qpc_hafs"]
        match = AYAH_NUMBER.search(text)
        if match is None or int(match.group(1).translate(ARABIC_INDIC)) != ayah:
            raise ValueError(f"{surah}:{ayah} does not end with its own number")
        verses.setdefault(surah, {})[ayah] = text[: match.start()]
    return {surah: [by_ayah[a] for a in sorted(by_ayah)] for surah, by_ayah in verses.items()}


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
    parser.add_argument("--cache", default="/tmp/bilal-quran-qpc", help="where downloads are kept")
    args = parser.parse_args()
    cache = pathlib.Path(args.cache)

    arabic = read_arabic(fetch(cache, "qpc_hafs.json"))
    english = read_translation(fetch(cache, "itani.txt"))
    suras = ET.parse(fetch(cache, "quran-data.xml")).getroot().find("suras")
    counts = {int(s.get("index")): int(s.get("ayas")) for s in suras}
    order = {int(s.get("index")): int(s.get("order")) for s in suras}
    names = {s["id"]: s for s in json.loads((ROOT / "scripts/quran/surahs.json").read_text(encoding="utf-8"))}

    total = sum(len(v) for v in arabic.values())
    if total != 6236 or len(english) != 6236:
        print(f"expected 6236 ayahs, got {total} Arabic and {len(english)} translated", file=sys.stderr)
        return 1

    basmala = arabic[1][0]
    # No surah but al-Fatihah may carry the Basmala inside its first ayah: it is a heading everywhere
    # else, and printing it from the heading and the ayah would show it twice.
    doubled = [s for s in range(2, 115) if arabic[s][0].startswith(basmala)]
    if doubled:
        print(f"the Basmala is inside ayah 1 of {doubled}", file=sys.stderr)
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
        if index not in (1, 9):
            surah["bismillah"] = basmala
        surahs.append(surah)
        translated.append([english[(index, ayah)] for ayah in range(1, counts[index] + 1)])
        hashes[str(index)] = hashlib.sha256("\n".join(verses).encode("utf-8")).hexdigest()

    write_json(ASSETS / "quran-ar.json", {"source": TEXT_LICENCE, "surahs": surahs})
    write_json(ASSETS / "quran-tr-clearquran.json", {**TRANSLATION, "verses": translated})
    write_json(TEST_RESOURCES / "quran-hashes.json", hashes)
    headings = sum(1 for s in surahs if "bismillah" in s)
    print(f"{total} ayahs, {len(surahs)} surahs, {headings} Basmala headings")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
