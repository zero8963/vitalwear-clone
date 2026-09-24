package com.example.vitalwearclonev1.game

import kotlin.random.Random

class ScienceProblemGenerator(private val grade: String, private val floor: Int, private val isAdjusted: Boolean = false) {
    private val random = Random(System.currentTimeMillis())
    private val questionCounts = mutableMapOf<String, Int>()

    fun generateLesson(seenTitles: List<String> = emptyList()): Lesson {
        val gradeLevel = when(grade) {
            "K" -> 0
            else -> grade.toIntOrNull() ?: 1
        }
        
        val lessons = when {
            gradeLevel <= 2 -> listOf(
                "The Water Cycle",
                "Day and Night",
                "The Five Senses",
                "Magnets",
                "The Moon",
                "Rainbows",
                "The Four Seasons",
                "Push and Pull",
                "The Sun",
                "Sink or Float"
            )
            gradeLevel <= 5 -> listOf(
                "States of Matter",
                "Our Solar System",
                "Simple Machines",
                "Sound Waves",
                "Electricity",
                "Gravity",
                "Friction",
                "Ecosystems",
                "Mixtures and Solutions",
                "The Rock Cycle"
            )
            else -> listOf(
                "Newton's Laws of Motion",
                "Light Science",
                "The Atmosphere",
                "The Periodic Table",
                "Thermodynamics",
                "Plate Tectonics",
                "Chemical Reactions",
                "Kinetic and Potential Energy",
                "The Carbon Cycle",
                "Acids and Bases"
            )
        }

        val unseenLessons = lessons.filter { !seenTitles.contains(it) }
        // Paced learning: work through new skills in a stable, gentle order instead of jumping randomly.
        val selectedTitle = if (unseenLessons.isNotEmpty()) {
            if (isAdjusted) unseenLessons.first() else unseenLessons.random(random)
        } else lessons.random(random)

        return when (selectedTitle) {
            // GRADE K-2
            "The Water Cycle" -> Lesson(
                title = "The Water Cycle",
                explanation = "Water on Earth is always moving in a big circle!",
                steps = listOf(
                    "1. Evaporation: Sun heats up water and it turns into vapor.",
                    "2. Condensation: Vapor cools down and makes clouds.",
                    "3. Precipitation: Water falls back down as rain or snow."
                ),
                example = "When you see clouds forming, that's condensation in action!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What is it called when water falls as rain?",
                practiceAnswer = "Precipitation",
                practiceOptions = listOf("Precipitation", "Evaporation", "Condensation", "Rainy").shuffled(),
                solveSteps = listOf("Rain, snow, and hail are all forms of Precipitation.")
            )
            "Day and Night" -> Lesson(
                title = "Day and Night",
                explanation = "Earth is like a spinning top. It spins once every 24 hours.",
                steps = listOf(
                    "1. One side of Earth faces the Sun (Day).",
                    "2. The other side faces away from the Sun (Night).",
                    "3. This spinning is called 'rotation'."
                ),
                example = "When it's breakfast time for you, it's bedtime on the other side of the world!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "How many hours are in one full day?",
                practiceAnswer = "24",
                practiceOptions = listOf("24", "12", "48", "7").shuffled(),
                solveSteps = listOf("Earth takes 24 hours to make one full spin.")
            )
            "The Five Senses" -> Lesson(
                title = "The Five Senses",
                explanation = "Our bodies have five special ways to learn about the world.",
                steps = listOf(
                    "1. Sight (Eyes), Hearing (Ears).",
                    "2. Smell (Nose), Taste (Tongue).",
                    "3. Touch (Skin/Hands)."
                ),
                example = "You use your sense of taste to know an orange is sweet.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Which sense do you use to hear a bell?",
                practiceAnswer = "Hearing",
                practiceOptions = listOf("Hearing", "Sight", "Touch", "Smell").shuffled(),
                solveSteps = listOf("Ears are for hearing sounds.")
            )
            "Magnets" -> Lesson(
                title = "Magnets",
                explanation = "Magnets are objects that can pull some metals toward them.",
                steps = listOf(
                    "1. Every magnet has a North and South pole.",
                    "2. Opposite poles attract (pull together).",
                    "3. Same poles repel (push apart)."
                ),
                example = "A magnet can stick to your refrigerator door.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What do two North poles do?",
                practiceAnswer = "Repel",
                practiceOptions = listOf("Repel", "Attract", "Stick", "Spin").shuffled(),
                solveSteps = listOf("Like poles (N and N) push each other away. That's called repelling.")
            )
            "The Moon" -> Lesson(
                title = "The Moon",
                explanation = "The Moon is Earth's natural satellite. It doesn't make its own light!",
                steps = listOf(
                    "1. The Moon orbits (circles) the Earth.",
                    "2. It reflects light from the Sun.",
                    "3. It changes shape in the sky (Phases)."
                ),
                example = "A full moon looks like a bright circle.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Where does the Moon get its light?",
                practiceAnswer = "The Sun",
                practiceOptions = listOf("The Sun", "Flashlights", "Electricity", "Stars").shuffled(),
                solveSteps = listOf("The Moon acts like a mirror for the Sun's light.")
            )
            "Rainbows" -> Lesson(
                title = "Rainbows",
                explanation = "Rainbows appear when sunlight shines through raindrops.",
                steps = listOf(
                    "1. Light enters a raindrop.",
                    "2. The light bends and splits into colors.",
                    "3. The colors are: Red, Orange, Yellow, Green, Blue, Indigo, Violet."
                ),
                example = "You often see rainbows right after it rains.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What is the first color of the rainbow?",
                practiceAnswer = "Red",
                practiceOptions = listOf("Red", "Blue", "Green", "Pink").shuffled(),
                solveSteps = listOf("The colors always follow the same order, starting with Red.")
            )

            // GRADE 3-5
            "States of Matter" -> Lesson(
                title = "States of Matter",
                explanation = "Everything around you is made of matter, and it comes in three main forms.",
                steps = listOf(
                    "1. Solid: Has a fixed shape (like a rock).",
                    "2. Liquid: Flows and takes the shape of its container (like water).",
                    "3. Gas: Fills up all the space it can (like air)."
                ),
                example = "Ice is a solid, water is a liquid, and steam is a gas!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Is air a solid, liquid, or gas?",
                practiceAnswer = "gas",
                practiceOptions = listOf("gas", "solid", "liquid").shuffled(),
                solveSteps = listOf("Air fills up the space and doesn't have a fixed shape. It's a gas.")
            )
            "Our Solar System" -> Lesson(
                title = "Our Solar System",
                explanation = "Our Sun has 8 main planets orbiting it.",
                steps = listOf(
                    "1. Mercury, Venus, Earth, Mars (Rock planets).",
                    "2. Jupiter, Saturn (Gas giants).",
                    "3. Uranus, Neptune (Ice giants)."
                ),
                example = "Earth is the third planet from the Sun.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Which planet is known as the 'Red Planet'?",
                practiceAnswer = "Mars",
                practiceOptions = listOf("Mars", "Venus", "Jupiter", "Saturn").shuffled(),
                solveSteps = listOf("Mars has red soil, which makes it look red from Earth.")
            )
            "Simple Machines" -> Lesson(
                title = "Simple Machines",
                explanation = "Simple machines help us do work more easily by using less force.",
                steps = listOf(
                    "1. Lever (like a seesaw), Pulley (used for flags).",
                    "2. Inclined Plane (a ramp), Screw.",
                    "3. Wheel and Axle, Wedge (like an axe head)."
                ),
                example = "A ramp helps you push a heavy box into a truck.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "A seesaw is an example of which simple machine?",
                practiceAnswer = "Lever",
                practiceOptions = listOf("Lever", "Pulley", "Wedge", "Screw").shuffled(),
                solveSteps = listOf("A lever has a bar that pivots on a point.")
            )
            "Sound Waves" -> Lesson(
                title = "Sound Waves",
                explanation = "Sound is made of vibrations that travel through the air in waves.",
                steps = listOf(
                    "1. Something vibrates (like a guitar string).",
                    "2. The vibration moves the air particles.",
                    "3. The waves reach your ears."
                ),
                example = "When you clap, you make the air vibrate.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What travels through the air to let us hear?",
                practiceAnswer = "Vibrations",
                practiceOptions = listOf("Vibrations", "Electricity", "Light", "Wind").shuffled(),
                solveSteps = listOf("Sound is all about vibrations moving in waves.")
            )
            "Electricity" -> Lesson(
                title = "Electricity",
                explanation = "Electricity is the flow of tiny particles called electrons through a wire.",
                steps = listOf(
                    "1. Electrons move in a loop called a Circuit.",
                    "2. A switch can open or close the loop.",
                    "3. Conductors (like copper) let electricity flow easily."
                ),
                example = "A battery provides the energy to move the electrons.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What do we call a loop that electricity flows through?",
                practiceAnswer = "Circuit",
                practiceOptions = listOf("Circuit", "Circle", "Wire", "Bridge").shuffled(),
                solveSteps = listOf("Just like a race track, electricity needs a complete loop called a Circuit.")
            )
            "Gravity" -> Lesson(
                title = "Gravity",
                explanation = "Gravity is an invisible pull that brings objects toward each other.",
                steps = listOf(
                    "1. Bigger objects (like Earth) have stronger gravity.",
                    "2. Gravity keeps our feet on the ground.",
                    "3. It also keeps the Moon orbiting the Earth."
                ),
                example = "If you drop a ball, gravity pulls it down.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Which has stronger gravity: The Earth or an Apple?",
                practiceAnswer = "The Earth",
                practiceOptions = listOf("The Earth", "An Apple", "Both are same").shuffled(),
                solveSteps = listOf("Massive objects have more gravity. Earth is much bigger than an apple!")
            )

            // MIDDLE SCHOOL+
            "Newton's Laws of Motion" -> Lesson(
                title = "Newton's Laws of Motion",
                explanation = "Isaac Newton discovered how things move in the universe.",
                steps = listOf(
                    "1. Inertia: Objects keep doing what they're doing unless pushed.",
                    "2. F=ma: Force equals mass times acceleration.",
                    "3. Action/Reaction: For every action, there is an equal and opposite reaction."
                ),
                example = "When you push a skateboard, it moves because of your applied force!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What is the force law formula?",
                practiceAnswer = "F=ma",
                practiceOptions = listOf("F=ma", "F=m+a", "F=v/t", "E=mc²").shuffled(),
                solveSteps = listOf("Force (F) = Mass (m) x Acceleration (a)")
            )
            "Light Science" -> Lesson(
                title = "Light Science",
                explanation = "Light travels in straight lines and can bounce or bend.",
                steps = listOf(
                    "1. Reflection: Light bouncing off a surface.",
                    "2. Refraction: Light bending when it enters water or glass.",
                    "3. Absorption: When a surface takes in the light energy."
                ),
                example = "A prism can split light into a rainbow.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What is it called when light bounces off a mirror?",
                practiceAnswer = "Reflection",
                practiceOptions = listOf("Reflection", "Refraction", "Absorption", "Diffraction").shuffled(),
                solveSteps = listOf("Mirrors reflect light, like a ball bouncing off a wall.")
            )
            "The Atmosphere" -> Lesson(
                title = "The Atmosphere",
                explanation = "Earth is surrounded by layers of gases that protect us.",
                steps = listOf(
                    "1. Troposphere: Where weather happens.",
                    "2. Stratosphere: Contains the Ozone layer.",
                    "3. Mesosphere, Thermosphere, Exosphere."
                ),
                example = "Planes usually fly in the Troposphere or the lower Stratosphere.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Which layer of the atmosphere contains weather?",
                practiceAnswer = "Troposphere",
                practiceOptions = listOf("Troposphere", "Stratosphere", "Mesosphere", "Exosphere").shuffled(),
                solveSteps = listOf("The Troposphere is the lowest layer where we live and breathe.")
            )
            "The Periodic Table" -> Lesson(
                title = "The Periodic Table",
                explanation = "This table organizes all the chemical elements in the universe.",
                steps = listOf(
                    "1. Elements are sorted by their Atomic Number.",
                    "2. Columns are called Groups.",
                    "3. Rows are called Periods."
                ),
                example = "Oxygen (O) and Gold (Au) are elements on the table.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What is the symbol for Oxygen?",
                practiceAnswer = "O",
                practiceOptions = listOf("O", "Ox", "H", "C").shuffled(),
                solveSteps = listOf("Oxygen is one of the most common elements, represented by 'O'.")
            )
            "Thermodynamics" -> Lesson(
                title = "Thermodynamics",
                explanation = "Heat is energy that moves from hotter objects to colder ones.",
                steps = listOf(
                    "1. Conduction: Heat moving through touch (like a spoon in tea).",
                    "2. Convection: Heat moving through liquids or gases (like boiling water).",
                    "3. Radiation: Heat moving through space (like heat from the Sun)."
                ),
                example = "A metal pan gets hot on a stove because of conduction.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What is heat transfer through touch called?",
                practiceAnswer = "Conduction",
                practiceOptions = listOf("Conduction", "Convection", "Radiation", "Insulation").shuffled(),
                solveSteps = listOf("Conduction requires direct physical contact between objects.")
            )
            // GRADE K-2 (continued)
            "The Four Seasons" -> Lesson(
                title = "The Four Seasons",
                explanation = "Earth's tilt gives us four seasons: spring, summer, fall, and winter - each with its own weather!",
                steps = listOf(
                    "1. Spring: flowers bloom and rain falls.",
                    "2. Summer: hot sun and long days.",
                    "3. Fall: leaves change color and drop. Winter: cold and snow."
                ),
                example = "Bears sleep through winter and wake up in spring!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Which season comes right after winter?",
                practiceAnswer = "Spring",
                practiceOptions = listOf("Spring", "Summer", "Fall", "Winter").shuffled(),
                solveSteps = listOf("The order is: Spring, Summer, Fall, Winter", "After Winter comes Spring")
            )
            "Push and Pull" -> Lesson(
                title = "Push and Pull",
                explanation = "A push moves something away from you. A pull brings something toward you. Both are forces!",
                steps = listOf(
                    "1. Push = away from you (kicking a ball).",
                    "2. Pull = toward you (opening a door).",
                    "3. Bigger pushes and pulls make things move faster!"
                ),
                example = "A swing needs pushes to go higher and higher!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Pulling a wagon toward you is a ___.",
                practiceAnswer = "pull",
                practiceOptions = listOf("pull", "push", "twist", "shake").shuffled(),
                solveSteps = listOf("The wagon moves TOWARD you", "Toward you = pull")
            )
            "The Sun" -> Lesson(
                title = "The Sun",
                explanation = "The Sun is a star - a giant ball of hot, glowing gas. It gives Earth light and heat!",
                steps = listOf(
                    "1. The Sun is a star, much bigger than Earth.",
                    "2. Its light takes 8 minutes to reach us.",
                    "3. Plants use sunlight to make food."
                ),
                example = "You could fit about 1.3 million Earths inside the Sun!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "The Sun is a ___.",
                practiceAnswer = "star",
                practiceOptions = listOf("star", "planet", "moon", "comet").shuffled(),
                solveSteps = listOf("It makes its own light and heat", "Objects that make their own light are stars")
            )
            "Sink or Float" -> Lesson(
                title = "Sink or Float",
                explanation = "Heavy, dense things sink. Light things full of air float! Shape matters too - a boat holds air inside.",
                steps = listOf(
                    "1. Dense and heavy usually sinks (a rock).",
                    "2. Light or air-filled usually floats (a beach ball).",
                    "3. Test it: guess first, then drop it in!"
                ),
                example = "A huge steel ship floats because its shape holds lots of air!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Which of these would FLOAT in water?",
                practiceAnswer = "A beach ball",
                practiceOptions = listOf("A beach ball", "A rock", "A coin", "A marble").shuffled(),
                solveSteps = listOf("Rocks, coins, and marbles are dense", "The beach ball is full of air - it floats!")
            )

            // GRADE 3-5 (continued)
            "Friction" -> Lesson(
                title = "Friction",
                explanation = "Friction is the grip between two surfaces rubbing together. It slows things down - and makes heat!",
                steps = listOf(
                    "1. Rub your hands fast: that warmth is friction!",
                    "2. Rough surfaces have more friction than smooth ones.",
                    "3. We use friction every day: shoes grip the floor because of it."
                ),
                example = "Ice is slippery because it has very little friction!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Rubbing your hands together makes them warm because of ___.",
                practiceAnswer = "friction",
                practiceOptions = listOf("friction", "gravity", "magnetism", "electricity").shuffled(),
                solveSteps = listOf("Rubbing surfaces create grip and heat", "That grip-force is called friction")
            )
            "Ecosystems" -> Lesson(
                title = "Ecosystems",
                explanation = "In an ecosystem, plants are producers (they make food), and animals are consumers (they eat food)!",
                steps = listOf(
                    "1. Producers: plants make food from sunlight.",
                    "2. Consumers: animals eat plants or other animals.",
                    "3. Decomposers: fungi and worms recycle dead things."
                ),
                example = "Grass -> rabbit -> fox: a tiny food chain!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "In an ecosystem, plants are called ___ because they make their own food.",
                practiceAnswer = "producers",
                practiceOptions = listOf("producers", "consumers", "predators", "prey").shuffled(),
                solveSteps = listOf("Plants PRODUCE food using sunlight", "So they are called producers")
            )
            "Mixtures and Solutions" -> Lesson(
                title = "Mixtures and Solutions",
                explanation = "In a mixture you can still see the parts (trail mix). In a solution, one thing dissolves and disappears (sugar in tea)!",
                steps = listOf(
                    "1. Mixture: parts stay separate and visible.",
                    "2. Solution: one substance dissolves into another.",
                    "3. You can often separate mixtures, but solutions are trickier!"
                ),
                example = "Salt water is a solution - the salt seems to vanish!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "When sugar disappears into water, it forms a ___.",
                practiceAnswer = "solution",
                practiceOptions = listOf("solution", "mixture", "solid", "gas").shuffled(),
                solveSteps = listOf("The sugar dissolves and can't be seen", "A dissolved mixture is called a solution")
            )
            "The Rock Cycle" -> Lesson(
                title = "The Rock Cycle",
                explanation = "Rocks never rest! Wind and water break them down (erosion), and heat and pressure build new ones.",
                steps = listOf(
                    "1. Weathering cracks rocks apart.",
                    "2. Erosion carries the pieces away.",
                    "3. Heat and pressure deep underground form new rock."
                ),
                example = "The Grand Canyon was carved by erosion over millions of years!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Rocks are broken down and carried away by wind and water in a process called ___.",
                practiceAnswer = "erosion",
                practiceOptions = listOf("erosion", "eruption", "evaporation", "evolution").shuffled(),
                solveSteps = listOf("Wind and water MOVE rock pieces", "That carrying-away process is erosion")
            )

            // HIGH SCHOOL+ (continued)
            "Chemical Reactions" -> Lesson(
                title = "Chemical Reactions",
                explanation = "A chemical reaction makes brand-new substances! Signs: fizzing, color change, heat, or light.",
                steps = listOf(
                    "1. Look for fizz, bubbles, or a color change.",
                    "2. New substances form - you can't easily undo it.",
                    "3. Baking soda + vinegar = fizzing carbon dioxide!"
                ),
                example = "Rusting, burning, and cooking are all chemical reactions!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Baking soda and vinegar fizzing together is a ___.",
                practiceAnswer = "chemical reaction",
                practiceOptions = listOf("chemical reaction", "physical change", "mixture", "solution").shuffled(),
                solveSteps = listOf("Fizzing means new gas is forming", "New substances = chemical reaction")
            )
            "Kinetic and Potential Energy" -> Lesson(
                title = "Kinetic and Potential Energy",
                explanation = "Potential energy is stored (a coaster at the top). Kinetic energy is motion (the coaster racing down)!",
                steps = listOf(
                    "1. High up and still = lots of potential energy.",
                    "2. Moving fast = lots of kinetic energy.",
                    "3. They trade back and forth: up the hill, potential grows!"
                ),
                example = "A stretched rubber band holds potential energy until you let go!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "A roller coaster paused at the very top of a hill has mostly ___ energy.",
                practiceAnswer = "potential",
                practiceOptions = listOf("potential", "kinetic", "thermal", "solar").shuffled(),
                solveSteps = listOf("It is high up but not moving", "Stored energy = potential energy")
            )
            "The Carbon Cycle" -> Lesson(
                title = "The Carbon Cycle",
                explanation = "Carbon travels in a loop: plants breathe in carbon dioxide, animals breathe out, and decomposers return it to the air!",
                steps = listOf(
                    "1. Plants take in carbon dioxide to grow.",
                    "2. Animals eat plants and breathe out carbon dioxide.",
                    "3. Burning fossil fuels adds extra carbon to the air."
                ),
                example = "The carbon in you was once in the air, plants, and dinosaurs!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Plants take in ___ from the air to make food.",
                practiceAnswer = "carbon dioxide",
                practiceOptions = listOf("carbon dioxide", "oxygen", "nitrogen", "hydrogen").shuffled(),
                solveSteps = listOf("Plants need carbon to build leaves and stems", "They absorb it as carbon dioxide")
            )
            "Acids and Bases" -> Lesson(
                title = "Acids and Bases",
                explanation = "Acids taste sour (lemon juice). Bases taste bitter and feel slippery (soap). They can cancel each other out!",
                steps = listOf(
                    "1. Sour taste = acid. Think lemons and vinegar.",
                    "2. Bitter, slippery = base. Think soap and baking soda.",
                    "3. Scientists measure strength with the pH scale."
                ),
                example = "Your stomach uses acid to digest food!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Lemon juice tastes sour because it is an ___.",
                practiceAnswer = "acid",
                practiceOptions = listOf("acid", "base", "salt", "metal").shuffled(),
                solveSteps = listOf("Sour taste is the clue", "Sour substances are acids")
            )
            else -> Lesson(
                title = "Plate Tectonics",
                explanation = "The Earth's outer shell is divided into several plates that glide over the mantle.",
                steps = listOf(
                    "1. Plates can move apart (Divergent).",
                    "2. Plates can crash together (Convergent).",
                    "3. Plates can slide past each other (Transform)."
                ),
                example = "Mountains are formed when plates crash together.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What happens when two plates crash together?",
                practiceAnswer = "Mountains form",
                practiceOptions = listOf("Mountains form", "Plates disappear", "Nothing happens", "The sea dries up").shuffled(),
                solveSteps = listOf("Convergent boundaries push the Earth's crust upward to form mountains.")
            )
        }
    }

