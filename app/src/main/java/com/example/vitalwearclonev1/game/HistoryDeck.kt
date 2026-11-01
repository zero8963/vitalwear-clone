package com.example.vitalwearclonev1.game

/**
 * The Slice 1 history decks (2026-11-01). Two bands, because the kids
 * this is built for are in 4th, 8th, and 10th grade:
 * - GENTLE_3_5: the gentle deck for the youngest (4th grade).
 * - UPPER_6_PLUS: the grades 6+ deck for the two older kids.
 *
 * Slice 1 leads with America's beginnings for the gentle unit and
 * the world timeline for the upper unit, so the two units together
 * cover both strands; the modern century (Great Depression → World
 * War II → Cold War → Space Race) is the natural Upper Unit 2.
 * All what-happened, why-it-mattered, and hook text is original to
 * this app. Units are 4 cards each, in chronological order — the
 * deck itself teaches the timeline. A unit is mastered when every
 * card in it is at mastery level 2 or higher (level 3 is the gold
 * "Mastered" card).
 */

/** Gentle grades 3–5 deck for the youngest (4th grade): America's beginnings. */
val GENTLE_HISTORY_CARDS: List<HistoryCard> = listOf(
    HistoryCard(
        id = "hist_native_cultures",
        eventName = "Native American Cultures",
        eraLabel = "Before European Arrival",
        dateLabel = "long before 1492",
        dateSortKey = -10000,
        description = "For thousands of years, many different peoples lived across North America, farming, hunting, fishing, trading, and building towns and cities in their own ways.",
        whyItMattered = "When later newcomers arrived, they met lands already full of nations with their own governments, languages, and trade routes — that is why the story of America starts before any ocean crossing.",
        memoryHook = "Think 'first peoples, first' — they were here first, for thousands of years.",
        cause = "Peoples moved into North America over thousands of years and settled in many regions.",
        consequence = "Dozens of distinct nations with their own languages and ways of life were living here when Europeans arrived.",
        band = HistoryBand.GENTLE_3_5,
        dateDistractors = listOf("1492", "1776", "1787"),
        eraDistractors = listOf("European Voyages", "American Revolution", "A New Government")
    ),
    HistoryCard(
        id = "hist_explorers",
        eventName = "Explorers of the World",
        eraLabel = "European Voyages",
        dateLabel = "1492",
        dateSortKey = 1492,
        description = "Sailors from Europe crossed the Atlantic in ships, reaching the Americas and opening a long exchange of goods, plants, animals, and diseases between continents.",
        whyItMattered = "These voyages linked Europe, Africa, and the Americas in new trade routes, but they also brought disease and conflict that harmed Native peoples — the same voyages had very different results for different people.",
        memoryHook = "1492: sails across the sea — but remember who was already home.",
        cause = "European rulers wanted faster trade routes to Asia and paid for ocean voyages.",
        consequence = "Europe, Africa, and the Americas became linked by trade — and by disease and conquest that devastated Native nations.",
        band = HistoryBand.GENTLE_3_5,
        prerequisites = listOf("hist_native_cultures"),
        dateDistractors = listOf("long before 1492", "1776", "1787"),
        eraDistractors = listOf("Before European Arrival", "American Revolution", "A New Government")
    ),
    HistoryCard(
        id = "hist_declaration",
        eventName = "The Declaration of Independence",
        eraLabel = "American Revolution",
        dateLabel = "1776",
        dateSortKey = 1776,
        description = "Delegates from thirteen colonies signed a paper saying they were breaking away from Britain and forming their own country, listing their reasons and the rights they believed all people had.",
        whyItMattered = "It turned a rebellion into a claim about rights — that governments exist by the consent of the governed — an idea later movements borrowed around the world.",
        memoryHook = "1776: the colonies declare — hold onto the promise that governments need the people's consent.",
        cause = "Britain taxed the colonies and ruled them without giving them a vote in Parliament.",
        consequence = "The colonies became the United States and fought a war to make independence real.",
        band = HistoryBand.GENTLE_3_5,
        prerequisites = listOf("hist_explorers"),
        dateDistractors = listOf("1787", "1765", "1800"),
        eraDistractors = listOf("Before European Arrival", "European Voyages", "A New Government")
    ),
    HistoryCard(
        id = "hist_constitution",
        eventName = "The Constitution",
        eraLabel = "A New Government",
        dateLabel = "1787",
        dateSortKey = 1787,
        description = "After independence, leaders wrote a plan for the new country's government, splitting power between three branches and between the states and the national government.",
        whyItMattered = "It is still the basic rulebook for how the United States governs itself, and its amendments let later generations expand who counts as a citizen with rights.",
        memoryHook = "1787: the rulebook year — one year after the Declaration's promise, the plan to keep it.",
        cause = "The first plan of government proved too weak to pay debts or settle disputes between states.",
        consequence = "A stronger national government with checks and balances took shape, and the Bill of Rights followed.",
        band = HistoryBand.GENTLE_3_5,
        prerequisites = listOf("hist_declaration"),
        dateDistractors = listOf("1776", "1791", "1800"),
        eraDistractors = listOf("Before European Arrival", "European Voyages", "American Revolution")
    )
)

