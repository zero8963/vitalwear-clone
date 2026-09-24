package com.example.vitalwearclonev1.game

import kotlin.random.Random

class GeometryProblemGenerator(private val grade: String, private val floor: Int, private val isAdjusted: Boolean = false) {
    private val random = Random(System.currentTimeMillis())
    private val questionCounts = mutableMapOf<String, Int>()

    fun generateLesson(seenTitles: List<String> = emptyList()): Lesson {
        val gradeLevel = when(grade) {
            "K" -> 0
            else -> grade.toIntOrNull() ?: 1
        }
        
        // Titles below are in the same order as the lessonIdx branches per band.
        val bandTitles = when {
            gradeLevel <= 2 -> listOf("Basic Shapes", "Counting Corners", "Inside vs. Outside", "Curves vs. Straight Lines", "Pentagons and Hexagons", "Sorting Shapes", "Meet the 3D Shapes", "First Lines of Symmetry")
            gradeLevel <= 5 -> listOf("Perimeter and Area", "Identifying Polygons", "Types of Angles", "Parallel vs. Perpendicular", "Quadrilaterals", "Right Angles", "Congruent Shapes", "The Coordinate Grid")
            else -> listOf("The Pythagorean Theorem", "Volume of 3D Shapes", "Symmetry", "Pi (π) and Circles", "Area of Circles", "Surface Area", "Slides, Flips, and Turns", "Angles in a Triangle")
        }
        val unseenTitles = bandTitles.filter { it !in seenTitles }
        // Paced learning: work through new skills in a stable, gentle order instead of jumping randomly.
        val selectedTitle = if (unseenTitles.isNotEmpty()) {
            if (isAdjusted) unseenTitles.first() else unseenTitles.random(random)
        } else bandTitles.random(random)
        val lessonIdx = bandTitles.indexOf(selectedTitle)

        return when {
            gradeLevel <= 2 -> {
                when (lessonIdx) {
                    0 -> {
                        val answer = "3"
                        Lesson(
                            title = "Basic Shapes",
                            explanation = "Shapes are all around us! We identify them by their sides and corners.",
                            steps = listOf(
                                "1. Circle: Round and has zero corners.",
                                "2. Triangle: Has 3 sides and 3 corners.",
                                "3. Square: Has 4 equal sides and 4 corners."
                            ),
                            example = "A pizza slice is shaped like a triangle!",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "How many sides does a triangle have?",
                            practiceAnswer = answer,
                            practiceOptions = listOf("3", "4", "0", "5").shuffled(),
                            solveSteps = listOf("A triangle has 3 sides and 3 corners.")
                        )
                    }
                    1 -> {
                        val answer = "4"
                        Lesson(
                            title = "Counting Corners",
                            explanation = "Corners (vertices) are where two sides of a shape meet.",
                            steps = listOf(
                                "1. Look at a shape.",
                                "2. Point to each sharp spot where sides join.",
                                "3. Count them one by one."
                            ),
                            example = "A square has 4 corners.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "How many corners does a square have?",
                            practiceAnswer = answer,
                            practiceOptions = listOf("4", "3", "5", "0").shuffled(),
                            solveSteps = listOf("A square has 4 sides and 4 corners.")
                        )
                    }
                    2 -> {
                        Lesson(
                            title = "Inside vs. Outside",
                            explanation = "Geometry also helps us describe where things are in relation to shapes.",
                            steps = listOf(
                                "1. A closed shape has an interior (inside).",
                                "2. The boundary is the line of the shape.",
                                "3. Everything else is the exterior (outside)."
                            ),
                            example = "Your drawing is inside the box.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "Is the center of a circle inside or outside the circle?",
                            practiceAnswer = "inside",
                            practiceOptions = listOf("inside", "outside").shuffled(),
                            solveSteps = listOf("The center is the very middle of the circle, so it's inside.")
                        )
                    }
                    3 -> {
                        Lesson(
                            title = "Curves vs. Straight Lines",
                            explanation = "Some shapes are made of straight lines, and some are made of curves.",
                            steps = listOf(
                                "1. A square has straight lines.",
                                "2. A circle is one continuous curve.",
                                "3. A heart has both!"
                            ),
                            example = "A ruler has straight edges. A coin is curved.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "Is a square made of curves or straight lines?",
                            practiceAnswer = "straight lines",
                            practiceOptions = listOf("straight lines", "curves").shuffled(),
                            solveSteps = listOf("A square has 4 straight sides.")
                        )
                    }
                    4 -> {
                        Lesson(
                            title = "Pentagons and Hexagons",
                            explanation = "A pentagon has 5 sides. A hexagon has 6 sides. Count the sides to name the shape!",
                            steps = listOf(
                                "1. Count all the straight sides.",
                                "2. 5 sides = pentagon. 6 sides = hexagon.",
                                "3. A stop sign is an octagon - it has 8 sides!"
                            ),
                            example = "A soccer ball is covered in pentagons and hexagons!",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "How many sides does a hexagon have?",
                            practiceAnswer = "6",
                            practiceOptions = listOf("6", "5", "8", "4").shuffled(),
                            solveSteps = listOf("Hexa means six", "A hexagon has 6 sides")
                        )
                    }
                    5 -> {
                        Lesson(
                            title = "Sorting Shapes",
                            explanation = "We sort shapes by what makes them special: number of sides, round or straight, big or small!",
                            steps = listOf(
                                "1. Pick a rule: for example, 'has 4 sides'.",
                                "2. Check each shape against the rule.",
                                "3. Shapes that match go in one group!"
                            ),
                            example = "Circles in one box, triangles in another!",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "Which shape does NOT belong with the squares?",
                            practiceAnswer = "Triangle",
                            practiceOptions = listOf("Triangle", "Rectangle", "Square", "Diamond").shuffled(),
                            solveSteps = listOf("Squares, rectangles, and diamonds all have 4 sides", "A triangle has 3 - it doesn't belong!")
                        )
                    }
                    6 -> {
                        Lesson(
                            title = "Meet the 3D Shapes",
                            explanation = "3D shapes are solid! A cube has square faces, a sphere is a ball, and a cylinder looks like a can.",
                            steps = listOf(
                                "1. Cube: 6 square faces (like a dice).",
                                "2. Sphere: perfectly round (like a ball).",
                                "3. Cylinder: two circles with a tube (like a soup can)."
                            ),
                            example = "An ice cream cone is... a cone!",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "Which 3D shape looks like a ball?",
                            practiceAnswer = "Sphere",
                            practiceOptions = listOf("Sphere", "Cube", "Cylinder", "Cone").shuffled(),
                            solveSteps = listOf("A ball is perfectly round", "The round 3D shape is a sphere")
                        )
                    }
                    else -> {
                        Lesson(
                            title = "First Lines of Symmetry",
                            explanation = "A line of symmetry cuts a shape into two mirror halves. Fold it - both sides match!",
                            steps = listOf(
                                "1. Imagine folding the shape in half.",
                                "2. If both halves match exactly, the fold is a line of symmetry.",
                                "3. A circle has endless lines of symmetry!"
                            ),
                            example = "A heart folded down the middle matches perfectly!",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "How many lines of symmetry does a square have?",
                            practiceAnswer = "4",
                            practiceOptions = listOf("4", "2", "1", "8").shuffled(),
                            solveSteps = listOf("Fold vertical, horizontal, and both diagonals", "That's 4 matching folds")
                        )
                    }
                }
            }
            gradeLevel <= 5 -> {
                when (lessonIdx) {
                    0 -> {
                        val w = random.nextInt(2, 6)
                        val h = random.nextInt(2, 6)
                        val area = w * h
                        Lesson(
                            title = "Perimeter and Area",
                            explanation = "Perimeter is the distance around a shape, and Area is the space inside.",
                            steps = listOf(
                                "1. Perimeter: Add all the side lengths together.",
                                "2. Area (Rectangle): Multiply the length by the width.",
                                "3. Area (Triangle): Multiply base by height and divide by 2."
                            ),
                            example = "A 4x5 rectangle has a perimeter of 18 and an area of 20.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "What is the area of a ${w}x${h} rectangle?",
                            practiceAnswer = area.toString(),
                            practiceOptions = listOf(area.toString(), (w + h).toString(), (2 * (w + h)).toString(), (area + 5).toString()).shuffled(),
                            solveSteps = listOf("Length = $w, Width = $h", "Area = $w x $h", "Area = $area")
                        )
                    }
                    1 -> {
                        val n = random.nextInt(3, 7)
                        val names = mapOf(3 to "Triangle", 4 to "Square/Rectangle", 5 to "Pentagon", 6 to "Hexagon")
                        val answer = names[n] ?: "Polygon"
                        Lesson(
                            title = "Identifying Polygons",
                            explanation = "Polygons are closed shapes with straight sides. We name them based on the number of sides.",
                            steps = listOf(
                                "1. Count the straight sides.",
                                "2. Use the correct prefix (tri-, quad-, penta-, hexa-).",
                                "3. A 'regular' polygon has all sides equal."
                            ),
                            example = "A 5-sided shape is a pentagon.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "What do you call a polygon with $n sides?",
                            practiceAnswer = answer,
                            practiceOptions = names.values.toList().shuffled(),
                            solveSteps = listOf("The prefix for $n is ${answer.take(3).lowercase()}", "So it's a $answer")
                        )
                    }
                    2 -> {
                        Lesson(
                            title = "Types of Angles",
                            explanation = "Angles measure the turn between two lines. We use a protractor to measure them in degrees.",
                            steps = listOf(
                                "1. Right Angle: Exactly 90 degrees (looks like an L).",
                                "2. Acute Angle: Less than 90 degrees (small and sharp).",
                                "3. Obtuse Angle: Greater than 90 degrees (wide)."
                            ),
                            example = "The corner of a book is a right angle.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "Is a 45-degree angle acute, obtuse, or right?",
                            practiceAnswer = "acute",
                            practiceOptions = listOf("acute", "obtuse", "right").shuffled(),
                            solveSteps = listOf("45 is less than 90", "Angles less than 90 are 'acute' (small and cute!)")
                        )
                    }
                    3 -> {
                        Lesson(
                            title = "Parallel vs. Perpendicular",
                            explanation = "Lines have special relationships based on if they cross or not.",
                            steps = listOf(
                                "1. Parallel lines never touch (like train tracks).",
                                "2. Perpendicular lines cross at a 90-degree angle (like a cross).",
                                "3. Intersecting lines cross at any angle."
                            ),
                            example = "The sides of a ladder are parallel.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "What do you call lines that never touch?",
                            practiceAnswer = "parallel",
                            practiceOptions = listOf("parallel", "perpendicular", "intersecting").shuffled(),
                            solveSteps = listOf("Parallel lines stay the same distance apart forever.")
                        )
                    }
                    4 -> {
                        Lesson(
                            title = "Quadrilaterals",
                            explanation = "Quadrilateral means 'four sides'! Squares, rectangles, and diamonds are all quadrilaterals.",
                            steps = listOf(
                                "1. Count the sides: quadrilaterals have exactly 4.",
                                "2. Squares and rectangles also have 4 right angles.",
                                "3. A trapezoid has only one pair of parallel sides."
                            ),
                            example = "A kite is a quadrilateral too!",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "How many sides does a quadrilateral have?",
                            practiceAnswer = "4",
                            practiceOptions = listOf("4", "3", "5", "6").shuffled(),
                            solveSteps = listOf("'Quad' means four", "A quadrilateral has 4 sides")
                        )
                    }
                    5 -> {
                        Lesson(
                            title = "Right Angles",
                            explanation = "A right angle is a perfect corner, like the corner of a book. It measures exactly 90 degrees!",
                            steps = listOf(
                                "1. Look for square corners.",
                                "2. A right angle makes a perfect 'L' shape.",
                                "3. Rectangles have four right angles!"
                            ),
                            example = "The hands of a clock at 3:00 make a right angle!",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "How many degrees is a right angle?",
                            practiceAnswer = "90",
                            practiceOptions = listOf("90", "45", "180", "60").shuffled(),
                            solveSteps = listOf("A right angle is a perfect corner", "It measures exactly 90 degrees")
                        )
                    }
                    6 -> {
                        Lesson(
                            title = "Congruent Shapes",
                            explanation = "Congruent shapes are exactly the same size and shape - like twins! They can be turned or flipped.",
                            steps = listOf(
                                "1. Check: same shape?",
                                "2. Check: same size?",
                                "3. If both match, they're congruent - even if one is rotated!"
                            ),
                            example = "Two identical cookies are congruent!",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "Which word means 'same size and same shape'?",
                            practiceAnswer = "Congruent",
                            practiceOptions = listOf("Congruent", "Similar", "Round", "Parallel").shuffled(),
                            solveSteps = listOf("Same size + same shape", "The word is 'congruent'")
                        )
                    }
                    else -> {
                        Lesson(
                            title = "The Coordinate Grid",
                            explanation = "A coordinate grid is a map for math! Go right along x, then up along y to find any point.",
                            steps = listOf(
                                "1. Start at (0, 0), the origin.",
                                "2. First number: walk right.",
                                "3. Second number: climb up. You found the point!"
                            ),
                            example = "In Battleship, 'B-4' works just like coordinates!",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "On a grid, you walk right 3 then up 2. What point are you at?",
                            practiceAnswer = "(3, 2)",
                            practiceOptions = listOf("(3, 2)", "(2, 3)", "(5, 0)", "(0, 5)").shuffled(),
                            solveSteps = listOf("Right 3 -> x = 3", "Up 2 -> y = 2", "Point: (3, 2)")
                        )
                    }
                }
            }
            else -> {
                when (lessonIdx) {
                    0 -> {
                        val a = 3
                        val b = 4
                        Lesson(
                            title = "The Pythagorean Theorem",
                            explanation = "In a right triangle, the sides have a special relationship.",
                            steps = listOf(
                                "1. Identify the 'legs' (a and b) and the 'hypotenuse' (c).",
                                "2. Square the legs: a² + b².",
                                "3. The sum equals the square of the hypotenuse: c²."
                            ),
                            example = "In a triangle with sides 3 and 4, the hypotenuse is 5 (9 + 16 = 25).",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "In a right triangle, if a=$a and b=$b, what is c?",
                            practiceAnswer = "5",
                            practiceOptions = listOf("5", "7", "12", "25").shuffled(),
                            solveSteps = listOf("a² = $a x $a = 9", "b² = $b x $b = 16", "c² = 9 + 16 = 25", "c = √25 = 5")
                        )
                    }
                    1 -> {
                        val s = random.nextInt(2, 5)
                        val volume = s * s * s
                        Lesson(
                            title = "Volume of 3D Shapes",
                            explanation = "Volume measures how much space a 3D object takes up, like how much water a bottle holds.",
                            steps = listOf(
                                "1. Find the area of the base.",
                                "2. Multiply the area of the base by the height.",
                                "3. For a cube, it's just side x side x side (s³)."
                            ),
                            example = "A 2x2x2 cube has a volume of 8.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "What is the volume of a ${s}x${s}x${s} cube?",
                            practiceAnswer = volume.toString(),
                            practiceOptions = listOf(volume.toString(), (s * 3).toString(), (s * s).toString(), (volume + 10).toString()).shuffled(),
                            solveSteps = listOf("Side = $s", "Volume = $s x $s x $s", "Volume = $volume")
                        )
                    }
                    2 -> {
                        Lesson(
                            title = "Symmetry",
                            explanation = "A shape has symmetry if you can fold it in half and both sides match perfectly.",
                            steps = listOf(
                                "1. Imagine a line going through the shape.",
                                "2. Flip one side over the line.",
                                "3. If they are identical, that line is a Line of Symmetry."
                            ),
                            example = "A butterfly or a heart has one line of symmetry.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "How many lines of symmetry does a square have?",
                            practiceAnswer = "4",
                            practiceOptions = listOf("4", "2", "1", "8").shuffled(),
                            solveSteps = listOf("Vertical, Horizontal, and two Diagonals.", "Total = 4")
                        )
                    }
                    3 -> {
                        Lesson(
                            title = "Pi (π) and Circles",
                            explanation = "Pi is a special number (about 3.14) that helps us find the distance around a circle.",
                            steps = listOf(
                                "1. Circumference is the distance around.",
                                "2. C = 2 x π x radius.",
                                "3. π is the ratio of circumference to diameter."
                            ),
                            example = "If radius is 1, Circumference is about 6.28.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "What is the approximate value of Pi?",
                            practiceAnswer = "3.14",
                            practiceOptions = listOf("3.14", "2.14", "4.14", "1.14").shuffled(),
                            solveSteps = listOf("Pi is approximately 22/7 or 3.14159...")
                        )
                    }
                    4 -> {
                        Lesson(
                            title = "Area of Circles",
                            explanation = "The area of a circle is pi times radius squared: A = πr². Pi is about 3.14!",
                            steps = listOf(
                                "1. Find the radius (center to edge).",
                                "2. Square it (multiply by itself).",
                                "3. Multiply by pi (about 3.14)."
                            ),
                            example = "Radius 3: 3² = 9, x 3.14 = 28.26.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "What is the formula for the area of a circle?",
                            practiceAnswer = "π x r²",
                            practiceOptions = listOf("π x r²", "2 x π x r", "π x d", "r + r").shuffled(),
                            solveSteps = listOf("Area needs radius squared", "Times pi: A = π x r²")
                        )
                    }
                    5 -> {
                        val s = random.nextInt(2, 5)
                        val answer = 6 * s * s
                        Lesson(
                            title = "Surface Area",
                            explanation = "Surface area is the total area of all faces of a 3D shape. A cube has 6 identical faces!",
                            steps = listOf(
                                "1. Find the area of one face.",
                                "2. Count how many faces the shape has.",
                                "3. Multiply: one face x number of faces."
                            ),
                            example = "Cube with side 2: one face = 4, x 6 faces = 24.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "What is the surface area of a cube with side $s?",
                            practiceAnswer = answer.toString(),
                            practiceOptions = listOf(answer.toString(), (s*s).toString(), (s*s*s).toString(), (4*s*s).toString()).shuffled(),
                            solveSteps = listOf("One face: $s x $s = ${s*s}", "A cube has 6 faces", "6 x ${s*s} = $answer")
                        )
                    }
                    6 -> {
                        Lesson(
                            title = "Slides, Flips, and Turns",
                            explanation = "Shapes can move without changing! Slide = translation, flip = reflection, turn = rotation.",
                            steps = listOf(
                                "1. Slide (translation): move without turning.",
                                "2. Flip (reflection): mirror image.",
                                "3. Turn (rotation): spin around a point."
                            ),
                            example = "A somersault is a rotation!",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "Which move makes a mirror image of a shape?",
                            practiceAnswer = "Flip",
                            practiceOptions = listOf("Flip", "Slide", "Turn", "Grow").shuffled(),
                            solveSteps = listOf("A mirror image is backwards", "The flip (reflection) makes mirror images")
                        )
                    }
                    else -> {
                        val a1 = random.nextInt(30, 80)
                        val a2 = random.nextInt(30, 100 - a1)
                        val answer = 180 - a1 - a2
                        Lesson(
                            title = "Angles in a Triangle",
                            explanation = "The three angles inside any triangle always add up to exactly 180 degrees!",
                            steps = listOf(
                                "1. Add the two angles you know.",
                                "2. Subtract from 180.",
                                "3. That's your missing angle!"
                            ),
                            example = "Angles 50° and 60°: 180 - 110 = 70°.",
                            type = LessonType.HOW_TO_SOLVE,
                            practiceQuestion = "A triangle has angles of $a1° and $a2°. What is the third angle?",
                            practiceAnswer = answer.toString(),
                            practiceOptions = listOf(answer.toString(), (answer + 10).toString(), (180 - a1).toString(), "90").shuffled(),
                            solveSteps = listOf("$a1 + $a2 = ${a1+a2}", "180 - ${a1+a2} = $answer")
                        )
                    }
                }
            }
        }
    }

