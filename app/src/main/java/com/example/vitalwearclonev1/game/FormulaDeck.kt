package com.example.vitalwearclonev1.game

/**
 * The Slice 1 formula decks (2026-11-01). Two bands, because the kids
 * this is built for are in 4th, 8th, and 10th grade:
 * - GENTLE_3_5: the gentle deck for the youngest (4th grade).
 * - UPPER_6_PLUS: the grades 6+ deck for the two older kids.
 *
 * All explanations, examples, and tricks are original to this app.
 * Units are 4 cards each. A unit is mastered when every card in it is
 * at mastery level 2 or higher (level 3 is the gold "Mastered" card).
 */

/** Grades 6+ deck — the 8-card starter deck from the audit. */
val UPPER_FORMULA_CARDS: List<FormulaCard> = listOf(
    FormulaCard(
        id = "rect_area",
        name = "Rectangle Area",
        formulaSymbols = "A = l × w",
        formulaWords = "Area equals length times width.",
        workedExample = "A rectangle is 6 long and 4 wide. Multiply 6 × 4 to get 24 square units of area.",
        memoryTrick = "Area covers the floor. Multiply the two sides that meet at a corner, like counting tiles in rows.",
        band = FormulaBand.UPPER_6_PLUS,
        nameDistractors = listOf("P = 2(l + w)", "A = l + w", "A = 2(l × w)"),
        fillPrompt = "A = ___ × w",
        fillAnswer = "l",
        fillDistractors = listOf("2", "l + w", "P")
    ),
    FormulaCard(
        id = "rect_perimeter",
        name = "Rectangle Perimeter",
        formulaSymbols = "P = 2(l + w)",
        formulaWords = "Perimeter equals two times length plus width.",
        workedExample = "A rectangle is 6 long and 4 wide. Add 6 + 4 to get 10, then double it to get a perimeter of 20.",
        memoryTrick = "Walk all the way around: two long sides and two short sides, so add one of each, then double.",
        band = FormulaBand.UPPER_6_PLUS,
        prerequisites = listOf("rect_area"),
        nameDistractors = listOf("A = l × w", "P = l + w", "P = 2l × w"),
        fillPrompt = "P = 2( ___ + w)",
        fillAnswer = "l",
        fillDistractors = listOf("2", "A", "l × w")
    ),
    FormulaCard(
        id = "triangle_area",
        name = "Triangle Area",
        formulaSymbols = "A = ½ × b × h",
        formulaWords = "Area equals half the base times the height.",
        workedExample = "A triangle has a base of 8 and a height of 5. Half of 8 × 5 is half of 40, which is 20 square units.",
        memoryTrick = "A triangle is half a rectangle. Build the rectangle first, then cut it in half.",
        band = FormulaBand.UPPER_6_PLUS,
        prerequisites = listOf("rect_area"),
        nameDistractors = listOf("A = b × h", "A = b + h", "A = 2(b + h)"),
        fillPrompt = "A = ___ × b × h",
        fillAnswer = "½",
        fillDistractors = listOf("2", "b", "h")
    ),
    FormulaCard(
        id = "circle_area",
        name = "Circle Area",
        formulaSymbols = "A = πr²",
        formulaWords = "Area equals pi times the radius squared.",
        workedExample = "A circle has a radius of 3. Square the radius to get 9, then multiply by pi (about 3.14) to get about 28.26 square units.",
        memoryTrick = "Square the radius first. Area grows with the square, not with the radius itself, so never skip the squaring step.",
        band = FormulaBand.UPPER_6_PLUS,
        prerequisites = listOf("triangle_area"),
        nameDistractors = listOf("C = 2πr", "A = 2πr", "A = πd"),
        fillPrompt = "A = π × ___",
        fillAnswer = "r²",
        fillDistractors = listOf("r", "2r", "d")
    ),
    FormulaCard(
        id = "circle_circumference",
        name = "Circle Circumference",
        formulaSymbols = "C = 2πr",
        formulaWords = "Circumference equals two times pi times the radius.",
        workedExample = "A circle has a radius of 5. Two times pi (about 3.14) times 5 is about 31.4 units around the circle.",
        memoryTrick = "Around the circle is about six radii: two pi is about six, so the circumference is about six times the radius.",
        band = FormulaBand.UPPER_6_PLUS,
        prerequisites = listOf("circle_area"),
        nameDistractors = listOf("A = πr²", "C = πr", "C = 2r"),
        fillPrompt = "C = 2π × ___",
        fillAnswer = "r",
        fillDistractors = listOf("r²", "d", "2")
    ),
    FormulaCard(
        id = "cube_volume",
        name = "Cube Volume",
        formulaSymbols = "V = s³",
        formulaWords = "Volume equals side cubed.",
        workedExample = "A cube has a side of 4. Multiply 4 × 4 × 4 to get a volume of 64 cubic units.",
        memoryTrick = "Volume fills the cube. The side appears three times, not two, because the cube stretches three ways.",
        band = FormulaBand.UPPER_6_PLUS,
        prerequisites = listOf("rect_area"),
        nameDistractors = listOf("SA = 6s²", "V = s²", "V = 6s"),
        fillPrompt = "V = s^___",
        fillAnswer = "3",
        fillDistractors = listOf("2", "6", "s")
    ),
    FormulaCard(
        id = "cube_surface_area",
        name = "Cube Surface Area",
        formulaSymbols = "SA = 6s²",
        formulaWords = "Surface area equals six times side squared.",
        workedExample = "A cube has a side of 4. One face is 4 × 4 = 16, and a cube has six faces, so 6 × 16 = 96 square units.",
        memoryTrick = "A cube has six faces. Find one face first, then count all six of them.",
        band = FormulaBand.UPPER_6_PLUS,
        prerequisites = listOf("cube_volume"),
        nameDistractors = listOf("V = s³", "SA = s²", "SA = 6s³"),
        fillPrompt = "SA = 6 × ___",
        fillAnswer = "s²",
        fillDistractors = listOf("s", "s³", "6")
    ),
    FormulaCard(
        id = "pythagorean",
        name = "Pythagorean Theorem",
        formulaSymbols = "a² + b² = c²",
        formulaWords = "The two shorter sides squared add up to the longest side squared.",
        workedExample = "A right triangle has legs 3 and 4. Square them to get 9 and 16, add them to get 25, and the square root of 25 is 5, the longest side.",
        memoryTrick = "Square the legs, add them, then take the square root. The answer is always the slanted side.",
        band = FormulaBand.UPPER_6_PLUS,
        prerequisites = listOf("triangle_area"),
        nameDistractors = listOf("a + b = c", "a² − b² = c²", "a² + b² = c"),
        fillPrompt = "a² + b² = ___",
        fillAnswer = "c²",
        fillDistractors = listOf("c", "a²", "b²")
    )
)

