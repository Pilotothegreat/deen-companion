# Attribution and credits

## Traffic Light

Bilal (formerly Deen Companion) started as a fork of [Traffic Light](https://github.com/leekleak/traffic-light), a privacy-focused network usage tracker by [leekleak](https://github.com/leekleak), licensed under the GNU General Public License v3.0. Thanks to its author for the original Jetpack Compose foundation.

Traffic Light is licensed under the GPLv3, so Bilal is distributed under the same license (see [LICENSE](LICENSE)).

## Content and assets

- **Quran text:** the King Fahd Glorious Quran Printing Complex's Uthmanic Hafs text (QPC Hafs), read through the [Quran.com API](https://api.quran.com/api/v4/quran/verses/qpc_hafs) by `scripts/quran/build_quran.py`. The Complex distributes it free of cost on condition that it is not changed, so it is shipped verbatim: search, copy and share use it exactly as published, and `QuranTextIntegrityTest` asserts a checksum for every surah. On the page, a line is justified the way the printed mushaf justifies it, by lengthening the joins between letters with tatweel (ـ); no letter or mark is added, removed or moved.
- **Mushaf divisions:** the page, juz, hizb, rub' al-hizb, manzil, ruku' and sajdah tables come from Tanzil's `quran-data.xml` (CC BY 3.0), generated into `MushafLayout.kt` by `scripts/quran/build_layout.py`.
- **Mushaf line breaks:** which line each word sits on comes from the King Fahd Glorious Quran Printing Complex's 1405H Madinah edition, read through the [Quran.com API](https://api.quran.com) and generated into `assets/mushaf-lines.json` by `scripts/quran/build_lines.py`, which matches the edition's words to the text word for word and refuses to write a table that does not match. Which few lines the print centres rather than stretches is read from its printed pages, as served by Quran for Android, by `scripts/quran/find_centred_lines.py`.
- **Quran translation:** *The Clear Quran* by Talal Itani, from [clearquran.com](https://clearquran.com), licensed under [CC BY-ND 4.0](https://creativecommons.org/licenses/by-nd/4.0/). It is named wherever it is read, in the reader and in Settings > About.
- **Weather:** [Open-Meteo](https://open-meteo.com), free for non-commercial use and requiring no account or key.
- **Eclipse predictions:** NASA/GSFC eclipse predictions by Fred Espenak, [eclipse.gsfc.nasa.gov](https://eclipse.gsfc.nasa.gov), a US government work in the public domain. Bundled as a table to 2045 by `scripts/eclipses/build_eclipses.py`.
- **Earthquakes:** the [USGS](https://earthquake.usgs.gov) real-time feeds, a US government work in the public domain.
- **Recitation audio:** streamed from [everyayah.com](https://everyayah.com).
- **Hadith collections:** [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api), served by jsDelivr.
- **Fonts:** KFGQPC Uthmanic Hafs, version 18 (King Fahd Glorious Quran Printing Complex; free to use and distribute, not to modify), the font the Complex builds for its QPC Hafs text; [Amiri](https://github.com/aliftype/amiri) (OFL) and Google Sans Flex (OFL).
- **Prayer times:** the [Adhan](https://github.com/batoulapps/adhan-kotlin) library by Batoul Apps (MIT). The Oman method is calibrated against the Ministry of Endowments and Religious Affairs timetable published at mara.gov.om. The Ja'fari and Tehran Maghrib angle follows the formulas documented at [praytimes.org](https://praytimes.org).
- **Morning and evening athkar:** [Morning & Evening Adhkar DB](https://github.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB) by Seen Arabic (MIT), including virtues and sources.
- **Other athkar:** *Hisn al-Muslim* by Sa'id ibn Wahf al-Qahtani, as published by [hisnmuslim.com](https://hisnmuslim.com), with the translation and transliteration from that site.
- **Cities:** [GeoNames](https://www.geonames.org) (CC BY 4.0): the cities15000 list with Arabic names from the alternate-names data.
- **Icon:** the Kufic بلال mark was drawn for this app; its sources are in `branding/`.
