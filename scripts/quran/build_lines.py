#!/usr/bin/env python3
"""Generates the mushaf's line breaks: where the printed page ends each of its fifteen lines.

The app already knew which ayahs belong to which page — that comes from Tanzil's metadata and has
since 1.8.0. What it never had is where the *lines* break inside a page, so the reader poured each
page into one justified paragraph and let the text engine break it wherever the phone's width ran
out. The result reflowed differently on every device and at every font size, stretched half-empty
lines to full width, and looked nothing like the mushaf it claimed to be.

The King Fahd Complex layout says which line each word sits on, and it does not divide the text
into words quite the way Tanzil's spaces do: a pause mark stands alone in Tanzil and sits tight
above its word in the print, and a handful of pairs the print sets as one unit — بَعْدَ مَا, for
instance — Tanzil separates. Rather than guess at a rule, this script takes the grouping from the
print itself: each of its words says how many of Tanzil's tokens it covers. The totals must agree
for all 6,236 ayahs or nothing is written.

What is written is deliberately small. Tanzil's tokens run in one global order — surah 1 to 114,
ayah 1 to n, token 1 to m — so a line needs only the index of its last token in that stream and
what kind of line it is, and the print's grouping is the list of tokens that join the one before
them. Six hundred and four pages of line breaks cost about eighty kilobytes.

Usage:
    python3 scripts/quran/build_lines.py [--cache DIR]

Writes:
    app/src/main/assets/mushaf-lines.json        the table the app reads
    app/src/test/resources/mushaf-lines.json     the same table, for the test to compare against

Sources:
    app/src/main/assets/quran-ar.json                     Tanzil Uthmani text (CC BY 3.0)
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
    "?words=true&word_fields=line_number,text_uthmani&per_page=60"
)
USER_AGENT = "bilal-build-script"

# The two texts spell the same word differently in places — ٱفْتَرَىٰهُ against افْتَرَاهُ — so the
# skeleton folds the variants that differ, exactly as the app's own search normaliser does.
FOLD = {
    0x0671: "\u0627", 0x0649: "\u0627", 0x0622: "\u0627", 0x0623: "\u0627", 0x0625: "\u0627",
    0x0629: "\u0647", 0x0624: "\u0621", 0x0626: "\u0621",
}


# Arabic letters and nothing else: not the tatweel, and not the stray directional marks that turn
# up in the print's own text (العظيم\u200f in 27:26).
KEEP = set(range(0x0621, 0x064B)) - {0x0640}


def letters(text: str) -> str:
    """A word reduced to the skeleton both texts agree on: no marks, no spelling variants."""
    out = []
    for character in text:
        folded = FOLD.get(ord(character), character)
        if ord(folded) in KEEP:
            out.append(folded)
    return "".join(out)


PAGES = 604
LINES_PER_PAGE = 15
AYAHS = 6236
SURAHS = 114
# Every surah gets its name in an ornamental band. Only 112 get a Basmala line under it: al-Fatihah
# because its Basmala is its first ayah, at-Tawbah because it opens without one.
BANNERS = 114
BASMALAS = 112

# The line kinds, as the app reads them. BLANK is a line the print leaves empty and this script
# could not account for — the table is rejected if any survives labelling in the body of a page.
AYAH_LINE = 0
CENTRED_LINE = 1
SURAH_HEADER = 2
BASMALA = 3
BLANK = 4



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
    """Tanzil's text as one token stream: (surah, ayah, tokens) per ayah, plus each ayah's tokens."""
    document = json.loads(TEXT.read_text(encoding="utf-8"))
    stream: list[tuple[int, int, int]] = []
    first: dict[tuple[int, int], int] = {}
    tokens: dict[tuple[int, int], list[str]] = {}
    cursor = 0
    for surah in document["surahs"]:
        for number, verse in enumerate(surah["verses"], start=1):
            parts = verse.split()
            first[(surah["id"], number)] = cursor
            tokens[(surah["id"], number)] = parts
            stream.append((surah["id"], number, len(parts)))
            cursor += len(parts)
    assert len(stream) == AYAHS, f"expected {AYAHS} ayahs, found {len(stream)}"
    return stream, first, tokens


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", default=".cache/mushaf-lines", type=pathlib.Path)
    arguments = parser.parse_args()
    cache = (ROOT / arguments.cache) if not arguments.cache.is_absolute() else arguments.cache
    cache.mkdir(parents=True, exist_ok=True)

    stream, first_word_of, tokens_of = load_text()
    total_words = sum(count for _, _, count in stream)

    # Per page, the last word index on each line and what kind of line it is. A line with no words
    # repeats the previous cursor, which is what makes an ornament recognisable.
    ends: list[list[int]] = []
    kinds: list[list[int]] = []
    # Where each surah's first word lands, so the banner and Basmala can be placed before it.
    opens_at: dict[int, tuple[int, int]] = {}
    # Tokens the print joins to the one before them.
    glue: list[int] = []
    cursor = -1

    print(f"reading {PAGES} pages")
    for page in range(1, PAGES + 1):
        payload = fetch_page(page, cache)
        per_line: dict[int, list[tuple[int, int, int]]] = {}
        for verse in payload["verses"]:
            surah, ayah = (int(part) for part in verse["verse_key"].split(":"))
            base = first_word_of[(surah, ayah)]
            ours = tokens_of[(surah, ayah)]
            position = 0
            for word in verse["words"]:
                if word["char_type_name"] != "word":
                    continue
                # How many of Tanzil's tokens this printed word covers. The two texts space their
                # words differently — the print keeps a pause mark with its word and occasionally
                # splits one of its own — so the span is found by matching bare letters rather than
                # by counting spaces, which neither side agrees on.
                wanted = letters(word["text_uthmani"] or "")
                span = 0
                seen = ""
                while position + span < len(ours) and seen != wanted:
                    seen += letters(ours[position + span])
                    span += 1
                if seen != wanted:
                    print(f"  {surah}:{ayah}: the print's {wanted!r} has no match in the text")
                    return 1
                # Everything after the first token of a printed word is glued to it: no stretch
                # between them, because the print sets them as one unit.
                for offset in range(1, span):
                    glue.append(base + position + offset)
                position += span
                per_line.setdefault(word["line_number"], []).append((surah, ayah, position - 1))
            # The sajdah sign closes fifteen ayahs in Tanzil's text and is not one of the print's
            # words, so it is left over here. It belongs to the word it follows.
            last_line = max(per_line) if per_line else None
            while position < len(ours) and letters(ours[position]) == "":
                glue.append(base + position)
                position += 1
                if last_line is not None:
                    entry = per_line[last_line]
                    entry[-1] = (entry[-1][0], entry[-1][1], position - 1)
            if position != len(ours):
                print(f"  {surah}:{ayah} covers {position} tokens in the print and {len(ours)} in the text")
                return 1
            if ayah == 1:
                opens_at.setdefault(surah, (page, min(w["line_number"] for w in verse["words"])))

        page_ends: list[int] = []
        page_kinds: list[int] = []
        # The cursor runs on from the previous page: a line with no words holds the page's place
        # rather than resetting it, which is what lets an ornament be recognised by a repeat.
        for line in range(1, LINES_PER_PAGE + 1):
            on_line = per_line.get(line)
            if on_line:
                surah, ayah, position = on_line[-1]
                cursor = first_word_of[(surah, ayah)] + position
            page_ends.append(cursor)
            page_kinds.append(AYAH_LINE if on_line else BLANK)
        ends.append(page_ends)
        kinds.append(page_kinds)
        if page % 50 == 0:
            print(f"  {page}/{PAGES}")
        time.sleep(0.05)

    owners = [[0] * LINES_PER_PAGE for _ in range(PAGES)]
    label_ornaments(kinds, owners, opens_at)
    mark_centred_lines(ends, kinds, stream)
    if not check(ends, kinds, total_words):
        return 1

    table = {
        "edition": "KFGQPC Madinah Mushaf, 1405H print (15 lines per page)",
        "pages": PAGES,
        "linesPerPage": LINES_PER_PAGE,
        "tokens": total_words,
        "ends": ends,
        "kinds": kinds,
        "owners": owners,
        "glue": sorted(glue),
    }
    body = json.dumps(table, separators=(",", ":")) + "\n"
    ASSET.write_text(body, encoding="utf-8")
    GOLDEN.parent.mkdir(parents=True, exist_ok=True)
    GOLDEN.write_text(body, encoding="utf-8")
    digest = hashlib.sha256(body.encode("utf-8")).hexdigest()
    print(f"wrote {ASSET.relative_to(ROOT)} ({len(body) // 1024} KB, {len(glue)} joined tokens, sha256 {digest[:16]})")
    print(f"wrote {GOLDEN.relative_to(ROOT)}")
    return 0


def label_ornaments(kinds, owners, opens_at) -> None:
    """
    The blank lines before a surah's first word are its banner and, where it has one, its Basmala.

    They are not always on the same page: Yunus opens at the top of page 208, so its banner sits on
    the last line of 207. Walking backwards from the first word handles both without a special case.
    al-Fatihah is a banner alone, because its Basmala is its first ayah; at-Tawbah is a banner alone
    because it has none.
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
            # Nearest the text is the Basmala; the line above it carries the surah's name.
            first = placed == 0 and needed == 2
            kinds[page_index][line_index] = BASMALA if first else SURAH_HEADER
            owners[page_index][line_index] = surah
            placed += 1
        if placed != needed:
            print(f"  surah {surah}: found {placed} ornament lines, expected {needed}")


