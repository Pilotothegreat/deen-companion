#!/usr/bin/env python3
"""Frames the raw store screenshots from StoreScreenshotTest with captions for Google Play.

Run from the repository root after
    ./gradlew testDebugUnitTest --tests '*StoreScreenshotTest*'
It writes 1080 x 1920 PNGs to fastlane/metadata/android/<locale>/images/phoneScreenshots/.
Needs Inkscape and the Reem Kufi and Readex Pro fonts (both on Google Fonts).
"""
import html
import pathlib
import subprocess
import tempfile

RAW = pathlib.Path("app/build/store-screenshots")
OUT = pathlib.Path("fastlane/metadata/android")
LOCALES = {"en": "en-US", "ar": "ar"}

WIDTH, HEIGHT = 1080, 1920
SHOT_W = 820
SHOT_H = round(SHOT_W * 2133 / 1200)  # the raw frames are 1200 x 2133
SHOT_X = (WIDTH - SHOT_W) // 2
SHOT_Y = 400

# (raw screenshot suffix, headline, subline) in store order.
CAPTIONS = {
    "en": [
        ("1-today", "Every prayer, on time", "A live countdown with adhan and iqama alerts"),
        ("3-athkar", "Athkar for every moment", "Morning, evening, after prayer and before sleep"),
        ("4-athkar-session", "Count with one tap", "With each dhikr's virtue and source"),
        ("2-today-daily", "A verse and a hadith each day", "The verse opens right in the mushaf"),
        ("5-reader", "The Madinah mushaf", "Uthmanic script, translation and recitation"),
        ("6-hadith", "Six hadith collections", "Search, grades and favourites"),
    ],
    "ar": [
        ("1-today", "كل صلاة في وقتها", "عدّ تنازلي مباشر وتنبيهات للأذان والإقامة"),
        ("3-athkar", "أذكار لكل وقت", "الصباح والمساء وبعد الصلاة وقبل النوم"),
        ("4-athkar-session", "عُدّ بلمسة واحدة", "مع فضل كل ذكر ومصدره"),
        ("2-today-daily", "آية وحديث كل يوم", "وتفتح الآية في المصحف مباشرة"),
        ("5-reader", "مصحف المدينة", "بالرسم العثماني مع الترجمة والتلاوة"),
        ("6-hadith", "الكتب الستة", "بحث ودرجات ومفضلة"),
    ],
    # The Qibla screen is left out: without a real compass sensor it only shows the "no compass" notice.
}


def frame(shot: pathlib.Path, title: str, subline: str) -> str:
    return f"""<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
     width="{WIDTH}" height="{HEIGHT}" viewBox="0 0 {WIDTH} {HEIGHT}">
  <defs>
    <clipPath id="screen"><rect x="{SHOT_X}" y="{SHOT_Y}" width="{SHOT_W}" height="{SHOT_H}" rx="44"/></clipPath>
  </defs>
  <rect width="{WIDTH}" height="{HEIGHT}" fill="#156C44"/>
  <circle cx="{WIDTH // 2}" cy="112" r="9" fill="#F2C572"/>
  <text x="{WIDTH // 2}" y="214" text-anchor="middle" font-family="Reem Kufi" font-weight="700"
        font-size="72" fill="#FFF8F3">{html.escape(title)}</text>
  <text x="{WIDTH // 2}" y="292" text-anchor="middle" font-family="Readex Pro" font-weight="400"
        font-size="34" fill="#FFF8F3" fill-opacity="0.85">{html.escape(subline)}</text>
  <image x="{SHOT_X}" y="{SHOT_Y}" width="{SHOT_W}" height="{SHOT_H}" preserveAspectRatio="xMidYMid slice"
         xlink:href="{shot.resolve().as_uri()}" clip-path="url(#screen)"/>
  <rect x="{SHOT_X}" y="{SHOT_Y}" width="{SHOT_W}" height="{SHOT_H}" rx="44" fill="none"
        stroke="#FFF8F3" stroke-opacity="0.25" stroke-width="3"/>
</svg>
"""


def main() -> None:
    with tempfile.TemporaryDirectory() as tmp:
        for prefix, locale in LOCALES.items():
            target = OUT / locale / "images" / "phoneScreenshots"
            target.mkdir(parents=True, exist_ok=True)
            for old in target.glob("*.png"):
                old.unlink()
            index = 0
            for suffix, title, subline in CAPTIONS[prefix]:
                shot = RAW / f"{prefix}-{suffix}.png"
                if not shot.exists():
                    print(f"skipped {shot} (missing)")
                    continue
                index += 1
                source = pathlib.Path(tmp) / f"{prefix}-{index}.svg"
                source.write_text(frame(shot, title, subline))
                png = target / f"{index}.png"
                subprocess.run(
                    ["inkscape", str(source), "--export-type=png", "--export-background-opacity=1",
                     f"--export-filename={png}", "-w", str(WIDTH), "-h", str(HEIGHT)],
                    check=True, capture_output=True,
                )
                print(f"wrote {png}")


if __name__ == "__main__":
    main()
