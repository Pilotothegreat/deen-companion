#!/usr/bin/env python3
"""Builds app/src/main/assets/athkar.json.

Run from the repository root:  python3 scripts/athkar/build_athkar.py
Sources (cached in scripts/athkar/.cache):
  - Morning & evening: Seen-Arabic Morning-And-Evening-Adhkar-DB (MIT),
    https://github.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB
  - Everything else: Hisn al-Muslim by Sa'id bin Ali bin Wahf al-Qahtani, via the
    hisnmuslim.com JSON API (Arabic text, transliteration, English and repeat counts).

Hisn al-Muslim sometimes joins several athkar in one entry with their counts written inline
("أستغفر الله (ثلاثاً) اللهم أنت السلام…"). The after-prayer and sleep lists are therefore
curated below: each part is copied verbatim from the source and checked against it, so every
entry counts one dhikr. Item IDs come from the sources ("seen-4", "hisn-66a") so saved
progress survives rebuilds.
"""
import json
import os
import re
import urllib.request

CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".cache")
OUTPUT = os.path.join("app", "src", "main", "assets", "athkar.json")
SEEN_URL = "https://raw.githubusercontent.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB/main/result/{}.json"
HISN_URL = "http://www.hisnmuslim.com/api/{lang}/{chapter}.json"

# (group id, English, Arabic, [(id, English, Arabic, Hisn al-Muslim chapters)])
EVERYDAY = [
    ("home", "Home & daily life", "المنزل والحياة اليومية", [
        ("leaving-home", "Leaving home", "الخروج من المنزل", [10]),
        ("entering-home", "Entering home", "دخول المنزل", [11]),
        ("restroom", "Restroom", "دخول الخلاء والخروج منه", [6, 7]),
        ("clothes", "Getting dressed", "اللباس", [2, 3, 5]),
        ("sneezing", "Sneezing", "العطاس", [77]),
    ]),
    ("prayer", "Prayer & mosque", "الصلاة والمسجد", [
        ("wudu", "Ablution", "الوضوء", [8, 9]),
        ("mosque", "Going to the mosque", "المسجد", [12, 13, 14]),
        ("adhan", "Hearing the adhan", "سماع الأذان", [15]),
        ("istikhara", "Istikhara", "الاستخارة", [26]),
    ]),
    ("food", "Food & fasting", "الطعام والصيام", [
        ("eating", "Before and after eating", "الطعام", [69, 70]),
        ("host", "For your host", "الدعاء لصاحب الطعام", [71]),
        ("iftar", "Breaking the fast", "الإفطار", [68]),
    ]),
    ("travel", "Travel", "السفر", [
        ("riding", "Riding", "الركوب", [95]),
        ("travelling", "Setting out", "السفر", [96]),
        ("market", "Entering a market", "دخول السوق", [98]),
        ("returning", "Returning home", "الرجوع من السفر", [105]),
    ]),
    ("hardship", "Hardship & feelings", "الشدائد والمشاعر", [
        ("worry", "Worry and grief", "الهم والحزن", [34]),
        ("distress", "Distress", "الكرب", [35]),
        ("difficulty", "When things are hard", "الأمر الصعب", [43]),
        ("calamity", "When tragedy strikes", "المصيبة", [53]),
        ("anger", "Anger", "الغضب", [82]),
        ("pain", "Pain", "الألم", [124]),
        ("fear", "Fear", "الفزع", [126]),
        ("evil-eye", "Evil eye", "العين", [125]),
    ]),
    ("nature", "Weather & nature", "الطقس والطبيعة", [
        ("wind", "Wind", "الريح", [61]),
        ("thunder", "Thunder", "الرعد", [62]),
        ("rain", "Rain", "المطر", [64, 65]),
        ("new-moon", "New moon", "رؤية الهلال", [67]),
    ]),
    ("social", "People & forgiveness", "الناس والاستغفار", [
        ("gathering", "Leaving a gathering", "كفارة المجلس", [85]),
        ("kindness", "Thanking a kindness", "لمن صنع إليك معروفًا", [87]),
        ("forgiveness", "Seeking forgiveness", "الاستغفار والتوبة", [129]),
    ]),
]