/** Gentle grades 3–5 deck for the youngest (4th grade). */
val GENTLE_FORMULA_CARDS: List<FormulaCard> = listOf(
    FormulaCard(
        id = "gentle_perimeter",
        name = "Perimeter: Add All the Sides",
        formulaSymbols = "P = add all the sides",
        formulaWords = "Perimeter is the total distance around a shape.",
        workedExample = "A square has sides 3, 3, 3, and 3. Add them all: 3 + 3 + 3 + 3 = 12 around the shape.",
        memoryTrick = "Walk the fence all the way around and add every step. If you stop early, the fence has a gap.",
        band = FormulaBand.GENTLE_3_5,
        nameDistractors = listOf("Multiply two sides", "Count only the corners", "Add two sides"),
        fillPrompt = "Perimeter = add ___ the sides",
        fillAnswer = "all",
        fillDistractors = listOf("two", "one", "none")
    ),
    FormulaCard(
        id = "gentle_area_rows",
        name = "Area: Rows Times Columns",
        formulaSymbols = "A = rows × columns",
        formulaWords = "Area equals rows times columns.",
        workedExample = "A grid has 3 rows with 4 tiles in each row. Multiply 3 × 4 to get 12 tiles in all.",
        memoryTrick = "Count one row, count the rows, multiply. Tiles in a grid never need counting one by one.",
        band = FormulaBand.GENTLE_3_5,
        prerequisites = listOf("gentle_perimeter"),
        nameDistractors = listOf("A = rows + columns", "A = rows − columns", "A = 2 × rows"),
        fillPrompt = "A = rows × ___",
        fillAnswer = "columns",
        fillDistractors = listOf("rows", "sides", "corners")
    ),
    FormulaCard(
        id = "gentle_rectangle",
        name = "Rectangle: Area and Perimeter",
        formulaSymbols = "A = l × w, P = 2(l + w)",
        formulaWords = "A rectangle's area is length times width, and its perimeter is two times length plus width.",
        workedExample = "A rectangle is 5 long and 2 wide. Its area is 5 × 2 = 10, and its perimeter is 2 × (5 + 2) = 14.",
        memoryTrick = "Area fills it, perimeter walks it. Two jobs, two formulas, same rectangle — say which job you need first.",
        band = FormulaBand.GENTLE_3_5,
        prerequisites = listOf("gentle_area_rows", "gentle_perimeter"),
        nameDistractors = listOf("A = l × w", "P = 2(l + w)", "A = l + w"),
        fillPrompt = "P = 2(l + ___)",
        fillAnswer = "w",
        fillDistractors = listOf("l", "2", "A")
    ),
    FormulaCard(
        id = "gentle_triangle_area",
        name = "Triangle Area, Gently",
        formulaSymbols = "A = ½ × b × h",
        formulaWords = "Area equals half the base times the height.",
        workedExample = "A triangle has a base of 6 and a height of 4. Half of 6 × 4 is half of 24, which is 12 square units.",
        memoryTrick = "Half a rectangle again. Draw the rectangle, cut it diagonally, and keep one half.",
        band = FormulaBand.GENTLE_3_5,
        prerequisites = listOf("gentle_rectangle"),
        nameDistractors = listOf("A = b × h", "A = b + h", "A = 2(b + h)"),
        fillPrompt = "A = ___ × b × h",
        fillAnswer = "½",
        fillDistractors = listOf("2", "b", "h")
    )
)