    private val k2Geometry = listOf(
        Pair("Which shape has 3 sides?", "Triangle"),
        Pair("A square has ___ equal sides.", "4"),
        Pair("Which shape is round like a ball?", "Circle"),
        Pair("A rectangle has ___ corners.", "4"),
        Pair("Which shape looks like an egg?", "Oval"),
        Pair("A stop sign is an ___.", "Octagon"),
        Pair("How many sides does a rectangle have?", "4"),
        Pair("A ___ has no straight sides.", "Circle"),
        Pair("A diamond shape is also called a ___.", "Rhombus"),
        Pair("A star has ___ points.", "5"),
        Pair("A cube looks like a ___.", "Box"),
        Pair("Which shape has 4 sides but isn't a square?", "Rectangle"),
        Pair("A cylinder looks like a ___.", "Can"),
        Pair("A cone looks like a party ___.", "Hat"),
        Pair("A sphere looks like a ___.", "Ball"),
            Pair("A triangle has ___ corners.", "3"),
            Pair("Which 3D shape has 6 square faces?", "Cube"),
    )

    private val elementaryGeometry = listOf(
        Pair("What is the perimeter of a 5x4 rectangle?", "18"),
        Pair("An angle less than 90 degrees is ___.", "Acute"),
        Pair("How many faces does a cube have?", "6"),
        Pair("The area of a 3x6 rectangle is ___.", "18"),
        Pair("A 90-degree angle is called a ___ angle.", "Right"),
        Pair("A triangle with two equal sides is ___.", "Isosceles"),
        Pair("The distance around a circle is the ___.", "Circumference"),
        Pair("A polygon with 5 sides is a ___.", "Pentagon"),
        Pair("Parallel lines ___ meet.", "Never"),
        Pair("A line from the center of a circle to the edge is the ___.", "Radius"),
        Pair("An angle greater than 90 degrees is ___.", "Obtuse"),
        Pair("A quadrilateral has ___ sides.", "4"),
        Pair("The total degrees in a square is ___.", "360"),
        Pair("Symmetry means one half is a ___ of the other.", "Mirror"),
        Pair("A ___ is a 3D shape with 12 edges.", "Cube")
    )