TAHLIL_AR = "لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ"
TAHLIL_EN = ("None has the right to be worshipped except Allah, alone, without partner, to Him belongs all "
             "sovereignty and praise and He is over all things omnipotent.")


# ---------------------------------------------------------------- download & parse

def fetch_text(url, name):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name)
    if not os.path.exists(path):
        print(f"Downloading {url}")
        # hisnmuslim.com rejects urllib's default user agent.
        request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (deen-companion dataset build)"})
        with urllib.request.urlopen(request, timeout=30) as response, open(path, "wb") as out:
            out.write(response.read())
    with open(path, encoding="utf-8-sig") as f:
        return f.read()


def seen_rows(lang):
    return json.loads(fetch_text(SEEN_URL.format(lang), f"seen_{lang}.json"))


def hisn_rows(lang, chapter):
    text = fetch_text(HISN_URL.format(lang=lang, chapter=chapter), f"hisn_{lang}_{chapter}.json")
    # Each file holds one array under a chapter-title key. Some keys are malformed (chapter 126 in
    # English) and some strings contain raw tabs, so only the array is parsed, leniently.
    return json.loads(text[text.index("["): text.rindex("]") + 1], strict=False)


# ---------------------------------------------------------------- cleaning

HARAKAT = re.compile("[ؐ-ًؚ-ٰٟۖ-ۭـ]")
# Short inline repeat notes such as "(ثَلاَثَاً)" or "[ثلاثاً]"; bounded so a whole quoted dhikr that
# happens to mention a number is never mistaken for a note.
_COUNT_WORDS = r"(?:مرة|مرّة|مرات|مرّات|مَرَّاتٍ|مَرَّةً|مرتين|مرّتين|ثلاث|ثَلاَث|أربع|سبع|عشر)"
ARABIC_COUNT_NOTE = re.compile(
    r"\([^()\[\]]{0,20}?" + _COUNT_WORDS + r"[^()\[\]]{0,20}\)|\[[^()\[\]]{0,20}?" + _COUNT_WORDS + r"[^()\[\]]{0,20}\]"
)
ENGLISH_COUNT_NOTE = re.compile(r"\((?:[^()]*?)(?:times|once|twice)[^()]*\)", re.IGNORECASE)
NUMBER_NOTE = re.compile(r"\s*\(\s*\d+\s*\)")
ARABIC_DIGITS = str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩")


def plain(text):
    """Arabic without diacritics or alef variants, for comparing wording."""
    text = HARAKAT.sub("", text)
    for alef in "ٱأإآ":
        text = text.replace(alef, "ا")
    return re.sub(r"\s+", " ", text).strip()


# English parts dropped by first_group(), printed after the build for review.
DROPPED = []
TRAILING_REFERENCE = re.compile(r"\s*\[[^\]]*\]\s*[.،]?\s*$")


GLOSSARY = re.compile(r"^[\w’'\- ]{1,25}:")


def top_level_groups(text):
    """Splits "(a)(b) c" into ["a", "b", "c"], leaving nested parentheses inside a group alone."""
    groups, depth, start = [], 0, 0
    for i, c in enumerate(text):
        if c == "(":
            if depth == 0:
                if text[start:i].strip():
                    groups.append(text[start:i].strip())
                start = i + 1
            depth += 1
        elif c == ")" and depth > 0:
            depth -= 1
            if depth == 0:
                groups.append(text[start:i].strip())
                start = i + 1
    if text[start:].strip():
        groups.append(text[start:].strip())
    return groups


