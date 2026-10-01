package com.pilotothegreat.deencompanion.core.text

import java.time.LocalDate
import java.util.Locale

data class Inspiration(
    val arabic: String,
    val english: String,
    val sourceArabic: String,
    val sourceEnglish: String,
) {
    fun text(locale: Locale): String = if (locale.language == "ar") arabic else english
    fun source(locale: Locale): String = if (locale.language == "ar") sourceArabic else sourceEnglish
}

private enum class Source(val arabic: String, val english: String) {
    AGREED("متفق عليه", "Bukhari & Muslim"),
    BUKHARI("صحيح البخاري", "Sahih al-Bukhari"),
    MUSLIM("صحيح مسلم", "Sahih Muslim"),
    TIRMIDHI("سنن الترمذي", "Jami' at-Tirmidhi"),
    ABU_DAWUD("سنن أبي داود", "Sunan Abu Dawud"),
    IBN_MAJAH("سنن ابن ماجه", "Sunan Ibn Majah"),
    ABU_DAWUD_TIRMIDHI("أبو داود والترمذي", "Abu Dawud & Tirmidhi"),
    ABU_DAWUD_NASAI("أبو داود والنسائي", "Abu Dawud & an-Nasa'i"),
    ABU_DAWUD_IBN_MAJAH("أبو داود وابن ماجه", "Abu Dawud & Ibn Majah"),
    TIRMIDHI_NASAI("الترمذي والنسائي", "Tirmidhi & an-Nasa'i"),
    TIRMIDHI_IBN_MAJAH("الترمذي وابن ماجه", "Tirmidhi & Ibn Majah"),
    ADAB("الأدب المفرد", "Al-Adab al-Mufrad"),
    IBN_HIBBAN("صحيح ابن حبان", "Sahih Ibn Hibban"),
}

private fun said(arabic: String, english: String, source: Source) = Inspiration(arabic, english, source.arabic, source.english)

/**
 * One hadith or prophetic dua per day, shared by the Today card and the widget. Quranic ayahs
 * have their own daily card ([DailyVerse]).
 */