/** Grades 6+ deck for the two older kids: how the modern world took shape. */
val UPPER_HISTORY_CARDS: List<HistoryCard> = listOf(
    HistoryCard(
        id = "hist_magna_carta",
        eventName = "The Magna Carta",
        eraLabel = "Limits on Kings",
        dateLabel = "1215",
        dateSortKey = 1215,
        description = "English barons forced King John to sign a charter promising he would follow certain laws, not jail free men without a fair trial, and not tax without agreement.",
        whyItMattered = "It planted the idea that even a king is under the law — a seed that grew into parliaments and bills of rights centuries later.",
        memoryHook = "1215: the king signs limits — a great charter that charted limits on power.",
        cause = "King John raised heavy taxes and jailed opponents, and his barons rebelled.",
        consequence = "The idea that rulers must obey laws spread, and later documents borrowed its promises.",
        band = HistoryBand.UPPER_6_PLUS,
        dateDistractors = listOf("1066", "1300", "1216"),
        eraDistractors = listOf("Rebirth of Learning", "Machines and Cities", "Global War")
    ),
    HistoryCard(
        id = "hist_renaissance",
        eventName = "The Renaissance",
        eraLabel = "Rebirth of Learning",
        dateLabel = "c. 1300–1600",
        dateSortKey = 1300,
        description = "Across Europe, artists, scholars, and inventors revived the learning of ancient Greece and Rome, painted with new realism, and printed books that spread ideas fast.",
        whyItMattered = "It changed how Europeans thought about art, science, and the individual — and the printing press meant new ideas could reach ordinary readers, not just scholars.",
        memoryHook = "Rebirth starts with R — a rebirth of art and learning after the Middle Ages.",
        cause = "Trade wealth in Italian cities paid for art and study, and ancient texts reached new readers.",
        consequence = "New art, new science, and new questions about old authorities spread across Europe.",
        band = HistoryBand.UPPER_6_PLUS,
        prerequisites = listOf("hist_magna_carta"),
        dateDistractors = listOf("1215", "1760", "c. 1100–1200"),
        eraDistractors = listOf("Limits on Kings", "Machines and Cities", "Global War")
    ),
    HistoryCard(
        id = "hist_industrial",
        eventName = "The Industrial Revolution",
        eraLabel = "Machines and Cities",
        dateLabel = "c. 1760–1840",
        dateSortKey = 1760,
        description = "New machines, steam power, and factories changed how goods were made, moving work from homes and workshops into mills and pulling families into fast-growing cities.",
        whyItMattered = "It made goods cheaper and plentiful, but also created crowded cities, long workdays, and later labor laws — the modern economy's promise and its problems arrived together.",
        memoryHook = "Steam and steel: factories first, then cities filled — industry remade daily life.",
        cause = "Coal, iron, and new machines like the steam engine made factory production possible.",
        consequence = "Cities swelled, goods became cheaper, and workers eventually organized for safer hours and pay.",
        band = HistoryBand.UPPER_6_PLUS,
        prerequisites = listOf("hist_renaissance"),
        dateDistractors = listOf("1914", "c. 1300–1600", "c. 1840–1900"),
        eraDistractors = listOf("Limits on Kings", "Rebirth of Learning", "Global War")
    ),
    HistoryCard(
        id = "hist_wwi",
        eventName = "World War I",
        eraLabel = "Global War",
        dateLabel = "1914–1918",
        dateSortKey = 1914,
        description = "A war that began in Europe in 1914 pulled in empires and colonies around the world, fought with trenches, new weapons, and millions of soldiers.",
        whyItMattered = "It redrew maps, toppled empires, and set the stage for the next decades of conflict — the peace treaty that ended it left grievances that later leaders exploited.",
        memoryHook = "1914: the war that was supposed to end all wars — and instead shaped the century after it.",
        cause = "Rival alliances, an arms race, and competition over colonies turned a crisis in the Balkans into a world war.",
        consequence = "Empires fell, new countries appeared, and the harsh peace fed resentment that helped spark another world war.",
        band = HistoryBand.UPPER_6_PLUS,
        prerequisites = listOf("hist_industrial"),
        dateDistractors = listOf("1939–1945", "1760", "c. 1300–1600"),
        eraDistractors = listOf("Limits on Kings", "Rebirth of Learning", "Machines and Cities")
    )
)

