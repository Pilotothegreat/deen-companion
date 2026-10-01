#!/usr/bin/env python3
"""Generates the mushaf's line breaks: where the printed page ends each of its fifteen lines.

The King Fahd Complex layout says which line each word sits on. Since 2.1 the app's Arabic is the
Complex's own text, so the print's words and the app's words are the same words: each ayah's text,
split on its spaces, must equal the print's word list for that ayah exactly, character for character,
or nothing is written.

That exactness is the point. 2.0 joined this layout to Tanzil's differently-spaced text by matching
letter skeletons, stopped each match as soon as the letters agreed, and so glued every trailing pause
mark — which has no letters — to the start of the *next* word. 4,361 words began with an orphan mark
that floated between words. Comparing whole words rules that out.

What is written is small. The words run in one global order — surah 1 to 114, ayah 1 to n, word 1 to
m — so a line needs only the index of its last word and what kind of line it is.

Usage:
    python3 scripts/quran/build_lines.py [--cache DIR]

Writes:
    app/src/main/assets/mushaf-lines.json        the table the app reads
    app/src/test/resources/mushaf-lines.json     the same table, for the test to compare against

Sources:
    app/src/main/assets/quran-ar.json                     the KFGQPC text, from build_quran.py
    https://api.quran.com/api/v4/verses/by_page/{1..604}  KFGQPC 1405H line numbers
"""
import argparse
import hashlib
import json
import pathlib
import sys
import time
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
TEXT = ROOT / "app/src/main/assets/quran-ar.json"
ASSET = ROOT / "app/src/main/assets/mushaf-lines.json"
GOLDEN = ROOT / "app/src/test/resources/mushaf-lines.json"

API = (
    "https://api.quran.com/api/v4/verses/by_page/{page}"
    "?words=true&word_fields=line_number,text_qpc_hafs&per_page=60"
)
USER_AGENT = "bilal-build-script"

PAGES = 604
LINES_PER_PAGE = 15
AYAHS = 6236
# Every surah is announced by a band carrying its name. Only 112 get a Basmala line under it:
# al-Fatihah because its Basmala is its first ayah, at-Tawbah because it opens without one.
BANNERS = 114
BASMALAS = 112

# The line kinds, as the app reads them.
AYAH_LINE = 0
CENTRED_LINE = 1
SURAH_HEADER = 2
BASMALA = 3
BLANK = 4

# The two opening pages are set in a smaller decorated frame, every line centred.
OPENING_PAGES = (1, 2)

# Elsewhere the print centres only these lines, the short closing ayahs at the very end of the mushaf;
# every other line, however short, is stretched to both margins. Which lines is the print's own choice,
# not a rule of width — al-Falaq's last line is centred and al-Ikhlas's, barely longer, is not — so it
# is read from the printed pages by find_centred_lines.py, which scans all 604 of them.
CENTRED_LINES = {
    600: (9,),  # 101 al-Qari'ah
    602: (5, 15),  # 106 Quraysh, 108 al-Kawthar
    603: (10,),  # 110 an-Nasr
    604: (9, 14, 15),  # 113 al-Falaq, 114 an-Nas
}


def fetch_page(page: int, cache: pathlib.Path) -> dict:
    path = cache / f"page-{page:03d}.json"
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8"))
    request = urllib.request.Request(API.format(page=page), headers={"User-Agent": USER_AGENT})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                body = response.read().decode("utf-8")
            path.write_text(body, encoding="utf-8")
            return json.loads(body)
        except (urllib.error.URLError, TimeoutError) as error:
            if attempt == 3:
                raise
            print(f"  page {page}: {error}; retrying")
            time.sleep(2 * (attempt + 1))
    raise AssertionError("unreachable")


