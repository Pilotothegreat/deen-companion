#!/usr/bin/env python3
"""Builds app/src/main/assets/cities.json from GeoNames (CC BY 4.0, https://www.geonames.org).

Run from the repository root:  python3 scripts/cities/build_cities.py
Downloads cities15000.zip and alternateNamesV2.zip (~200 MB) into scripts/cities/.cache.

Each city row is [english, arabic, country, latitude, longitude, timezone, population],
sorted by population. Country names are localized at runtime from the country code.
"""
import io
import json
import os
import re
import urllib.request
import zipfile

BASE_URL = "https://download.geonames.org/export/dump/"
CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".cache")
OUTPUT = os.path.join("app", "src", "main", "assets", "cities.json")

MIN_POPULATION = 50_000
# The Gulf is the app's main audience, so smaller towns there are kept too.
REGIONAL_COUNTRIES = {"OM", "AE", "SA", "KW", "QA", "BH", "YE"}
REGIONAL_MIN_POPULATION = 15_000
# Sections of cities, historical and abandoned places.
SKIPPED_FEATURES = {"PPLX", "PPLH", "PPLQ", "PPLW", "PPLCH"}
ARABIC_SCRIPT = re.compile("[؀-ۿ]")

# Gulf cities whose GeoNames Arabic name is missing or only a Latin transliteration.
ARABIC_OVERRIDES = {
    ("Seeb", "OM"): "السيب", ("Ibri", "OM"): "عبري", ("Saham", "OM"): "صحم", ("Sohar", "OM"): "صحار", ("Sur", "OM"): "صور",
    ("Sufalat Sama'il", "OM"): "سفالة سمائل", ("Ibra'", "OM"): "إبراء", ("Bidbid", "OM"): "بدبد",
    ("Badiyah", "OM"): "بدية",
    ("Mohammed Bin Zayed City", "AE"): "مدينة محمد بن زايد", ("Al Sajaah", "AE"): "السجعة",
    ("Tarif Kalba", "AE"): "طريف كلباء", ("Kalba", "AE"): "كلباء", ("Dibba Al-Hisn", "AE"): "دبا الحصن",
    ("Halwan", "AE"): "حلوان",
    ("Sultanah", "SA"): "السلطانة", ("Unaizah", "SA"): "عنيزة", ("Ad Dawadimi", "SA"): "الدوادمي",
    ("Tarut", "SA"): "تاروت", ("Bariq", "SA"): "بارق", ("Rabigh", "SA"): "رابغ", ("Al Lith", "SA"): "الليث",
    ("Turayf", "SA"): "طريف", ("King Faisal Military City", "SA"): "مدينة الملك فيصل العسكرية",
    ("Al Khafji", "SA"): "الخفجي", ("Afif", "SA"): "عفيف", ("Rahimah", "SA"): "رحيمة", ("Tayma'", "SA"): "تيماء",
    ("Hawtah Bani Tamim", "SA"): "حوطة بني تميم", ("Layla", "SA"): "ليلى", ("Al Khurmah", "SA"): "الخرمة",
    ("Badr Hunayn", "SA"): "بدر حنين", ("Thuwal", "SA"): "ثول", ("Samitah", "SA"): "صامطة", ("al Haql", "SA"): "حقل",
    ("Rumah", "SA"): "رماح", ("As Sulayyil", "SA"): "السليل", ("Turabah", "SA"): "تربة",
    ("Mahd adh Dhahab", "SA"): "مهد الذهب", ("Khulays", "SA"): "خليص", ("Al 'Aqiq", "SA"): "العقيق",
    ("Al Battaliyah", "SA"): "البطالية", ("Al Munayzilah", "SA"): "المنيزلة",
    ("Lusail", "QA"): "لوسيل", ("Mocha", "YE"): "المخا",
}


def fetch(name):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name)
    if not os.path.exists(path):
        print(f"Downloading {name}…")
        urllib.request.urlretrieve(BASE_URL + name, path)
    return path


def read_cities():
    cities = {}
    with zipfile.ZipFile(fetch("cities15000.zip")) as archive, archive.open("cities15000.txt") as raw:
        for line in io.TextIOWrapper(raw, "utf-8"):
            f = line.rstrip("\n").split("\t")
            geoname_id, ascii_name, lat, lon, feature, country, population, timezone = (
                f[0], f[2], f[4], f[5], f[7], f[8], int(f[14] or 0), f[17],
            )
            minimum = REGIONAL_MIN_POPULATION if country in REGIONAL_COUNTRIES else MIN_POPULATION
            if population < minimum or feature in SKIPPED_FEATURES or not timezone:
                continue
            cities[geoname_id] = {
                "ascii": ascii_name,
                "country": country,
                "lat": round(float(lat), 4),
                "lon": round(float(lon), 4),
                "timezone": timezone,
                "population": population,
            }
    return cities


def read_names(city_ids):
    """Best English and Arabic name per city: preferred, not short, not colloquial or historic."""
    best = {"en": {}, "ar": {}}
    with zipfile.ZipFile(fetch("alternateNamesV2.zip")) as archive, archive.open("alternateNamesV2.txt") as raw:
        for line in io.TextIOWrapper(raw, "utf-8"):
            f = line.rstrip("\n").split("\t")
            if len(f) < 8 or f[2] not in best or f[1] not in city_ids:
                continue
            preferred, short, colloquial, historic = (f[4] == "1", f[5] == "1", f[6] == "1", f[7] == "1")
            if colloquial or historic:
                continue
            # Some "ar" entries are Latin transliterations such as "Şūr".
            if f[2] == "ar" and not ARABIC_SCRIPT.search(f[3]):
                continue
            score = (preferred, not short)
            current = best[f[2]].get(f[1])
            if current is None or score > current[0]:
                best[f[2]][f[1]] = (score, f[3].strip())
    return {lang: {k: v[1] for k, v in names.items()} for lang, names in best.items()}


def main():
    cities = read_cities()
    names = read_names(set(cities))
    rows = []
    for geoname_id, city in cities.items():
        english = names["en"].get(geoname_id) or city["ascii"]
        arabic = ARABIC_OVERRIDES.get((english, city["country"])) or names["ar"].get(geoname_id, "")
        rows.append([english, arabic, city["country"], city["lat"], city["lon"], city["timezone"], city["population"]])
    rows.sort(key=lambda row: -row[6])
    document = {
        "source": "GeoNames, https://www.geonames.org (CC BY 4.0)",
        "fields": ["name", "nameAr", "country", "latitude", "longitude", "timezone", "population"],
        "cities": rows,
    }
    with open(OUTPUT, "w", encoding="utf-8") as out:
        json.dump(document, out, ensure_ascii=False, separators=(",", ":"))
    arabic = sum(1 for row in rows if row[1])
    print(f"Wrote {len(rows)} cities ({arabic} with Arabic names) to {OUTPUT}")


if __name__ == "__main__":
    main()
