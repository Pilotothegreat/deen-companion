#!/usr/bin/env python3
"""Builds the bundled eclipse table from NASA's decade tables.

Eclipses are predictable centuries ahead, so there is no reason for the app to ask a server about
them — and every reason not to, since the prayer is tied to seeing the eclipse and a phone with no
signal should still know one is coming.

Usage:
    python3 scripts/eclipses/build_eclipses.py [--cache DIR]

Writes:
    app/src/main/assets/eclipses.json

Source:
    https://eclipse.gsfc.nasa.gov  (NASA/GSFC eclipse predictions by Fred Espenak; public domain)
"""
import argparse
from datetime import datetime, timedelta, timezone
import json
import pathlib
import re
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSET = ROOT / "app/src/main/assets/eclipses.json"
BASE = "https://eclipse.gsfc.nasa.gov"
DECADES = (2021, 2031, 2041)
FIRST_YEAR = 2026
LAST_YEAR = 2045

MONTHS = {m: i + 1 for i, m in enumerate(
    ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
)}

ROW = re.compile(r"<tr[^>]*>(.*?)</tr>", re.S)
CELL = re.compile(r"<td[^>]*>(.*?)</td>", re.S)
DATE = re.compile(r"(\d{4})\s+([A-Z][a-z]{2})\s+(\d{1,2})")
TIME = re.compile(r"(\d{1,2}):(\d{2}):(\d{2})")


def fetch(cache: pathlib.Path, kind: str, decade: int) -> str:
    cache.mkdir(parents=True, exist_ok=True)
    name = f"{kind}decade{decade}.html"
    path = cache / name
    if not path.exists():
        print(f"downloading {name}")
        url = f"{BASE}/{kind}decade/{name}"
        request = urllib.request.Request(url, headers={"User-Agent": "bilal-build-script"})
        with urllib.request.urlopen(request, timeout=120) as response:
            path.write_bytes(response.read())
    return path.read_text(encoding="utf-8", errors="replace")


def text(html: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html)).strip()


def parse(html: str, kind: str) -> list[dict]:
    out = []
    for row in ROW.findall(html):
        cells = [text(c) for c in CELL.findall(row)]
        if len(cells) < 7:
            continue
        date = DATE.search(cells[0])
        time = TIME.search(cells[1])
        if not date or not time:
            continue
        year = int(date.group(1))
        if not FIRST_YEAR <= year <= LAST_YEAR:
            continue
        # NASA rounds seconds, so the tables contain times such as 17:44:60. datetime normalises it.
        at = datetime(
            year, MONTHS[date.group(2)], int(date.group(3)),
            int(time.group(1)), int(time.group(2)), tzinfo=timezone.utc,
        ) + timedelta(seconds=int(time.group(3)))
        out.append({
            "kind": kind,
            # Greatest eclipse, in UTC. NASA publishes Terrestrial Dynamical Time, which runs about
            # 70 seconds ahead; that is far below the precision anything here claims.
            "at": at.strftime("%Y-%m-%dT%H:%M:%SZ"),
            "type": cells[2].split()[0].lower(),
            "magnitude": float(cells[4]) if re.fullmatch(r"[\d.]+", cells[4]) else 0.0,
            "regions": cells[6].replace("[", "(").replace("]", ")"),
        })
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", default="/tmp/bilal-eclipses")
    args = parser.parse_args()
    cache = pathlib.Path(args.cache)

    eclipses = []
    for decade in DECADES:
        eclipses += parse(fetch(cache, "SE", decade), "solar")
        eclipses += parse(fetch(cache, "LE", decade), "lunar")
    eclipses.sort(key=lambda e: e["at"])

    if not eclipses:
        print("no eclipses parsed; the source layout has changed", file=sys.stderr)
        return 1
    solar = sum(1 for e in eclipses if e["kind"] == "solar")
    # Roughly two to five of each a year; far outside that means the parse went wrong.
    years = LAST_YEAR - FIRST_YEAR + 1
    if not years * 2 <= len(eclipses) <= years * 8:
        print(f"{len(eclipses)} eclipses over {years} years looks wrong", file=sys.stderr)
        return 1

    ASSET.write_text(json.dumps({
        "source": {
            "name": "NASA/GSFC eclipse predictions",
            "credit": "Fred Espenak, NASA's Goddard Space Flight Center",
            "source": BASE,
        },
        "from": FIRST_YEAR,
        "to": LAST_YEAR,
        "eclipses": eclipses,
    }, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"wrote {ASSET.relative_to(ROOT)}: {solar} solar, {len(eclipses) - solar} lunar")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