object Inspirations {
    val all = listOf(
        said("إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ", "Actions are but by intentions.", Source.AGREED),
        said("خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ", "The best of you are those who learn the Quran and teach it.", Source.BUKHARI),
        said("الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ", "A good word is charity.", Source.AGREED),
        said(
            "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
            "None of you truly believes until he loves for his brother what he loves for himself.",
            Source.AGREED,
        ),
        said("مِنْ حُسْنِ إِسْلَامِ الْمَرْءِ تَرْكُهُ مَا لَا يَعْنِيهِ", "Part of a person's good Islam is leaving what does not concern him.", Source.TIRMIDHI),
        said(
            "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الْآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
            "Whoever believes in Allah and the Last Day, let him speak good or stay silent.",
            Source.AGREED,
        ),
        said("الدِّينُ النَّصِيحَةُ", "The religion is sincere advice.", Source.MUSLIM),
        said("لَا تَغْضَبْ", "Do not become angry.", Source.BUKHARI),
        said(
            "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ",
            "Be mindful of Allah wherever you are, follow a bad deed with a good one to erase it, and treat people with good character.",
            Source.TIRMIDHI,
        ),
        said("تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ", "Your smile to your brother is charity.", Source.TIRMIDHI),
        said("الطُّهُورُ شَطْرُ الْإِيمَانِ", "Purity is half of faith.", Source.MUSLIM),
        said("أَحَبُّ الْأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ", "The deeds most beloved to Allah are the most constant, even if small.", Source.AGREED),
        said("الْمُسْلِمُ مَنْ سَلِمَ الْمُسْلِمُونَ مِنْ لِسَانِهِ وَيَدِهِ", "A Muslim is one from whose tongue and hand the Muslims are safe.", Source.AGREED),
        said(
            "لَيْسَ الشَّدِيدُ بِالصُّرَعَةِ، إِنَّمَا الشَّدِيدُ الَّذِي يَمْلِكُ نَفْسَهُ عِنْدَ الْغَضَبِ",
            "The strong one is not the wrestler; the strong one controls himself when angry.",
            Source.AGREED,
        ),
        said("إِنَّ اللَّهَ رَفِيقٌ يُحِبُّ الرِّفْقَ فِي الْأَمْرِ كُلِّهِ", "Allah is gentle and loves gentleness in every matter.", Source.BUKHARI),
        said("مَنْ لَا يَرْحَمُ لَا يُرْحَمُ", "Whoever does not show mercy will not be shown mercy.", Source.AGREED),
        said("الرَّاحِمُونَ يَرْحَمُهُمُ الرَّحْمَنُ", "The merciful are shown mercy by the Most Merciful.", Source.ABU_DAWUD_TIRMIDHI),
        said(
            "يَسِّرُوا وَلَا تُعَسِّرُوا، وَبَشِّرُوا وَلَا تُنَفِّرُوا",
            "Make things easy, not hard; bring good news and do not drive people away.",
            Source.AGREED,
        ),
        said(
            "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            "Two phrases are light on the tongue, heavy on the scale and beloved to the Most Merciful: glory be to Allah and praise Him; glory be to Allah the Magnificent.",
            Source.AGREED,
        ),
        said(
            "أَقْرَبُ مَا يَكُونُ الْعَبْدُ مِنْ رَبِّهِ وَهُوَ سَاجِدٌ، فَأَكْثِرُوا الدُّعَاءَ",
            "A servant is closest to his Lord while prostrating, so supplicate much.",
            Source.MUSLIM,
        ),
        said("الدُّعَاءُ هُوَ الْعِبَادَةُ", "Supplication is worship.", Source.ABU_DAWUD_TIRMIDHI),
        said(
            "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ",
            "Whoever takes a path seeking knowledge, Allah makes easy for him a path to Paradise.",
            Source.MUSLIM,
        ),
        said(
            "لَيْسَ الْغِنَى عَنْ كَثْرَةِ الْعَرَضِ، وَلَكِنَّ الْغِنَى غِنَى النَّفْسِ",
            "Richness is not having many belongings; true richness is contentment of the soul.",
            Source.AGREED,
        ),
        said("مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ", "Charity never decreases wealth.", Source.MUSLIM),
        said("اتَّقُوا النَّارَ وَلَوْ بِشِقِّ تَمْرَةٍ", "Shield yourselves from the Fire, even with half a date given in charity.", Source.AGREED),
        said(
            "الْمُؤْمِنُ الْقَوِيُّ خَيْرٌ وَأَحَبُّ إِلَى اللَّهِ مِنَ الْمُؤْمِنِ الضَّعِيفِ، وَفِي كُلٍّ خَيْرٌ",
            "The strong believer is better and more beloved to Allah than the weak believer, and in both there is good.",
            Source.MUSLIM,
        ),
        said("احْرِصْ عَلَى مَا يَنْفَعُكَ، وَاسْتَعِنْ بِاللَّهِ وَلَا تَعْجَزْ", "Strive for what benefits you, seek Allah's help, and do not give up.", Source.MUSLIM),
        said("عَجَبًا لِأَمْرِ الْمُؤْمِنِ، إِنَّ أَمْرَهُ كُلَّهُ خَيْرٌ", "How wonderful is the affair of the believer: all of it is good for him.", Source.MUSLIM),
        said("احْفَظِ اللَّهَ يَحْفَظْكَ", "Be mindful of Allah and He will protect you.", Source.TIRMIDHI),
        said(
            "إِذَا سَأَلْتَ فَاسْأَلِ اللَّهَ، وَإِذَا اسْتَعَنْتَ فَاسْتَعِنْ بِاللَّهِ",
            "When you ask, ask Allah; when you seek help, seek help from Allah.",
            Source.TIRMIDHI,
        ),
        said("كُنْ فِي الدُّنْيَا كَأَنَّكَ غَرِيبٌ أَوْ عَابِرُ سَبِيلٍ", "Be in this world as if you were a stranger or a traveller.", Source.BUKHARI),
        said(
            "إِنَّ اللَّهَ لَا يَنْظُرُ إِلَى صُوَرِكُمْ وَأَمْوَالِكُمْ، وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ وَأَعْمَالِكُمْ",
            "Allah does not look at your appearance or your wealth, but at your hearts and your deeds.",
            Source.MUSLIM,
        ),
        said("أَكْمَلُ الْمُؤْمِنِينَ إِيمَانًا أَحْسَنُهُمْ خُلُقًا", "The believers most complete in faith are those with the best character.", Source.ABU_DAWUD_TIRMIDHI),
        said(
            "خَيْرُكُمْ خَيْرُكُمْ لِأَهْلِهِ، وَأَنَا خَيْرُكُمْ لِأَهْلِي",
            "The best of you are the best to their families, and I am the best of you to my family.",
            Source.TIRMIDHI,
        ),
        said("لَا يَشْكُرُ اللَّهَ مَنْ لَا يَشْكُرُ النَّاسَ", "Whoever does not thank people does not thank Allah.", Source.ABU_DAWUD),
        said("تَهَادَوْا تَحَابُّوا", "Give each other gifts and you will love one another.", Source.ADAB),
        said("أَفْشُوا السَّلَامَ بَيْنَكُمْ", "Spread the greeting of peace among yourselves.", Source.MUSLIM),
        said(
            "الْمُؤْمِنُ لِلْمُؤْمِنِ كَالْبُنْيَانِ يَشُدُّ بَعْضُهُ بَعْضًا",
            "Believers are to one another like a building, each part strengthening the other.",
            Source.AGREED,
        ),
        said(
            "مَنْ نَفَّسَ عَنْ مُؤْمِنٍ كُرْبَةً مِنْ كُرَبِ الدُّنْيَا، نَفَّسَ اللَّهُ عَنْهُ كُرْبَةً مِنْ كُرَبِ يَوْمِ الْقِيَامَةِ",
            "Whoever relieves a believer of a hardship of this world, Allah will relieve him of a hardship of the Day of Resurrection.",
            Source.MUSLIM,
        ),
        said("وَاللَّهُ فِي عَوْنِ الْعَبْدِ مَا كَانَ الْعَبْدُ فِي عَوْنِ أَخِيهِ", "Allah helps His servant as long as the servant helps his brother.", Source.MUSLIM),
        said("مَنْ دَلَّ عَلَى خَيْرٍ فَلَهُ مِثْلُ أَجْرِ فَاعِلِهِ", "Whoever guides someone to good has a reward like the one who does it.", Source.MUSLIM),
        said(
            "لَا تَحْقِرَنَّ مِنَ الْمَعْرُوفِ شَيْئًا، وَلَوْ أَنْ تَلْقَى أَخَاكَ بِوَجْهٍ طَلْقٍ",
            "Do not belittle any good deed, even meeting your brother with a cheerful face.",
            Source.MUSLIM,
        ),
        said("دَعْ مَا يَرِيبُكَ إِلَى مَا لَا يَرِيبُكَ", "Leave what gives you doubt for what does not.", Source.TIRMIDHI_NASAI),
        said("الْبِرُّ حُسْنُ الْخُلُقِ", "Righteousness is good character.", Source.MUSLIM),
        said("إِنَّ اللَّهَ طَيِّبٌ لَا يَقْبَلُ إِلَّا طَيِّبًا", "Allah is Good and accepts only what is good.", Source.MUSLIM),
        said("إِنَّ اللَّهَ كَتَبَ الْإِحْسَانَ عَلَى كُلِّ شَيْءٍ", "Allah has prescribed excellence in everything.", Source.MUSLIM),
        said("قُلْ: آمَنْتُ بِاللَّهِ، ثُمَّ اسْتَقِمْ", "Say, \"I believe in Allah,\" then stay steadfast.", Source.MUSLIM),
        said(
            "نِعْمَتَانِ مَغْبُونٌ فِيهِمَا كَثِيرٌ مِنَ النَّاسِ: الصِّحَّةُ وَالْفَرَاغُ",
            "Two blessings many people waste: health and free time.",
            Source.BUKHARI,
        ),
        said(
            "مَنْ صَامَ رَمَضَانَ إِيمَانًا وَاحْتِسَابًا غُفِرَ لَهُ مَا تَقَدَّمَ مِنْ ذَنْبِهِ",
            "Whoever fasts Ramadan out of faith and hope of reward has his past sins forgiven.",
            Source.AGREED,
        ),
        said("أَحَبُّ الْبِلَادِ إِلَى اللَّهِ مَسَاجِدُهَا", "The places most beloved to Allah are the mosques.", Source.MUSLIM),
        said(
            "صَلَاةُ الْجَمَاعَةِ تَفْضُلُ صَلَاةَ الْفَذِّ بِسَبْعٍ وَعِشْرِينَ دَرَجَةً",
            "Prayer in congregation is twenty-seven degrees better than praying alone.",
            Source.AGREED,
        ),
        said(
            "اقْرَءُوا الْقُرْآنَ فَإِنَّهُ يَأْتِي يَوْمَ الْقِيَامَةِ شَفِيعًا لِأَصْحَابِهِ",
            "Read the Quran, for it will come on the Day of Resurrection as an intercessor for its companions.",
            Source.MUSLIM,
        ),
        said(
            "مَثَلُ الَّذِي يَذْكُرُ رَبَّهُ وَالَّذِي لَا يَذْكُرُ رَبَّهُ مَثَلُ الْحَيِّ وَالْمَيِّتِ",
            "One who remembers his Lord and one who does not are like the living and the dead.",
            Source.BUKHARI,
        ),
        said("لَا يَزَالُ لِسَانُكَ رَطْبًا مِنْ ذِكْرِ اللَّهِ", "Keep your tongue moist with the remembrance of Allah.", Source.TIRMIDHI),
        said("كُلُّ مَعْرُوفٍ صَدَقَةٌ", "Every good deed is charity.", Source.AGREED),
        said("إِنَّ اللَّهَ جَمِيلٌ يُحِبُّ الْجَمَالَ", "Allah is Beautiful and loves beauty.", Source.MUSLIM),
        said("الْحَيَاءُ لَا يَأْتِي إِلَّا بِخَيْرٍ", "Modesty brings nothing but good.", Source.AGREED),
        said(
            "إِنَّ الصِّدْقَ يَهْدِي إِلَى الْبِرِّ، وَإِنَّ الْبِرَّ يَهْدِي إِلَى الْجَنَّةِ",
            "Truthfulness leads to righteousness, and righteousness leads to Paradise.",
            Source.AGREED,
        ),
        said("الْيَدُ الْعُلْيَا خَيْرٌ مِنَ الْيَدِ السُّفْلَى", "The upper (giving) hand is better than the lower (receiving) hand.", Source.AGREED),
        said(
            "مَنْ يُرِدِ اللَّهُ بِهِ خَيْرًا يُفَقِّهْهُ فِي الدِّينِ",
            "When Allah wills good for someone, He gives them understanding of the religion.",
            Source.AGREED,
        ),
        said("بَلِّغُوا عَنِّي وَلَوْ آيَةً", "Convey from me, even a single verse.", Source.BUKHARI),
        said(
            "انْظُرُوا إِلَى مَنْ أَسْفَلَ مِنْكُمْ، وَلَا تَنْظُرُوا إِلَى مَنْ هُوَ فَوْقَكُمْ، فَهُوَ أَجْدَرُ أَنْ لَا تَزْدَرُوا نِعْمَةَ اللَّهِ عَلَيْكُمْ",
            "Look at those below you, not at those above you; that is more likely to keep you from belittling Allah's blessings upon you.",
            Source.MUSLIM,
        ),
        said("مَا مَلَأَ آدَمِيٌّ وِعَاءً شَرًّا مِنْ بَطْنٍ", "No human fills a vessel worse than his stomach.", Source.TIRMIDHI),

        // Prophetic duas
        said(
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى وَالتُّقَى وَالْعَفَافَ وَالْغِنَى",
            "O Allah, I ask You for guidance, piety, chastity and self-sufficiency.",
            Source.MUSLIM,
        ),
        said("يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ", "O Turner of hearts, keep my heart firm upon Your religion.", Source.TIRMIDHI),
        said(
            "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
            "O Allah, help me to remember You, to thank You and to worship You well.",
            Source.ABU_DAWUD_NASAI,
        ),
        said("اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي", "O Allah, You are Pardoning and love to pardon, so pardon me.", Source.TIRMIDHI_IBN_MAJAH),
        said(
            "اللَّهُمَّ رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً، وَفِي الْآخِرَةِ حَسَنَةً، وَقِنَا عَذَابَ النَّارِ",
            "O Allah, our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.",
            Source.AGREED,
        ),
        said(
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا",
            "O Allah, I ask You for beneficial knowledge, good provision and accepted deeds.",
            Source.IBN_MAJAH,
        ),
        said(
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ، وَغَلَبَةِ الرِّجَالِ",
            "O Allah, I seek refuge in You from worry and grief, helplessness and laziness, miserliness and cowardice, the burden of debt and being overpowered by others.",
            Source.BUKHARI,
        ),
        said("اللَّهُمَّ اهْدِنِي وَسَدِّدْنِي", "O Allah, guide me and keep me on the right course.", Source.MUSLIM),
        said(
            "رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ، إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ",
            "My Lord, forgive me and accept my repentance; You are the Accepter of repentance, the Merciful.",
            Source.ABU_DAWUD,
        ),
        said(
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ زَوَالِ نِعْمَتِكَ، وَتَحَوُّلِ عَافِيَتِكَ، وَفُجَاءَةِ نِقْمَتِكَ، وَجَمِيعِ سَخَطِكَ",
            "O Allah, I seek refuge in You from the loss of Your blessings, the change of Your protection, Your sudden punishment and all Your displeasure.",
            Source.MUSLIM,
        ),
        said(
            "اللَّهُمَّ آتِ نَفْسِي تَقْوَاهَا، وَزَكِّهَا أَنْتَ خَيْرُ مَنْ زَكَّاهَا، أَنْتَ وَلِيُّهَا وَمَوْلَاهَا",
            "O Allah, grant my soul its piety and purify it, for You are the best to purify it; You are its Guardian and Protector.",
            Source.MUSLIM,
        ),
        said(
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ",
            "O Allah, I ask You for well-being in this world and the Hereafter.",
            Source.ABU_DAWUD_IBN_MAJAH,
        ),
        said(
            "اللَّهُمَّ لَا سَهْلَ إِلَّا مَا جَعَلْتَهُ سَهْلًا، وَأَنْتَ تَجْعَلُ الْحَزْنَ إِذَا شِئْتَ سَهْلًا",
            "O Allah, nothing is easy except what You make easy, and You make the difficult easy if You will.",
            Source.IBN_HIBBAN,
        ),
        said("يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ", "O Ever-Living, O Sustainer of all, by Your mercy I seek relief.", Source.TIRMIDHI),
        said(
            "اللَّهُمَّ رَحْمَتَكَ أَرْجُو، فَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ، وَأَصْلِحْ لِي شَأْنِي كُلَّهُ، لَا إِلَهَ إِلَّا أَنْتَ",
            "O Allah, it is Your mercy I hope for, so do not leave me to myself for the blink of an eye, and set right all my affairs; there is no god but You.",
            Source.ABU_DAWUD,
        ),
        said(
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ",
            "Glory be to Allah and praise Him, as many times as His creation, as much as pleases Him, as heavy as His Throne and as vast as His words.",
            Source.MUSLIM,
        ),
    )

    /** Coprime with the list size, so consecutive days jump around the list and still visit every entry. */
    internal const val STRIDE = 29L

    fun forDate(date: LocalDate): Inspiration = all[Math.floorMod(date.toEpochDay() * STRIDE, all.size.toLong()).toInt()]
}