def first_group(text, item_id=None):
    """Hisn al-Muslim wraps translations in parentheses, sometimes followed by more of the translation,
    a glossary entry ("Tawrah: …") or commentary. Keeps the translation, drops the rest."""
    text = text.strip()
    if not text.startswith("("):
        return text
    groups = top_level_groups(text)
    kept = groups[:1]
    for group in groups[1:]:
        if GLOSSARY.match(group) or group.startswith("There are other views"):
            if item_id:
                DROPPED.append((item_id, group))
        else:
            kept.append(group)
    return " ".join(kept)


def tidy_quran(text):
    return text.replace("*", " ۝ ").translate(ARABIC_DIGITS)


def clean_arabic(text):
    """Hisn al-Muslim Arabic: parentheses and brackets only mark quotations and optional words."""
    text = ARABIC_COUNT_NOTE.sub("", text)
    text = NUMBER_NOTE.sub("", text)
    for mark in "()[]":
        text = text.replace(mark, "")
    return re.sub(r"\s+", " ", tidy_quran(text)).strip(" .،")


def clean_seen_arabic(text):
    """Seen-Arabic ends Quran passages with a "[البقرة ٢٨٥ - ٢٨٦]" reference, which the source field already has."""
    return re.sub(r"\s+", " ", tidy_quran(TRAILING_REFERENCE.sub("", text))).strip()


def clean_english(text, item_id=None):
    text = ENGLISH_COUNT_NOTE.sub("", text or "")
    text = NUMBER_NOTE.sub(" ", text)
    return re.sub(r"\s+", " ", first_group(text, item_id)).strip()


def item(item_id, ar, en="", tr="", count=1, virtue=("", ""), source=("", ""), note=("", "")):
    return {
        "id": item_id,
        "ar": ar,
        "en": en,
        "tr": tr,
        "count": count,
        "virtueEn": virtue[0],
        "virtueAr": virtue[1],
        "sourceEn": source[0],
        "sourceAr": source[1],
        "noteEn": note[0],
        "noteAr": note[1],
    }


def hisn_source(chapter):
    return f"Hisn al-Muslim, chapter {chapter}", f"حصن المسلم، الباب {chapter}"


# ---------------------------------------------------------------- sources

class Hisn:
    def __init__(self):
        self.rows = {}
        self.chapter_of = {}

    def chapter(self, chapter):
        english = {row["ID"]: row for row in hisn_rows("en", chapter)}
        ids = []
        for row in hisn_rows("ar", chapter):
            self.rows[row["ID"]] = (row, english.get(row["ID"], {}))
            self.chapter_of[row["ID"]] = chapter
            ids.append(row["ID"])
        return ids

    def item(self, row_id, **overrides):
        ar_row, en_row = self.rows[row_id]
        base = item(
            f"hisn-{row_id}",
            clean_arabic(ar_row["ARABIC_TEXT"]),
            clean_english(en_row.get("TRANSLATED_TEXT"), f"hisn-{row_id}"),
            clean_english(en_row.get("LANGUAGE_ARABIC_TRANSLATED_TEXT")),
            max(1, int(ar_row.get("REPEAT") or 1)),
            source=hisn_source(self.chapter_of[row_id]),
        )
        base.update(overrides)
        return base

    def verify(self, row_id, text):
        """Fails unless [text] appears in the entry; quotation marks and brackets are ignored."""
        source = re.sub(r"[()\[\]]", "", self.rows[row_id][0]["ARABIC_TEXT"])
        assert plain(text) in plain(source), f"hisn-{row_id}: {text[:40]!r} not found in source"

    def part(self, row_id, suffix, ar, en, count, note=("", ""), virtue=("", "")):
        """A dhikr copied verbatim out of an entry that mixes several athkar or instructions. [ar] may be
        a list of verbatim segments, joined with commas."""
        segments = ar if isinstance(ar, list) else [ar]
        for segment in segments:
            self.verify(row_id, segment)
        text = "، ".join(clean_arabic(s) for s in segments)
        return item(f"hisn-{row_id}{suffix}", text, en, "", count, virtue=virtue,
                    source=hisn_source(self.chapter_of[row_id]), note=note)


