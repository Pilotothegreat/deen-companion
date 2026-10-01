#!/usr/bin/env python3
"""Measures every line of the mushaf in the shipped font, to choose the page's one text size.

A printed mushaf is set at one size on every page. The app does the same: it stretches every justified
line to a fixed measure, and that measure has to be wide enough for the print's longer lines at the
font's natural spacing. This prints the spread of those widths; the app's LINE_EM (MushafPage.kt) is the
99th percentile, so the rare wider line is set slightly smaller instead of every page shrinking for it.

Needs HarfBuzz's hb-shape and fontTools.

Usage:
    python3 scripts/quran/measure_lines.py
"""
import json
import pathlib
import re
import subprocess
import tempfile

from fontTools.ttLib import TTFont

ROOT = pathlib.Path(__file__).resolve().parents[2]
FONT = ROOT / "app/src/main/res/font/uthmanic_hafs.ttf"
TEXT = ROOT / "app/src/main/assets/quran-ar.json"
TABLE = ROOT / "app/src/main/assets/mushaf-lines.json"

AYAH_LINE, CENTRED_LINE = 0, 1
DIGITS = str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩")


def lines():
    """Every text line as the app draws it before stretching: words, and each closing ayah's number."""
    tokens = []
    for surah in json.loads(TEXT.read_text(encoding="utf-8"))["surahs"]:
        for number, verse in enumerate(surah["verses"], start=1):
            parts = verse.split(" ")
            tokens += [(part, number if k == len(parts) - 1 else None) for k, part in enumerate(parts)]
    table = json.loads(TABLE.read_text(encoding="utf-8"))
    joins = set(table["joins"])
    previous = -1
    for page, (ends, kinds) in enumerate(zip(table["ends"], table["kinds"]), start=1):
        for line, (end, kind) in enumerate(zip(ends, kinds), start=1):
            if kind in (AYAH_LINE, CENTRED_LINE):
                words = []
                for index in range(previous + 1, end + 1):
                    text, ayah = tokens[index]
                    word = text + (" " + str(ayah).translate(DIGITS) if ayah else "")
                    if index in joins and words:
                        words[-1] += " " + word
                    else:
                        words.append(word)
                yield page, line, kind, " ".join(words)
            previous = end


def main() -> None:
    upem = TTFont(FONT)["head"].unitsPerEm
    rows = list(lines())
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".txt", delete=False) as handle:
        handle.write("\n".join(row[3] for row in rows) + "\n")
    shaped = subprocess.run(
        ["hb-shape", str(FONT), f"--text-file={handle.name}", "--no-glyph-names", "--no-clusters"],
        capture_output=True, text=True, check=True,
    ).stdout.splitlines()
    assert len(shaped) == len(rows), "hb-shape returned a different number of lines"
    widths = [sum(int(advance) for advance in re.findall(r"\+(-?\d+)", glyphs)) / upem for glyphs in shaped]
    justified = sorted(width for width, row in zip(widths, rows) if row[2] == AYAH_LINE)

    def at(share: float) -> float:
        return justified[min(len(justified) - 1, int(share * len(justified)))]

    print(f"{len(justified)} justified lines, widths in ems at the font's natural spacing:")
    print(f"  min {justified[0]:.2f}  median {at(0.5):.2f}  p95 {at(0.95):.2f}  p99 {at(0.99):.2f}  max {justified[-1]:.2f}")


if __name__ == "__main__":
    main()
