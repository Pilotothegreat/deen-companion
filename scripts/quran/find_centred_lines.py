#!/usr/bin/env python3
"""Finds which lines the printed mushaf centres instead of stretching to both margins.

That is the print's own choice, line by line — al-Falaq's last line is centred and al-Ikhlas's, barely
longer, is not — so it is read from the print rather than guessed from widths. Each page of the 1405H
Madinah edition, as Quran for Android serves it, is scanned for the ink of every text line: a stretched
line runs from margin to margin, a centred one leaves wide, equal margins on both sides.

The two opening pages, set in their own narrow frame, are centred throughout and are not listed.

Usage:
    python3 scripts/quran/find_centred_lines.py [--cache DIR]

Prints the lines to copy into CENTRED_LINES in build_lines.py.
"""
import argparse
import json
import pathlib
import time
import urllib.request

import numpy
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
TABLE = ROOT / "app/src/main/assets/mushaf-lines.json"
PAGE_IMAGE = "https://android.quran.com/data/width_1024/page{page:03d}.png"
TEXT_KINDS = (0, 1)
OPENING_PAGES = (1, 2)
# A stretched line leaves 30 to 80 pixels either side on a 1,024-pixel page; a centred one well over 100.
CENTRED_MARGIN = 100


def page_image(page: int, cache: pathlib.Path) -> numpy.ndarray:
    path = cache / f"{page:03d}.png"
    if not path.exists():
        request = urllib.request.Request(PAGE_IMAGE.format(page=page), headers={"User-Agent": "bilal-build-script"})
        with urllib.request.urlopen(request, timeout=60) as response:
            path.write_bytes(response.read())
        time.sleep(0.1)
    return numpy.asarray(Image.open(path).convert("L")) < 110


def margins(ink: numpy.ndarray, line: int) -> tuple[int, int] | None:
    """The blank space left and right of a line's ink, from the middle half of its slot."""
    height, width = ink.shape
    slot = height / 15
    band = ink[int((line - 1) * slot + slot * 0.25):int((line - 1) * slot + slot * 0.75)]
    columns = numpy.flatnonzero(band.any(axis=0))
    return (int(columns[0]), int(width - 1 - columns[-1])) if columns.size else None


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", default=".cache/mushaf-pages", type=pathlib.Path)
    arguments = parser.parse_args()
    cache = arguments.cache if arguments.cache.is_absolute() else ROOT / arguments.cache
    cache.mkdir(parents=True, exist_ok=True)
    kinds = json.loads(TABLE.read_text(encoding="utf-8"))["kinds"]
    centred: dict[int, list[int]] = {}
    for page in range(1, len(kinds) + 1):
        if page in OPENING_PAGES:
            continue
        ink = page_image(page, cache)
        for line, kind in enumerate(kinds[page - 1], start=1):
            found = margins(ink, line) if kind in TEXT_KINDS else None
            if found and min(found) > CENTRED_MARGIN:
                centred.setdefault(page, []).append(line)
                print(f"  page {page} line {line}: margins {found[0]} | {found[1]}")
    print("CENTRED_LINES = {")
    for page, lines in centred.items():
        print(f"    {page}: {tuple(lines)},")
    print("}")


if __name__ == "__main__":
    main()