def seen_items():
    arabic = {row["order"]: row for row in seen_rows("ar")}
    english = {row["order"]: row for row in seen_rows("en")}
    items = []
    for order in sorted(arabic):
        ar, en = arabic[order], english.get(order, {})
        entry = item(
            f"seen-{order}",
            clean_seen_arabic(ar["content"]),
            clean_english(en.get("translation"), f"seen-{order}"),
            clean_english(en.get("transliteration")),
            max(1, int(ar.get("count") or 1)),
            virtue=((en.get("fadl") or "").strip(), (ar.get("fadl") or "").strip()),
            source=((en.get("source") or "").strip(), (ar.get("source") or "").strip()),
        )
        entry["type"] = ar["type"]
        items.append(entry)
    return items


def find_seen(seen, needle):
    hits = [i for i in seen if plain(needle) in plain(i["ar"])]
    assert len(hits) == 1, f"expected one Seen item containing {needle!r}, found {len(hits)}"
    return dict(hits[0])


def without_type(entry):
    return {k: v for k, v in entry.items() if k != "type"}


# ---------------------------------------------------------------- curated lists

def after_prayer(hisn, seen):
    hisn.chapter(25)
    fajr_maghrib_three = ("Three times after Fajr and Maghrib", "ثلاث مرات بعد الفجر والمغرب")
    quls = []
    for needle in ("قل هو الله احد", "قل اعوذ برب الفلق", "قل اعوذ برب الناس"):
        qul = without_type({**find_seen(seen, needle), "count": 1})
        qul["noteEn"], qul["noteAr"] = fajr_maghrib_three
        quls.append(qul)
    kursi = without_type({**find_seen(seen, "تاخذه سنة ولا نوم"), "count": 1})
    return [
        hisn.part(66, "a", "أَسْتَغْفِرُ اللَّهَ", "I ask Allah for forgiveness.", 3),
        hisn.part(66, "b", "اللَّهُمَّ أَنْتَ السَّلاَمُ، وَمِنْكَ السَّلاَمُ، تَبَارَكْتَ يَا ذَا الْجَلاَلِ وَالْإِكْرَامِ",
                  "O Allah, You are As-Salam and from You is all peace, blessed are You, O Possessor of majesty and honour.", 1),
        hisn.part(67, "a", TAHLIL_AR, TAHLIL_EN, 3),
        hisn.part(67, "b", "اللَّهُمَّ لاَ مَانِعَ لِمَا أَعْطَيْتَ، وَلاَ مُعْطِيَ لِمَا مَنَعْتَ، وَلاَ يَنْفَعُ ذَا الْجَدِّ مِنْكَ الجَدُّ",
                  "O Allah, none can prevent what You have willed to bestow and none can bestow what You have willed "
                  "to prevent, and no wealth or majesty can benefit anyone, as from You is all wealth and majesty.", 1),
        hisn.item(68),
        hisn.part(69, "a", "سُبْحَانَ اللَّهِ", "How perfect Allah is.", 33),
        hisn.part(69, "b", "الْحَمْدُ لِلَّهِ", "All praise is for Allah.", 33),
        hisn.part(69, "c", "اللَّهُ أَكْبَرُ", "Allah is the greatest.", 33),
        hisn.part(69, "d", TAHLIL_AR, TAHLIL_EN, 1, note=("Completes the hundred", "تمام المئة")),
        kursi,
        *quls,
        hisn.part(72, "", "لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ يُحْيِي وَيُمِيتُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
                  "None has the right to be worshipped except Allah, alone, without partner, to Him belongs all "
                  "sovereignty and praise, He gives life and causes death and He is over all things omnipotent.", 10,
                  note=("After Fajr and Maghrib", "بعد صلاة الفجر والمغرب")),
        hisn.part(73, "", "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْماً نافِعاً، وَرِزْقاً طَيِّباً، وَعَمَلاً مُتَقَبَّلاً",
                  "O Allah, I ask You for knowledge which is beneficial and sustenance which is good, and deeds which are acceptable.", 1,
                  note=("After Fajr", "بعد صلاة الفجر")),
    ]