    private val k2Science = listOf(
        Pair("What do plants need to grow?", "Water and Sunlight"),
        Pair("Which animal lives in the ocean?", "Shark"),
        Pair("What is the center of our solar system?", "Sun"),
        Pair("Which season is the coldest?", "Winter"),
        Pair("We use our ___ to see.", "eyes"),
        Pair("A ___ grows from a seed.", "plant"),
        Pair("Which animal says \"moo\"?", "Cow"),
        Pair("The ___ comes out at night.", "moon"),
        Pair("Rain falls from the ___.", "clouds"),
        Pair("We breathe ___.", "air"),
        Pair("Which body part do you use to hear?", "Ears"),
        Pair("A ___ has eight legs.", "spider"),
        Pair("What color are most leaves?", "Green"),
        Pair("Ice is very ___.", "cold"),
        Pair("The ___ is hot and gives us light.", "sun"),
            Pair("Which planet do we live on?", "Earth"),
            Pair("What do we call water falling from clouds?", "Rain"),
    )

    private val elementaryScience = listOf(
        Pair("What state of matter is water?", "Liquid"),
        Pair("Which planet is known as the Red Planet?", "Mars"),
        Pair("What do bees collect from flowers?", "Nectar"),
        Pair("The process of a caterpillar turning into a butterfly is ___.", "Metamorphosis"),
        Pair("What is the boiling point of water (Celsius)?", "100"),
        Pair("A ___ is an animal with a backbone.", "Vertebrate"),
        Pair("The force that pulls things to the ground is ___.", "Gravity"),
        Pair("Evaporation is when water turns into ___.", "Gas"),
        Pair("The largest planet in our solar system is ___.", "Jupiter"),
        Pair("An ___ is a scientist who studies space.", "Astronomer"),
        Pair("Sound travels in ___.", "Waves"),
        Pair("A magnet attracts things made of ___.", "Iron"),
        Pair("The ___ is the outer layer of the Earth.", "Crust"),
        Pair("A ___ is an animal that eats only plants.", "Herbivore"),
        Pair("The three states of matter are solid, liquid, and ___.", "Gas"),
            Pair("A lever, a pulley, and a ramp are all ___ machines.", "Simple"),
            Pair("What gas do humans need to breathe in?", "Oxygen"),
    )

