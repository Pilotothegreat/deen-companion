#!/usr/bin/env python3
"""Generates the print-exact mushaf: the King Fahd Complex's QCF V2 page glyphs, line by line.

The QCF V2 fonts are the Complex's own drawing of the 1421H Madinah print. Each of the 604 pages has a
font of its own whose glyphs are that page's words, already shaped as the calligrapher wrote them, so a
line set in its page's font looks like the printed line rather than like a font's idea of it. This
table says which glyphs sit on which line, and which ayah each one belongs to.

The 1421H print breaks some lines in other places than the 1405H print the bundled table follows, so
this is a table of its own. Every one of its words is still matched, character for character, to the
app's verified KFGQPC text (the same `align` as build_lines.py), and each line records which of the
text's words it holds: search, copy, share and screen readers keep using that text, never the glyphs.

Usage:
    python3 scripts/quran/build_qcf.py [--cache DIR] [--fonts]

    --fonts also downloads the 604 page fonts and the surah-name font (about 200 MB, cached) to pin
    their sizes and SHA-256 checksums, which the app checks every download against.

Writes:
    app/src/main/assets/mushaf-qcf2.json        the table the app reads
    app/src/test/resources/mushaf-qcf2.json     the same table, for the test to compare against
    app/src/main/assets/qcf2-fonts.json         sizes and checksums of the fonts (with --fonts)

Sources:
    https://api.quran.com/api/v4/verses/by_page/{page}?mushaf=1   QCF V2 glyph codes and line numbers
    https://static.qurancdn.com/fonts/quran/hafs/v2/ttf/p{page}.ttf   the page fonts
"""
import argparse
import concurrent.futures
import hashlib
import json
import pathlib
import sys
import time
import urllib.error
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from build_lines import (  # noqa: E402  (shared with the bundled table, so both match the text the same way)
    AYAHS, BANNERS, BASMALAS, BLANK, BASMALA, SURAH_HEADER, AYAH_LINE, CENTRED_LINE, LINES_PER_PAGE, OPENING_PAGES, PAGES, ROOT,
    align, label_ornaments, load_text,
)

ASSET = ROOT / "app/src/main/assets/mushaf-qcf2.json"
GOLDEN = ROOT / "app/src/test/resources/mushaf-qcf2.json"
FONTS_ASSET = ROOT / "app/src/main/assets/qcf2-fonts.json"

API = (
    "https://api.quran.com/api/v4/verses/by_page/{page}"
    "?mushaf=1&words=true&word_fields=code_v2,line_number,page_number,v2_page,text_qpc_hafs&per_page=60"
)
FONT_URL = "https://static.qurancdn.com/fonts/quran/hafs/v2/ttf/p{page}.ttf"
NAMES_URL = "https://static.qurancdn.com/fonts/quran/surah-names/v2/sura_names.ttf"
USER_AGENT = "bilal-build-script"

# Between words in a line's glyphs. Never a glyph: the QCF codes are in the private-use area.
SEPARATOR = "|"