    private val middleGeometry = listOf(
        Pair("In a right triangle, a² + b² = ___.", "c²"),
        Pair("What is the volume of a 2x3x4 box?", "24"),
        Pair("The sum of angles in a triangle is ___.", "180°"),
        Pair("A line touching a circle at one point is a ___.", "Tangent"),
        Pair("Two lines that never meet are ___.", "Parallel"),
        Pair("The area of a triangle is 1/2 * base * ___.", "Height"),
        Pair("A ___ is a transformation that flips a shape.", "Reflection"),
        Pair("The ratio of circumference to diameter is ___.", "Pi"),
        Pair("Vertical angles are always ___.", "Equal"),
        Pair("A ___ is a 7-sided polygon.", "Heptagon"),
        Pair("Congruent shapes are the ___ size and shape.", "Same"),
        Pair("The ___ is the longest side of a right triangle.", "Hypotenuse"),
        Pair("Complementary angles sum to ___ degrees.", "90"),
        Pair("Supplementary angles sum to ___ degrees.", "180"),
        Pair("A ___ is a movement of a shape without rotating.", "Translation"),
            Pair("What is the perimeter of a 6x4 rectangle?", "20"),
    )

    private val highGeometry = listOf(
        Pair("What is the area of a circle with radius 3? (use π=3.14)", "28.26"),
        Pair("In trigonometry, Sine = Opposite / ___.", "Hypotenuse"),
        Pair("A polygon with 8 sides is a ___.", "Octagon"),
        Pair("What is the slope of a line between (1,1) and (3,3)?", "1"),
        Pair("The longest side of a right triangle is the ___.", "Hypotenuse"),
        Pair("The Law of Cosines relates the sides and an ___ of a triangle.", "Angle"),
        Pair("A ___ is a set of all points equidistant from a point.", "Circle"),
        Pair("The volume of a sphere is (4/3) * π * ___.", "r³"),
        Pair("An ellipse has two focus points called ___.","Foci"),
        Pair("The distance formula is derived from the ___ Theorem.","Pythagorean"),
        Pair("Cosine is defined as Adjacent / ___.","Hypotenuse"),
        Pair("A tangent line is ___ to the radius at that point.","Perpendicular"),
        Pair("Vectors have both magnitude and ___.","Direction"),
        Pair("The area of a trapezoid is ((a+b)/2) * ___.","h"),
        Pair("Radian measure relates arc length to ___.","Radius"),
            Pair("What is the slope of a line through (0,0) and (2,6)?","3"),
    )