    private val middleScience = listOf(
        Pair("What is the powerhouse of the cell?", "Mitochondria"),
        Pair("Which element has the chemical symbol 'O'?", "Oxygen"),
        Pair("What force pulls objects toward the Earth?", "Gravity"),
        Pair("Rocks formed from cooling lava are called ___.", "Igneous"),
        Pair("What is the closest planet to the Sun?", "Mercury"),
        Pair("The ___ system includes the heart and blood vessels.", "Circulatory"),
        Pair("A ___ is a group of similar cells working together.", "Tissue"),
        Pair("Kinetic energy is the energy of ___.", "Motion"),
        Pair("The ___ Scale measures the acidity of a liquid.", "pH"),
        Pair("Photosynthesis takes place in the ___ of a plant cell.", "Chloroplasts"),
        Pair("The ___ is the basic unit of life.", "Cell"),
        Pair("Newton's First Law is also known as the Law of ___.", "Inertia"),
        Pair("A ___ is a substance that cannot be broken down further.", "Element"),
        Pair("The Earth's path around the sun is called an ___.", "Orbit"),
        Pair("Natural ___ is the process behind evolution.", "Selection"),
            Pair("The center of an atom is called the ___.", "Nucleus"),
    )

    private val highScience = listOf(
        Pair("What is the speed of light (approx)?", "300,000 km/s"),
        Pair("Which law states that for every action there is an equal and opposite reaction?", "Newton's Third Law"),
        Pair("The atomic number of an element is the number of ___.", "Protons"),
        Pair("What is the most abundant gas in Earth's atmosphere?", "Nitrogen"),
        Pair("DNA stands for ___ acid.", "Deoxyribonucleic"),
        Pair("The half-life of a substance is used in ___ dating.", "Radioactive"),
        Pair("Entropy is a measure of ___ in a system.", "Disorder"),
        Pair("A ___ bond involves the sharing of electrons.", "Covalent"),
        Pair("The Doppler Effect explains changes in ___.", "Frequency"),
        Pair("Mitosis results in two identical ___ cells.", "Daughter"),
        Pair("The Second Law of Thermodynamics relates to ___.", "Entropy"),
        Pair("An ___ is an atom with a different number of neutrons.", "Isotope"),
        Pair("Subduction occurs at ___ plate boundaries.", "Convergent"),
        Pair("The Higgs Boson is often called the ___ particle.", "God"),
        Pair("Respiration occurs in the ___ to produce ATP.", "Mitochondria"),
            Pair("What tiny particle carries a negative charge?", "Electron"),
    )

    fun generate(): Problem {
        var gradeLevel = when(grade) {
            "K" -> 0
            else -> grade.toIntOrNull() ?: 1
        }
        
        if (isAdjusted) {
            gradeLevel = (gradeLevel - 2).coerceAtLeast(0)
        }
        
        val pool = when {
            gradeLevel <= 2 -> k2Science
            gradeLevel <= 4 -> elementaryScience
            gradeLevel <= 5 -> middleScience
            else -> highScience
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
        val allPoolAnswers = (k2Science + elementaryScience + middleScience + highScience).map { it.second }
        
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