def load_text():
    """The app's words per ayah, and each ayah's first index in the global word order."""
    document = json.loads(TEXT.read_text(encoding="utf-8"))
    words: dict[tuple[int, int], list[str]] = {}
    first: dict[tuple[int, int], int] = {}
    last_word_of_surah: dict[int, int] = {}
    cursor = 0
    for surah in document["surahs"]:
        for number, verse in enumerate(surah["verses"], start=1):
            parts = verse.split(" ")
            words[(surah["id"], number)] = parts
            first[(surah["id"], number)] = cursor
            cursor += len(parts)
        last_word_of_surah[surah["id"]] = cursor - 1
    assert len(words) == AYAHS, f"expected {AYAHS} ayahs, found {len(words)}"
    return words, first, last_word_of_surah, cursor


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", default=".cache/mushaf-lines-qpc", type=pathlib.Path)
    arguments = parser.parse_args()
    cache = arguments.cache if arguments.cache.is_absolute() else ROOT / arguments.cache
    cache.mkdir(parents=True, exist_ok=True)

    words_of, first_of, _, total_words = load_text()

    ends: list[list[int]] = []
    kinds: list[list[int]] = []
    opens_at: dict[int, tuple[int, int]] = {}
    joins: list[int] = []
    cursor = -1

    print(f"reading {PAGES} pages")
    for page in range(1, PAGES + 1):
        per_line: dict[int, int] = {}
        for verse in fetch_page(page, cache)["verses"]:
            surah, ayah = (int(part) for part in verse["verse_key"].split(":"))
            printed = [w for w in verse["words"] if w["char_type_name"] == "word"]
            ours = words_of[(surah, ayah)]
            base = first_of[(surah, ayah)]
            placed = align(ours, printed)
            if placed is None:
                print(f"  {surah}:{ayah}: the print's words do not match the text")
                print(f"    print: {[w['text_qpc_hafs'] for w in printed]}")
                print(f"    text:  {ours}")
                return 1
            for token, line, joined in placed:
                index = base + token
                if joined:
                    joins.append(index)
                per_line[line] = max(per_line.get(line, -1), index)
            if ayah == 1:
                opens_at.setdefault(surah, (page, min(w["line_number"] for w in verse["words"])))

        page_ends, page_kinds = [], []
        for line in range(1, LINES_PER_PAGE + 1):
            if line in per_line:
                cursor = per_line[line]
            page_ends.append(cursor)
            page_kinds.append(AYAH_LINE if line in per_line else BLANK)
        ends.append(page_ends)
        kinds.append(page_kinds)
        if page % 100 == 0:
            print(f"  {page}/{PAGES}")
        time.sleep(0.03)

    owners = [[0] * LINES_PER_PAGE for _ in range(PAGES)]
    label_ornaments(kinds, owners, opens_at)
    if not mark_centred_lines(kinds) or not check(ends, kinds, owners, total_words):
        return 1

    table = {
        "edition": "KFGQPC Madinah Mushaf, 1405H print (15 lines per page)",
        "pages": PAGES,
        "linesPerPage": LINES_PER_PAGE,
        "tokens": total_words,
        "ends": ends,
        "kinds": kinds,
        "owners": owners,
        "joins": sorted(joins),
    }
    body = json.dumps(table, separators=(",", ":")) + "\n"
    ASSET.write_text(body, encoding="utf-8")
    GOLDEN.parent.mkdir(parents=True, exist_ok=True)
    GOLDEN.write_text(body, encoding="utf-8")
    digest = hashlib.sha256(body.encode("utf-8")).hexdigest()
    print(f"wrote {ASSET.relative_to(ROOT)} ({len(body) // 1024} KB, {len(joins)} joined tokens, sha256 {digest[:16]})")
    return 0


def align(tokens: list[str], printed: list[dict]):
    """
    Places each text token on the line the print puts it, or returns None if they do not match.

    The per-word feed follows quran.com's word-by-word segmentation, which is almost but not quite the
    text's spacing, in both directions: it joins two tokens into one word (بَعۡدَ مَا in 2:181) and splits
    one token into two (لَّوۡمَا in 15:7). Both are matched exactly, character for character — the only
    tolerated difference is the no-break space inside "۞ word" — and a split token whose pieces the
    print puts on different lines fails, because a token cannot sit on two lines.

    Returns (token index, line, joins the token before) for every token.
    """
    def bare(text: str) -> str:
        # Whitespace is the only thing the two disagree on: the no-break space in "۞ word", and the
        # space the feed puts before a trailing ۩ that the text attaches (17:109). Every letter and
        # every mark must still match.
        return text.replace(" ", "").replace(" ", "")

    out = []
    i = j = 0
    while i < len(tokens) and j < len(printed):
        token, word = bare(tokens[i]), bare(printed[j]["text_qpc_hafs"])
        line = printed[j]["line_number"]
        if token == word:
            out.append((i, line, False))
            i, j = i + 1, j + 1
        elif word.startswith(token):
            # The feed joins tokens: take them until they are the word.
            span, joined = 1, token
            while joined != word and i + span < len(tokens) and len(joined) < len(word):
                joined += bare(tokens[i + span])
                span += 1
            if joined != word:
                return None
            out.extend((i + k, line, k > 0) for k in range(span))
            i, j = i + span, j + 1
        elif token.startswith(word):
            # The feed splits a token: take words until they are the token, all on one line.
            span, pieces, lines = 1, word, {line}
            while pieces != token and j + span < len(printed) and len(pieces) < len(token):
                pieces += bare(printed[j + span]["text_qpc_hafs"])
                lines.add(printed[j + span]["line_number"])
                span += 1
            if pieces != token or len(lines) != 1:
                return None
            out.append((i, line, False))
            i, j = i + 1, j + span
        else:
            return None
    return out if i == len(tokens) and j == len(printed) else None