val ALL_FORMULA_CARDS: List<FormulaCard> = GENTLE_FORMULA_CARDS + UPPER_FORMULA_CARDS

/** Units of 4 cards each. Order is the unlock order within and across bands. */
val FORMULA_UNITS: List<FormulaUnit> = listOf(
    FormulaUnit(
        id = "gentle_perimeter_area",
        name = "Perimeter & Area Basics",
        band = FormulaBand.GENTLE_3_5,
        cardIds = listOf("gentle_perimeter", "gentle_area_rows", "gentle_rectangle", "gentle_triangle_area")
    ),
    FormulaUnit(
        id = "upper_area_perimeter",
        name = "Area & Perimeter",
        band = FormulaBand.UPPER_6_PLUS,
        cardIds = listOf("rect_area", "rect_perimeter", "triangle_area", "circle_area")
    ),
    FormulaUnit(
        id = "upper_circles_cubes_triangles",
        name = "Circles, Cubes & Right Triangles",
        band = FormulaBand.UPPER_6_PLUS,
        cardIds = listOf("circle_circumference", "cube_volume", "cube_surface_area", "pythagorean")
    )
)

/** Which band's deck shows first for a grade picker value. */
fun formulaBandForGrade(grade: String): FormulaBand {
    val level = if (grade == "K") 0 else grade.toIntOrNull() ?: 1
    return if (level >= 6) FormulaBand.UPPER_6_PLUS else FormulaBand.GENTLE_3_5
}

fun formulaCardsForBand(band: FormulaBand): List<FormulaCard> =
    ALL_FORMULA_CARDS.filter { it.band == band }

fun formulaCardById(id: String): FormulaCard? = ALL_FORMULA_CARDS.firstOrNull { it.id == id }

fun formulaCardByName(name: String): FormulaCard? = ALL_FORMULA_CARDS.firstOrNull { it.name == name }

/**
 * Global unlock rule (not per grade): the gentle unit is always open.
 * The first upper unit opens when the gentle unit is mastered OR the
 * entry band is already upper. Each later unit opens when the previous
 * unit is mastered. [masteredUnitIds] are unit ids whose every card is
 * at mastery level 2 or higher.
 */
fun computeUnlockedUnits(
    entryBand: FormulaBand,
    masteredUnitIds: Set<String>
): List<FormulaUnit> {
    val unlocked = mutableListOf<FormulaUnit>()
    for ((index, unit) in FORMULA_UNITS.withIndex()) {
        val open = when (index) {
            0 -> true
            1 -> "gentle_perimeter_area" in masteredUnitIds || entryBand == FormulaBand.UPPER_6_PLUS
            else -> FORMULA_UNITS[index - 1].id in masteredUnitIds
        }
        if (open) unlocked.add(unit)
    }
    return unlocked
}