def fetch(url: str, path: pathlib.Path, binary: bool = False):
    if path.exists():
        data = path.read_bytes()
        return data if binary else json.loads(data.decode("utf-8"))
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(5):
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                data = response.read()
            path.write_bytes(data)
            return data if binary else json.loads(data.decode("utf-8"))
        except (urllib.error.URLError, TimeoutError) as error:
            if attempt == 4:
                raise
            print(f"  {url}: {error}; retrying")
            time.sleep(3 * (attempt + 1))
    raise AssertionError("unreachable")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", default=".cache/qcf2", type=pathlib.Path)
    parser.add_argument("--fonts", action="store_true")
    arguments = parser.parse_args()
    cache = arguments.cache if arguments.cache.is_absolute() else ROOT / arguments.cache
    (cache / "pages").mkdir(parents=True, exist_ok=True)

    words_of, first_of, _, total_words = load_text()
    ayah_index: dict[tuple[int, int], int] = {key: i for i, key in enumerate(sorted(words_of))}

    # Per page, per line: the glyphs, the runs of ayahs they belong to, the text's words they hold.
    glyphs = [["" for _ in range(LINES_PER_PAGE)] for _ in range(PAGES)]
    runs: list[list[list[list[int]]]] = [[[] for _ in range(LINES_PER_PAGE)] for _ in range(PAGES)]
    tokens: list[list[list[int]]] = [[[] for _ in range(LINES_PER_PAGE)] for _ in range(PAGES)]
    kinds = [[BLANK] * LINES_PER_PAGE for _ in range(PAGES)]
    opens_at: dict[int, tuple[int, int]] = {}
    seen_tokens = 0

    # Every word once, with the page and line the print puts it on. The 1421H print lets a few ayahs run
    # over a page break (5:77 starts at the foot of page 120), and the feed then lists such an ayah under
    # both pages, so words are gathered first and each ayah placed whole.
    print(f"reading {PAGES} pages")
    verses: dict[tuple[int, int], dict[int, dict]] = {}
    for page in range(1, PAGES + 1):
        data = fetch(API.format(page=page), cache / "pages" / f"page-{page:03d}.json")
        for verse in data["verses"]:
            key = tuple(int(part) for part in verse["verse_key"].split(":"))
            for word in verse["words"]:
                # Placed on the page the word is drawn on, whichever page's feed listed it.
                drawn_on = word.get("v2_page") or word["page_number"]
                verses.setdefault(key, {})[word["position"]] = word | {"page": drawn_on}
        if page % 100 == 0:
            print(f"  {page}/{PAGES}")
        time.sleep(0.02)
    if len(verses) != AYAHS:
        print(f"  FAIL {len(verses)} ayahs in the feed, expected {AYAHS}")
        return 1

    spanning = 0
    for key in sorted(verses):
        surah, ayah = key
        index = ayah_index[key]
        words = [verses[key][position] for position in sorted(verses[key])]
        if len({w["page"] for w in words}) > 1:
            spanning += 1
        for word in words:
            page, line = word["page"], word["line_number"] - 1
            code = word["code_v2"]
            if not code or SEPARATOR in code:
                print(f"  FAIL {surah}:{ayah} word {word['position']} has no usable glyph")
                return 1
            # Words are kept apart by a separator, not a space: a line is justified by widening the gaps
            # between its words, never inside one, and a word such as «۞ إِنَّ» is two glyphs and a space.
            glyphs[page - 1][line] += (SEPARATOR if glyphs[page - 1][line] else "") + code
            kinds[page - 1][line] = AYAH_LINE
            line_runs = runs[page - 1][line]
            ends = 1 if word["char_type_name"] == "end" else 0
            if line_runs and line_runs[-1][0] == index and line_runs[-1][2] == 0:
                line_runs[-1][1] += 1
                line_runs[-1][2] = ends
            else:
                line_runs.append([index, 1, ends])
        printed = [w for w in words if w["char_type_name"] == "word"]
        placed = align_pages(words_of[key], printed)
        if placed is None:
            print(f"  FAIL {surah}:{ayah}: the print's words do not match the text")
            return 1
        base = first_of[key]
        for token, (page, line) in placed:
            tokens[page - 1][line - 1].append(base + token)
            seen_tokens += 1
        if ayah == 1:
            first_word = words[0]
            opens_at[surah] = (first_word["page"], first_word["line_number"])
    print(f"  {spanning} ayahs run over a page break")

    owners = [[0] * LINES_PER_PAGE for _ in range(PAGES)]
    label_ornaments(kinds, owners, opens_at)
    # The two opening pages are centred, line by line, inside their own ornament. Elsewhere the reader
    # centres a line by its width, measured in its page's font: the 1405H list of centred lines that
    # build_lines.py keeps does not fit the 1421H print's line breaks.
    for page in OPENING_PAGES:
        kinds[page - 1] = [CENTRED_LINE if kind == AYAH_LINE else kind for kind in kinds[page - 1]]

    lines_out = []
    for p in range(PAGES):
        page_lines = []
        for l in range(LINES_PER_PAGE):
            held = tokens[p][l]
            first, last = (min(held), max(held)) if held else (-1, -1)
            # [kind, owner, glyphs (words separated by SEPARATOR), first word of the text, last word of the text,
            #  runs as flat (ayah, word count, ends here) triples; a run that ends here closes on its rosette]
            page_lines.append([kinds[p][l], owners[p][l], glyphs[p][l], first, last, [n for run in runs[p][l] for n in run]])
        lines_out.append(page_lines)

    if not check(lines_out, total_words, seen_tokens):
        return 1

    table = {
        "edition": "KFGQPC Madinah Mushaf, 1421H print, QCF V2 page glyphs (15 lines per page)",
        "pages": PAGES,
        "linesPerPage": LINES_PER_PAGE,
        "ayahs": AYAHS,
        "tokens": total_words,
        "lines": lines_out,
    }
    body = json.dumps(table, separators=(",", ":"), ensure_ascii=False) + "\n"
    ASSET.write_text(body, encoding="utf-8")
    GOLDEN.parent.mkdir(parents=True, exist_ok=True)
    GOLDEN.write_text(body, encoding="utf-8")
    print(f"wrote {ASSET.relative_to(ROOT)} ({len(body.encode()) // 1024} KB)")

    if arguments.fonts:
        return pin_fonts(cache)
    return 0