def mark_centred_lines(ends, kinds, stream) -> None:
    """A surah's closing line is centred in the print rather than stretched to the margins."""
    last_word_of_surah = {}
    cursor = 0
    for surah, _, count in stream:
        cursor += count
        last_word_of_surah[surah] = cursor - 1
    closing = set(last_word_of_surah.values())
    for page_index, page_ends in enumerate(ends):
        for line_index, end in enumerate(page_ends):
            if kinds[page_index][line_index] != AYAH_LINE:
                continue
            if end in closing:
                kinds[page_index][line_index] = CENTRED_LINE


def check(ends, kinds, total_words: int) -> bool:
    """Everything that must be true before this table is allowed anywhere near the app."""
    ok = True

    def fail(message: str) -> None:
        nonlocal ok
        print(f"  FAIL {message}")
        ok = False

    if len(ends) != PAGES:
        fail(f"{len(ends)} pages, expected {PAGES}")
    if any(len(page) != LINES_PER_PAGE for page in ends):
        fail("a page does not have fifteen lines")
    flat_ends = [e for page in ends for e in page]
    flat_kinds = [k for page in kinds for k in page]

    # The cursor may only stand still or move forward, and must finish on the last word.
    previous = -1
    for index, end in enumerate(flat_ends):
        if end < previous:
            fail(f"line {index} goes backwards: {end} after {previous}")
            break
        previous = end
    if flat_ends[-1] != total_words - 1:
        fail(f"the last line ends at word {flat_ends[-1]}, expected {total_words - 1}")

    banners = flat_kinds.count(SURAH_HEADER)
    basmalas = flat_kinds.count(BASMALA)
    if banners != BANNERS:
        fail(f"{banners} surah banners, expected {BANNERS}")
    if basmalas != BASMALAS:
        fail(f"{basmalas} Basmala lines, expected {BASMALAS}")

    # A blank the script could not account for is only allowed at the foot of a page, where the
    # print simply ran out of text. One in the middle means the labelling missed something.
    for page_index, page_kinds in enumerate(kinds):
        seen_text_after_blank = False
        for line_index in range(LINES_PER_PAGE - 1, -1, -1):
            kind = page_kinds[line_index]
            if kind != BLANK:
                seen_text_after_blank = True
            elif seen_text_after_blank:
                fail(f"page {page_index + 1} line {line_index + 1} is an unexplained blank")
                break

    if ok:
        print(f"checked: {PAGES} pages, {total_words} words, {banners} banners, {basmalas} Basmalas")
    return ok


if __name__ == "__main__":
    sys.exit(main())
