# Attribution and credits

## Traffic Light

Bilal (formerly Deen Companion) started as a fork of [Traffic Light](https://github.com/leekleak/traffic-light), a privacy-focused network usage tracker by [leekleak](https://github.com/leekleak), licensed under the GNU General Public License v3.0. Thanks to its author for the original Jetpack Compose foundation.

Traffic Light is licensed under the GPLv3, so Bilal is distributed under the same license (see [LICENSE](LICENSE)).

## Content and assets

- **Quran text:** the [Tanzil Project](https://tanzil.net) Uthmani text (version 1.1), copyright © 2007-2026 Tanzil Project, licensed under [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/). It is shipped verbatim: Tanzil permits copying and distribution but not modification, so the app applies no transformation between the asset and the screen, and `QuranTextIntegrityTest` asserts a checksum for every surah.
- **Mushaf divisions:** the page, juz, hizb, rub' al-hizb, manzil, ruku' and sajdah tables come from Tanzil's `quran-data.xml` (CC BY 3.0), generated into `MushafLayout.kt` by `scripts/quran/build_layout.py`.
- **Quran translation:** *The Clear Quran* by Talal Itani, from [clearquran.com](https://clearquran.com), licensed under [CC BY-ND 4.0](https://creativecommons.org/licenses/by-nd/4.0/). It is named wherever it is read, in the reader and in Settings > About.
- **Weather:** [Open-Meteo](https://open-meteo.com), free for non-commercial use and requiring no account or key.
- **Eclipse predictions:** NASA/GSFC eclipse predictions by Fred Espenak, [eclipse.gsfc.nasa.gov](https://eclipse.gsfc.nasa.gov), a US government work in the public domain. Bundled as a table to 2045 by `scripts/eclipses/build_eclipses.py`.
- **Earthquakes:** the [USGS](https://earthquake.usgs.gov) real-time feeds, a US government work in the public domain.
- **Recitation audio:** streamed from [everyayah.com](https://everyayah.com).
- **Hadith collections:** [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api), served by jsDelivr.
- **Fonts:** KFGQPC Uthmanic Hafs (King Fahd Glorious Quran Printing Complex), [Amiri](https://github.com/aliftype/amiri) (OFL) and Google Sans Flex (OFL).
- **Prayer times:** the [Adhan](https://github.com/batoulapps/adhan-kotlin) library by Batoul Apps (MIT). The Oman method is calibrated against the Ministry of Endowments and Religious Affairs timetable published at mara.gov.om. The Ja'fari and Tehran Maghrib angle follows the formulas documented at [praytimes.org](https://praytimes.org).
- **Morning and evening athkar:** [Morning & Evening Adhkar DB](https://github.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB) by Seen Arabic (MIT), including virtues and sources.
- **Other athkar:** *Hisn al-Muslim* by Sa'id ibn Wahf al-Qahtani, as published by [hisnmuslim.com](https://hisnmuslim.com), with the translation and transliteration from that site.
- **Cities:** [GeoNames](https://www.geonames.org) (CC BY 4.0): the cities15000 list with Arabic names from the alternate-names data.
- **Icon:** the Kufic بلال mark was drawn for this app; its sources are in `branding/`.