def align_pages(tokens: list[str], printed: list[dict]):
    """build_lines.align, with each word's line made (page, line), so an ayah may cross a page."""
    tagged = [w | {"line_number": (w["page"], w["line_number"])} for w in printed]
    placed = align(tokens, tagged)
    return None if placed is None else [(token, where) for token, where, _ in placed]


def pin_fonts(cache: pathlib.Path) -> int:
    folder = cache / "fonts"
    folder.mkdir(parents=True, exist_ok=True)

    def one(page: int):
        data = fetch(FONT_URL.format(page=page), folder / f"p{page}.ttf", binary=True)
        return page, len(data), hashlib.sha256(data).hexdigest()

    print("downloading the 604 page fonts")
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        results = sorted(pool.map(one, range(1, PAGES + 1)))
    names = fetch(NAMES_URL, folder / "sura_names.ttf", binary=True)
    fonts = {
        "source": "https://static.qurancdn.com/fonts/quran/hafs/v2/ttf/",
        "pages": [[size, digest] for _, size, digest in results],
        "surahNames": [len(names), hashlib.sha256(names).hexdigest()],
    }
    FONTS_ASSET.write_text(json.dumps(fonts, separators=(",", ":")) + "\n", encoding="utf-8")
    total = sum(size for _, size, _ in results)
    print(f"wrote {FONTS_ASSET.relative_to(ROOT)}: {PAGES} fonts, {total / 1e6:.0f} MB in all")
    return 0


def check(lines_out, total_words: int, seen_tokens: int) -> bool:
    ok = True

    def fail(message: str) -> None:
        nonlocal ok
        print(f"  FAIL {message}")
        ok = False

    if len(lines_out) != PAGES or any(len(p) != LINES_PER_PAGE for p in lines_out):
        fail("the table is not 604 pages of fifteen lines")
    flat = [line for page in lines_out for line in page]
    kinds = [line[0] for line in flat]
    if kinds.count(SURAH_HEADER) != BANNERS:
        fail(f"{kinds.count(SURAH_HEADER)} surah bands, expected {BANNERS}")
    if kinds.count(BASMALA) != BASMALAS:
        fail(f"{kinds.count(BASMALA)} Basmala lines, expected {BASMALAS}")
    ends = sum(line[5][i + 2] for line in flat for i in range(0, len(line[5]), 3))
    if ends != AYAHS:
        fail(f"{ends} ayah endings, expected {AYAHS}")
    if seen_tokens != total_words:
        fail(f"{seen_tokens} words placed, expected {total_words}")
    # The text's words run on, line after line, with nothing skipped and nothing twice.
    cursor = -1
    for line in flat:
        first, last = line[3], line[4]
        if first < 0:
            continue
        if first != cursor + 1:
            fail(f"a line starts at word {first}, expected {cursor + 1}")
            break
        cursor = last
    if cursor != total_words - 1:
        fail(f"the last line ends at word {cursor}, expected {total_words - 1}")
    for line in flat:
        if sum(line[5][i + 1] for i in range(0, len(line[5]), 3)) != (len(line[2].split(SEPARATOR)) if line[2] else 0):
            fail("a line's ayah runs do not cover its words")
            break
    if ok:
        print(f"checked: {PAGES} pages, {total_words} words, {AYAHS} ayah endings, {BANNERS} bands, {BASMALAS} Basmalas")
    return ok


if __name__ == "__main__":
    sys.exit(main())