val ALL_HISTORY_CARDS: List<HistoryCard> = GENTLE_HISTORY_CARDS + UPPER_HISTORY_CARDS

/** Units of 4 cards each, in chronological order. Order is the unlock order within and across bands. */
val HISTORY_UNITS: List<HistoryUnit> = listOf(
    HistoryUnit(
        id = "gentle_americas_beginnings",
        name = "America's Beginnings",
        band = HistoryBand.GENTLE_3_5,
        cardIds = listOf("hist_native_cultures", "hist_explorers", "hist_declaration", "hist_constitution")
    ),
    HistoryUnit(
        id = "upper_modern_world",
        name = "How the Modern World Took Shape",
        band = HistoryBand.UPPER_6_PLUS,
        cardIds = listOf("hist_magna_carta", "hist_renaissance", "hist_industrial", "hist_wwi")
    )
)

/** Which band's deck shows first for a grade picker value. */
fun historyBandForGrade(grade: String): HistoryBand {
    val level = if (grade == "K") 0 else grade.toIntOrNull() ?: 1
    return if (level >= 6) HistoryBand.UPPER_6_PLUS else HistoryBand.GENTLE_3_5
}

fun historyCardsForBand(band: HistoryBand): List<HistoryCard> =
    ALL_HISTORY_CARDS.filter { it.band == band }

fun historyCardById(id: String): HistoryCard? = ALL_HISTORY_CARDS.firstOrNull { it.id == id }

fun historyCardByEventName(name: String): HistoryCard? = ALL_HISTORY_CARDS.firstOrNull { it.eventName == name }

/**
 * Global unlock rule (not per grade), mirroring formulas/spelling:
 * the gentle unit is always open. The first upper unit opens when
 * the gentle unit is mastered OR the entry band is already upper.
 * Each later unit opens when the previous unit is mastered.
 * [masteredUnitIds] are unit ids whose every card is at mastery
 * level 2 or higher.
 */
fun computeUnlockedHistoryUnits(
    entryBand: HistoryBand,
    masteredUnitIds: Set<String>
): List<HistoryUnit> {
    val unlocked = mutableListOf<HistoryUnit>()
    for ((index, unit) in HISTORY_UNITS.withIndex()) {
        val open = when (index) {
            0 -> true
            1 -> "gentle_americas_beginnings" in masteredUnitIds || entryBand == HistoryBand.UPPER_6_PLUS
            else -> HISTORY_UNITS[index - 1].id in masteredUnitIds
        }
        if (open) unlocked.add(unit)
    }
    return unlocked
}