def sleep(hisn, seen):
    ids = hisn.chapter(28) + hisn.chapter(29) + hisn.chapter(30)
    palms = ("Cup your hands, blow into them and recite, then wipe over your body. Do this three times.",
             "اجمع كفيك وانفث فيهما واقرأ، ثم امسح بهما ما استطعت من جسدك. ثلاث مرات")
    quls = []
    for index, needle in enumerate(("قل هو الله احد", "قل اعوذ برب الفلق", "قل اعوذ برب الناس")):
        entry = without_type({**find_seen(seen, needle), "count": 3})
        entry["id"] = f"hisn-99-{entry['id']}"
        entry["noteEn"], entry["noteAr"] = palms if index == 0 else ("", "")
        quls.append(entry)
    items = []
    for row_id in ids:
        if row_id == 99:
            items += quls
        elif row_id == 106:
            items += [
                hisn.part(106, "a", "سُبْحَانَ اللَّهِ", "How perfect Allah is.", 33),
                hisn.part(106, "b", "الْحَمْدُ لِلَّهِ", "All praise is for Allah.", 33),
                hisn.part(106, "c", "اللَّهُ أَكْبَرُ", "Allah is the greatest.", 34),
            ]
        else:
            items.append(hisn.item(row_id))
    return items


def curated_everyday(hisn, row_id):
    """Entries whose Arabic mixes the dhikr with narration or instructions, split into what to say plus
    a note. Returns None for entries used as they are and [] for advice with nothing to recite."""
    p = hisn.part
    if row_id == 20:
        return [p(20, "", [
            "أَعُوذُ بِاللَّهِ العَظِيمِ، وَبِوَجْهِهِ الْكَرِيمِ، وَسُلْطَانِهِ الْقَدِيمِ، مِنَ الشَّيْطَانِ الرَّجِيمِ",
            "بِسْمِ اللَّهِ، وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ",
            "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
        ], "I take refuge with Allah, The Supreme and with His Noble Face, and His eternal authority from the accursed "
           "devil. In the name of Allah, and prayers and peace be upon the Messenger of Allah. O Allah, open the gates "
           "of Your mercy for me.", 1, note=("Enter with your right foot", "ادخل برجلك اليمنى"))]
    if row_id == 21:
        return [p(21, "", "بِسْمِ اللَّهِ وَالصّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِك، اللَّهُمَّ اعْصِمْنِي مِنَ الشَّيْطَانِ الرَّجِيمِ",
                  "In the name of Allah, and prayers and peace be upon the Messenger of Allah. O Allah, I ask You from "
                  "Your favour. O Allah, guard me from the accursed devil.", 1,
                  note=("Leave with your left foot", "اخرج برجلك اليسرى"))]
    if row_id == 22:
        return [p(22, "", "لاَ حَوْلَ وَلاَ قُوَّةَ إِلاَّ بِاللَّهِ", "There is no might nor power except with Allah.", 1,
                  note=("Repeat after the mu'adhin, and say this at “Hayya ʿala-s-salah” and “Hayya ʿala-l-falah”",
                        "ردّد مع المؤذن، وقل هذا عند «حيّ على الصلاة» و«حيّ على الفلاح»"))]
    if row_id == 23:
        return [p(23, "", "وَأَنَا أَشْهَدُ أَنْ لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ وَأَنَّ مُحَمَّداً عَبْدُهُ وَرَسُولُهُ، رَضِيتُ بِاللَّهِ رَبَّاً، وَبِمُحَمَّدٍ رَسُولاً، وَبِالْإِسْلاَمِ دِينَاً",
                  "And I too bear witness that none has the right to be worshipped except Allah, alone, without partner, "
                  "and that Muhammad is His slave and Messenger. I am pleased with Allah as a Lord, and Muhammad as a "
                  "Messenger and Islam as a religion.", 1,
                  note=("After the mu'adhin's testimony of faith", "عقب تشهّد المؤذن"))]
    if row_id in (24, 26, 249, 251, 252, 253):
        return []
    if row_id == 25:
        return [p(25, "", "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلاَةِ الْقَائِمَةِ، آتِ مُحَمَّداً الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامَاً مَحمُوداً الَّذِي وَعَدْتَهُ، إِنَّكَ لَا تُخْلِفُ الْمِيعَادَ",
                  "O Allah, Owner of this perfect call and Owner of this prayer to be performed, bestow upon Muhammad "
                  "al-waseelah and al-fadeelah and send him upon a praised platform which You have promised him. "
                  "Verily, You never fail in Your promise.", 1,
                  note=("After the adhan, send blessings on the Prophet ﷺ, then say", "بعد الأذان صلِّ على النبي ﷺ ثم قل"))]
    if row_id == 74:
        return [p(74, "", "اللَّهُمَّ إِنِّي أَسْتَخِيرُكَ بِعِلْمِكَ، وَأَسْتَقْدِرُكَ بِقُدْرَتِكَ، وَأَسْأَلُكَ مِنْ فَضْلِكَ العَظِيمِ؛ فَإِنَّكَ تَقْدِرُ وَلاَ أَقْدِرُ، وَتَعْلَمُ وَلاَ أَعْلَمُ، وَأَنْتَ عَلاَّمُ الغُيُوبِ، اللَّهُمَّ إِنْ كُنْتَ تَعْلَمُ أَنَّ هَذَا الأمْرَ - وَيُسَمِّي حَاجَتَهُ - خَيْرٌ لِي فِي دِينِي وَمَعَاشِي وَعَاقِبَةِ أَمْرِي – أَوْ قَالَ: عَاجِلِهِ وَآجِلِهِ - فَاقْدُرْهُ لِي وَيَسِّرْهُ لِي ثمَّ بَارِكْ لِي فِيهِ، وَإِنْ كُنْتَ تَعْلَمُ أَنَّ هَذَا الْأَمْرَ شَرٌّ لِي فِي دِينِي وَمَعَاشِي وَعَاقِبَةِ أَمْرِي – أَوْ قَالَ: عَاجِلِهِ وَآجِلِهِ – فَاصْرِفْهُ عَنِّي وَاصْرِفْنِي عَنْهُ وَاقْدُرْ لِيَ الْخَيْرَ حَيْثُ كَانَ، ثُمَّ أَرْضِنِي بِهِ",
                  "O Allah, I seek Your counsel by Your knowledge and by Your power I seek strength and I ask You from "
                  "Your immense favour, for verily You are able while I am not and verily You know while I do not and You "
                  "are the Knower of the unseen. O Allah, if You know this affair (name it) to be good for me in relation "
                  "to my religion, my life, and end, then decree and facilitate it for me, and bless me with it, and if "
                  "You know this affair to be ill for me towards my religion, my life, and end, then remove it from me "
                  "and remove me from it, and decree for me what is good wherever it be and make me satisfied with such.", 1,
                  note=("Pray two voluntary rak'ahs, then say this, naming your matter where marked",
                        "صلِّ ركعتين من غير الفريضة، ثم قل وسمِّ حاجتك في موضعها"))]
    if row_id == 10:
        return [p(10, "", ["بِسْمِ اللَّهِ", "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبْثِ وَالْخَبائِث"],
                  "In the name of Allah. O Allah, I take refuge with You from all evil and evil-doers.", 1,
                  note=("Before entering the restroom", "قبل دخول الخلاء"))]
    if row_id == 178:
        return [
            p(178, "a", "بِسْمِ اللَّهِ", "In the name of Allah.", 1, note=("Before eating", "قبل الأكل")),
            p(178, "b", "بسمِ اللَّهِ فِي أَوَّلِهِ وَآخِرِهِ", "In the name of Allah, at its beginning and its end.", 1,
              note=("If you forgot at the start, say this when you remember", "إن نسيت في أوله فقل حين تذكر")),
        ]
    if row_id == 179:
        return [
            p(179, "a", "اللَّهُمَّ بَارِكْ لَنَا فِيهِ وَأَطْعِمْنَا خَيْراً مِنْهُ", "O Allah, bless it for us and feed us better than it.", 1,
              note=("When Allah gives you food", "إذا أطعمك الله طعامًا")),
            p(179, "b", "اللَّهُمَّ بَارِكْ لَنَا فِيهِ وَزِدْنَا مِنْهُ", "O Allah, bless it for us and give us more of it.", 1,
              note=("When Allah gives you milk to drink", "إذا سقاك الله لبنًا")),
        ]
    if row_id == 188:
        return [
            p(188, "a", "الْحَمْدُ لِلَّهِ", "All praise is for Allah.", 1, note=("When you sneeze", "إذا عطست")),
            p(188, "b", "يَرْحَمُكَ اللَّهُ", "May Allah have mercy upon you.", 1,
              note=("To someone who sneezes and praises Allah", "لمن عطس فحمد الله")),
            p(188, "c", "يَهْدِيكُمُ اللَّهُ وَيُصْلِحُ بَالَكُمْ", "May Allah guide you and rectify your condition.", 1,
              note=("The reply of the one who sneezed", "ردّ العاطس")),
        ]
    if row_id == 207:
        return [p(207, "", [
            "اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، ﴿سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ * وَإِنَّا إِلَى رَبِّنَا لَمُنقَلِبُونَ﴾",
            "اللَّهُمَّ إِنّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا البِرَّ وَالتَّقْوَى، وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَذَا وَاطْوِ عَنَّا بُعْدَهُ، اللَّهُمَّ أَنْتَ الصَّاحِبُ فِي السَّفَرِ، وَالْخَليفَةُ فِي الْأَهْلِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ وَعْثَاءِ السَّفَرِ، وَكَآبَةِ الْمَنْظَرِ، وَسُوءِ الْمُنْقَلَبِ فِي الْمَالِ وَالْأَهْلِ",
        ], "Allah is the greatest, Allah is the greatest, Allah is the greatest. How perfect He is, The One Who has "
           "placed this (transport) at our service, and we ourselves would not have been capable of that, and to our "
           "Lord is our final destiny. O Allah, we ask You for birr and taqwa in this journey of ours, and we ask You "
           "for deeds which please You. O Allah, facilitate our journey and let us cover its distance quickly. O Allah, "
           "You are The Companion on the journey and The Successor over the family. O Allah, I take refuge with You "
           "from the difficulties of travel, from having a change of hearts and being in a bad predicament, and I take "
           "refuge in You from an ill fated outcome with wealth and family.", 1)]
    if row_id == 217:
        hisn.verify(207, "اللَّهُ أَكْبَرُ")
        return [
            item("hisn-217a", "اللَّهُ أَكْبَرُ", "Allah is the greatest.", "", 3, source=hisn_source(105),
                 note=("On the way back, at every high point", "على كل مرتفع في طريق العودة")),
            p(217, "b", "لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ، وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، آيِبُونَ، تَائِبُونَ، عَابِدُونَ، لِرَبِّنا حَامِدُونَ، صَدَقَ اللَّهُ وَعْدَهُ، وَنَصَرَ عَبْدَهُ، وَهَزَمَ الْأَحْزابَ وَحْدَهُ",
              "None has the right to be worshipped except Allah, alone, without partner. To Him belongs all sovereignty "
              "and praise, and He is over all things omnipotent. We return, repent, worship and praise our Lord. Allah "
              "fulfilled His promise, aided His Servant, and single-handedly defeated the allies.", 1,
              note=("Then say", "ثم قل")),
        ]
    if row_id == 243:
        return [
            p(243, "a", "بِسْمِ اللَّهِ", "In the name of Allah.", 3,
              note=("Place your hand where it hurts", "ضع يدك على موضع الألم")),
            p(243, "b", "أَعُوذُ بِاللَّهِ وَقُدْرَتِهِ مِنْ شَرِّ مَا أَجِدُ وَأُحَاذِرُ",
              "I take refuge in Allah and within His omnipotence from the evil that I feel and am wary of.", 7),
        ]
    if row_id == 248:
        return [p(248, "", "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", "I seek Allah's forgiveness and repent to Him.", 100,
                  virtue=("The Prophet ﷺ said: “By Allah, I seek forgiveness and repent to Allah more than seventy times a day,” "
                          "and he told people to repent a hundred times a day.",
                          clean_arabic(hisn.rows[248][0]["ARABIC_TEXT"])))]
    if row_id == 250:
        return [p(250, "", "أَسْتَغْفِرُ اللَّهَ الْعَظيمَ الَّذِي لاَ إِلَهَ إِلاَّ هُوَ الْحَيُّ القَيّوُمُ وَأَتُوبُ إِلَيهِ",
                  "I seek Allah's forgiveness, besides whom, none has the right to be worshipped except He, The Ever "
                  "Living, The Self-Subsisting and Supporter of all, and I turn to Him in repentance.", 1,
                  virtue=("Allah forgives whoever says it, even one who fled from the battlefield.",
                          "غَفَرَ اللَّهُ لَهُ وَإِنْ كَانَ فَرَّ مِنَ الزَّحْفِ"))]
    return None