def label_ornaments(kinds, owners, opens_at) -> None:
    """
    The blank lines before a surah's first word are its band and, where it has one, its Basmala.

    They are not always on the same page: Yunus opens at the top of page 208, so its band sits on the
    last line of 207. Walking backwards from the first word handles both without a special case.
    """
    for surah, (page, line) in sorted(opens_at.items()):
        needed = 1 if surah in (1, 9) else 2
        index = (page - 1) * LINES_PER_PAGE + (line - 1)
        placed = 0
        while placed < needed and index > 0:
            index -= 1
            page_index, line_index = divmod(index, LINES_PER_PAGE)
            if kinds[page_index][line_index] != BLANK:
                break
            nearest_the_text = placed == 0 and needed == 2
            kinds[page_index][line_index] = BASMALA if nearest_the_text else SURAH_HEADER
            owners[page_index][line_index] = surah
            placed += 1
        if placed != needed:
            print(f"  surah {surah}: found {placed} ornament lines, expected {needed}")


def mark_centred_lines(kinds) -> bool:
    """Every line of the two opening pages, and the listed closing lines, are centred in the print."""
    for page in OPENING_PAGES:
        kinds[page - 1] = [CENTRED_LINE if kind == AYAH_LINE else kind for kind in kinds[page - 1]]
    ok = True
    for page, lines in CENTRED_LINES.items():
        for line in lines:
            if kinds[page - 1][line - 1] != AYAH_LINE:
                print(f"  FAIL page {page} line {line} is listed as centred but holds no text")
                ok = False
            kinds[page - 1][line - 1] = CENTRED_LINE
    return ok


def check(ends, kinds, owners, total_words: int) -> bool:
    """Everything that must be true before this table is allowed anywhere near the app."""
    ok = True

    def fail(message: str) -> None:
        nonlocal ok
        print(f"  FAIL {message}")
        ok = False

    if len(ends) != PAGES or any(len(p) != LINES_PER_PAGE for p in ends):
        fail("the table is not 604 pages of fifteen lines")
    flat_ends = [e for p in ends for e in p]
    flat_kinds = [k for p in kinds for k in p]
    if any(b < a for a, b in zip(flat_ends, flat_ends[1:])):
        fail("a line goes backwards")
    if flat_ends[-1] != total_words - 1:
        fail(f"the last line ends at word {flat_ends[-1]}, expected {total_words - 1}")
    if flat_kinds.count(SURAH_HEADER) != BANNERS:
        fail(f"{flat_kinds.count(SURAH_HEADER)} surah bands, expected {BANNERS}")
    if flat_kinds.count(BASMALA) != BASMALAS:
        fail(f"{flat_kinds.count(BASMALA)} Basmala lines, expected {BASMALAS}")
    # A blank the labelling could not account for is allowed only at the foot of a page.
    for page_index, page_kinds in enumerate(kinds):
        seen_text = False
        for line_index in range(LINES_PER_PAGE - 1, -1, -1):
            if page_kinds[line_index] != BLANK:
                seen_text = True
            elif seen_text:
                fail(f"page {page_index + 1} line {line_index + 1} is an unexplained blank")
                break
    if ok:
        print(f"checked: {PAGES} pages, {total_words} words, {BANNERS} bands, {BASMALAS} Basmalas")
    return ok


if __name__ == "__main__":
    sys.exit(main())