    // Random rectangle perimeter (3-5); distractors target classic confusions (area vs perimeter)
    private fun randomPerimeterProblem(): Problem {
        val w = random.nextInt(2, 10)
        val h = random.nextInt(2, 10)
        val answer = 2 * (w + h)
        val options = mutableSetOf(answer, w * h, w + h)
        while (options.size < 4) options.add(answer + random.nextInt(1, 6))
        return Problem(
            question = "What is the perimeter of a ${w}x${h} rectangle?",
            correctAnswer = answer.toString(),
            options = options.toList().shuffled().map { it.toString() }
        )
    }

    // Polygon naming by side count (2-5)
    private fun randomPolygonNameProblem(): Problem {
        val sides = random.nextInt(3, 9)
        val names = mapOf(3 to "Triangle", 4 to "Quadrilateral", 5 to "Pentagon", 6 to "Hexagon", 7 to "Heptagon", 8 to "Octagon")
        val answer = names[sides]!!
        val options = (names.values.shuffled(random).filter { it != answer }.take(3) + answer).shuffled()
        return Problem(
            question = "A polygon with $sides sides is called ___.",
            correctAnswer = answer,
            options = options
        )
    }
    fun generate(): Problem {
        var gradeLevel = when(grade) {
            "K" -> 0
            else -> grade.toIntOrNull() ?: 1
        }
        
        if (isAdjusted) {
            gradeLevel = (gradeLevel - 2).coerceAtLeast(0)
        }
        
        val pool = when {
            gradeLevel <= 2 -> k2Geometry
            gradeLevel <= 4 -> elementaryGeometry
            gradeLevel <= 5 -> middleGeometry
            else -> highGeometry
        }

        // Procedural variety templates (35%) for grades 1-5.
        if (gradeLevel in 1..5 && random.nextFloat() < 0.35f) {
            return if (random.nextBoolean()) randomPerimeterProblem() else randomPolygonNameProblem()
        }

        val eligibleQuestions = pool.filter { (questionCounts[it.first] ?: 0) < 2 }
        val selection = if (eligibleQuestions.isNotEmpty()) {
            eligibleQuestions.random(random)
        } else {
            pool.random(random)
        }
        
        questionCounts[selection.first] = (questionCounts[selection.first] ?: 0) + 1
        val question = selection.first
        val answer = selection.second

        val options = mutableSetOf(answer)
        val allPoolAnswers = (k2Geometry + elementaryGeometry + middleGeometry + highGeometry).map { it.second }
        
        while (options.size < 4) {
            val distractor = allPoolAnswers.random(random)
            if (distractor != answer) options.add(distractor)
        }

        return Problem(
            question = question,
            correctAnswer = answer,
            options = options.toList().shuffled()
        )
    }
}