def category(cid, title_en, title_ar, items):
    return {"id": cid, "titleEn": title_en, "titleAr": title_ar, "items": items}


def main():
    seen = seen_items()
    hisn = Hisn()
    core = [
        category("morning", "Morning", "أذكار الصباح", [without_type(i) for i in seen if i["type"] in (0, 1)]),
        category("evening", "Evening", "أذكار المساء", [without_type(i) for i in seen if i["type"] in (0, 2)]),
        category("after-prayer", "After prayer", "أذكار بعد الصلاة", after_prayer(hisn, seen)),
        category("waking", "Waking up", "أذكار الاستيقاظ", [hisn.item(i) for i in hisn.chapter(1)]),
        category("sleep", "Before sleep", "أذكار النوم", sleep(hisn, seen)),
    ]
    groups = []
    for gid, group_en, group_ar, entries in EVERYDAY:
        categories = []
        for cid, en, ar, chapters in entries:
            items = []
            for row_id in [row_id for chapter in chapters for row_id in hisn.chapter(chapter)]:
                special = curated_everyday(hisn, row_id)
                items += [hisn.item(row_id)] if special is None else special
            categories.append(category(cid, en, ar, items))
        groups.append({"id": gid, "titleEn": group_en, "titleAr": group_ar, "categories": categories})

    for cat in core + [c for g in groups for c in g["categories"]]:
        ids = [i["id"] for i in cat["items"]]
        assert len(ids) == len(set(ids)), f"duplicate item ids in {cat['id']}"
        assert all(i["ar"] and i["count"] >= 1 for i in cat["items"]), f"empty item in {cat['id']}"

    document = {
        "sources": [
            "Seen-Arabic Morning and Evening Adhkar DB (MIT)",
            "Hisn al-Muslim by Sa'id bin Ali bin Wahf al-Qahtani, via hisnmuslim.com",
        ],
        "core": core,
        "groups": groups,
    }
    with open(OUTPUT, "w", encoding="utf-8") as out:
        json.dump(document, out, ensure_ascii=False, indent=1)

    total = sum(len(c["items"]) for c in core) + sum(len(c["items"]) for g in groups for c in g["categories"])
    print(f"Wrote {len(core)} core categories, {sum(len(g['categories']) for g in groups)} everyday ones, "
          f"{total} items to {OUTPUT}")
    if DROPPED:
        print("English notes dropped after the translation (review these):")
        for item_id, rest in DROPPED:
            print(f"  {item_id}: {rest[:160]}")


if __name__ == "__main__":
    main()
