package com.example.vitalwearclonev1.game

import kotlin.random.Random

class LifeScienceProblemGenerator(private val grade: String, private val floor: Int, private val isAdjusted: Boolean = false) {
    private val random = Random(System.currentTimeMillis())
    private val questionCounts = mutableMapOf<String, Int>()

    fun generateLesson(seenTitles: List<String> = emptyList()): Lesson {
        val gradeLevel = when(grade) {
            "K" -> 0
            else -> grade.toIntOrNull() ?: 1
        }
        
        // Titles below are in the same order as the lessonIdx branches per band.
        val bandTitles = when {
            gradeLevel <= 2 -> listOf("Plant Parts", "Animal Habitats", "Living vs. Non-Living", "Mammals", "What Plants Need", "Animal Babies", "Hibernation", "Predators and Prey")
            gradeLevel <= 5 -> listOf("Food Chains", "Life Cycle of a Butterfly", "Internal Organs", "Pollination", "Camouflage", "Migration", "Herbivores and Carnivores", "Fossils")
            else -> listOf("The Human Cell", "DNA and Genes", "Photosynthesis Deep Dive", "Natural Selection", "Mitosis", "Biomes of the World", "The Nervous System", "Evidence for Evolution")
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
                    0 -> Lesson(
                        title = "Plant Parts",
                        explanation = "Plants have different parts that help them survive.",
                        steps = listOf(
                            "1. Roots: Drink water from the ground.",
                            "2. Stem: Carries water and helps the plant stand tall.",
                            "3. Leaves: Catch sunlight to make food.",
                            "4. Flowers: Make seeds for new plants."
                        ),
                        example = "When you eat a carrot, you are eating the root of a plant!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Which part of the plant makes food from sunlight?",
                        practiceAnswer = "Leaves",
                        practiceOptions = listOf("Leaves", "Roots", "Stem", "Petals").shuffled(),
                        solveSteps = listOf("Leaves have green chlorophyll to catch sunlight.")
                    )
                    1 -> Lesson(
                        title = "Animal Habitats",
                        explanation = "A habitat is a place where an animal lives and finds food.",
                        steps = listOf(
                            "1. Ocean: Saltwater home for fish and whales.",
                            "2. Forest: Trees provide homes for birds and squirrels.",
                            "3. Desert: Hot and dry home for camels and lizards."
                        ),
                        example = "A polar bear's habitat is the cold Arctic ice.",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Which habitat is very hot and dry?",
                        practiceAnswer = "Desert",
                        practiceOptions = listOf("Desert", "Forest", "Ocean", "River").shuffled(),
                        solveSteps = listOf("Deserts get very little rain and can be extremely hot.")
                    )
                    2 -> Lesson(
                        title = "Living vs. Non-Living",
                        explanation = "Living things grow, breathe, and need energy. Non-living things do not.",
                        steps = listOf(
                            "1. Can it grow?",
                            "2. Does it need food or water?",
                            "3. Can it have babies or make seeds?"
                        ),
                        example = "A puppy is living. A rock is non-living.",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Is a tree living or non-living?",
                        practiceAnswer = "living",
                        practiceOptions = listOf("living", "non-living").shuffled(),
                        solveSteps = listOf("Trees grow from seeds and need water to live.")
                    )
                    3 -> Lesson(
                        title = "Mammals",
                        explanation = "Mammals are a group of animals that have hair or fur and feed their babies milk.",
                        steps = listOf(
                            "1. They are warm-blooded.",
                            "2. They usually have hair or fur.",
                            "3. They breathe air with lungs."
                        ),
                        example = "Dogs, cats, and even humans are mammals!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Are humans mammals?",
                        practiceAnswer = "Yes",
                        practiceOptions = listOf("Yes", "No").shuffled(),
                        solveSteps = listOf("Humans have hair and breathe air, so we are mammals.")
                    )
                    4 -> Lesson(
                        title = "What Plants Need",
                        explanation = "Plants need four things to grow big and strong: sunlight, water, air, and soil!",
                        steps = listOf(
                            "1. Sunlight: leaves catch it to make food.",
                            "2. Water: roots drink it from the soil.",
                            "3. Air and soil give the rest."
                        ),
                        example = "A plant in a dark closet will grow tall and pale reaching for light!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Which of these does a plant NOT need to grow?",
                        practiceAnswer = "Candy",
                        practiceOptions = listOf("Candy", "Sunlight", "Water", "Soil").shuffled(),
                        solveSteps = listOf("Plants need sunlight, water, air, and soil", "Candy is not on the list!")
                    )
                    5 -> Lesson(
                        title = "Animal Babies",
                        explanation = "Baby animals have special names! A baby dog is a puppy, a baby cat is a kitten, a baby bear is a cub.",
                        steps = listOf(
                            "1. Dog -> puppy. Cat -> kitten.",
                            "2. Bear -> cub. Duck -> duckling.",
                            "3. Frog babies are tadpoles - they change as they grow!"
                        ),
                        example = "A baby kangaroo is called a joey and rides in its mom's pouch!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "What is a baby bear called?",
                        practiceAnswer = "Cub",
                        practiceOptions = listOf("Cub", "Puppy", "Kitten", "Calf").shuffled(),
                        solveSteps = listOf("Puppy = dog, kitten = cat", "Bear babies are cubs!")
                    )
                    6 -> Lesson(
                        title = "Hibernation",
                        explanation = "Some animals sleep all winter to save energy! This deep sleep is called hibernation.",
                        steps = listOf(
                            "1. In fall, animals eat lots to store fat.",
                            "2. They find a cozy den and fall into deep sleep.",
                            "3. In spring, they wake up hungry!"
                        ),
                        example = "A hibernating bear's heartbeat slows way down!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "What is it called when animals sleep through winter?",
                        practiceAnswer = "Hibernation",
                        practiceOptions = listOf("Hibernation", "Migration", "Camouflage", "Pollination").shuffled(),
                        solveSteps = listOf("Sleeping through winter saves energy", "This is called hibernation")
                    )
                    else -> Lesson(
                        title = "Predators and Prey",
                        explanation = "Predators hunt other animals. Prey are the animals being hunted. A rabbit is prey; a fox is a predator!",
                        steps = listOf(
                            "1. Predator: the hunter (lion, hawk, shark).",
                            "2. Prey: the hunted (zebra, mouse, fish).",
                            "3. Many animals are both - a frog eats flies but is hunted by snakes!"
                        ),
                        example = "Owls are predators with super hearing for hunting mice!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "A fox hunting a rabbit: which animal is the PREDATOR?",
                        practiceAnswer = "The fox",
                        practiceOptions = listOf("The fox", "The rabbit", "The grass", "The sky").shuffled(),
                        solveSteps = listOf("The hunter is the predator", "The fox hunts, so the fox is the predator")
                    )
                }
            }
            gradeLevel <= 5 -> {
                when (lessonIdx) {
                    0 -> Lesson(
                        title = "Food Chains",
                        explanation = "A food chain shows how energy moves from one living thing to another.",
                        steps = listOf(
                            "1. Producer: Plants make energy from the sun.",
                            "2. Consumer: Animals eat plants or other animals.",
                            "3. Decomposer: Fungi and bacteria break down dead things."
                        ),
                        example = "Sun -> Grass -> Grasshopper -> Bird.",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "In a food chain, what do we call plants?",
                        practiceAnswer = "Producers",
                        practiceOptions = listOf("Producers", "Consumers", "Decomposers", "Hunters").shuffled(),
                        solveSteps = listOf("Plants produce their own food using sunlight.")
                    )
                    1 -> Lesson(
                        title = "Life Cycle of a Butterfly",
                        explanation = "Butterflies go through four stages as they grow.",
                        steps = listOf(
                            "1. Egg: A tiny egg on a leaf.",
                            "2. Larva: A caterpillar eating leaves.",
                            "3. Pupa: Inside a chrysalis (hard shell).",
                            "4. Adult: A beautiful butterfly."
                        ),
                        example = "The caterpillar stage is called the Larva.",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "What is the second stage of a butterfly's life?",
                        practiceAnswer = "Larva",
                        practiceOptions = listOf("Larva", "Egg", "Pupa", "Adult").shuffled(),
                        solveSteps = listOf("Egg -> Larva (Caterpillar) -> Pupa -> Adult")
                    )
                    2 -> Lesson(
                        title = "Internal Organs",
                        explanation = "Our bodies have special parts inside that do important jobs.",
                        steps = listOf(
                            "1. Heart: Pumps blood.",
                            "2. Lungs: Help you breathe.",
                            "3. Stomach: Digests food.",
                            "4. Brain: Controls everything."
                        ),
                        example = "Your heart beats faster when you run.",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Which organ helps you breathe air?",
                        practiceAnswer = "Lungs",
                        practiceOptions = listOf("Lungs", "Heart", "Brain", "Liver").shuffled(),
                        solveSteps = listOf("Lungs take in oxygen from the air.")
                    )
                    3 -> Lesson(
                        title = "Pollination",
                        explanation = "Bees and butterflies help plants make seeds by moving pollen from one flower to another.",
                        steps = listOf(
                            "1. An insect visits a flower for nectar.",
                            "2. Pollen sticks to the insect's body.",
                            "3. The insect carries the pollen to another flower."
                        ),
                        example = "Bees are the most famous pollinators.",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "What do bees move between flowers?",
                        practiceAnswer = "Pollen",
                        practiceOptions = listOf("Pollen", "Honey", "Water", "Dirt").shuffled(),
                        solveSteps = listOf("Pollen is needed for flowers to create new seeds.")
                    )
                    4 -> Lesson(
                        title = "Camouflage",
                        explanation = "Camouflage is blending in! Animals hide from predators - or sneak up on prey - by matching their surroundings.",
                        steps = listOf(
                            "1. Color match: a green frog vanishes into leaves.",
                            "2. Pattern break: stripes confuse the eye.",
                            "3. Some animals even change color with the seasons!"
                        ),
                        example = "The arctic fox is brown in summer and white in winter!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Why does a stick insect look like a twig?",
                        practiceAnswer = "Camouflage",
                        practiceOptions = listOf("Camouflage", "Migration", "Hibernation", "Molting").shuffled(),
                        solveSteps = listOf("Looking like a twig hides it from birds", "Hiding by blending in is camouflage")
                    )
                    5 -> Lesson(
                        title = "Migration",
                        explanation = "Migration is a long seasonal journey. Birds, whales, and butterflies travel thousands of miles to find food!",
                        steps = listOf(
                            "1. Animals leave when food gets scarce.",
                            "2. They travel to warmer places with more food.",
                            "3. They return when seasons change back."
                        ),
                        example = "Arctic terns fly from pole to pole every year - 44,000 miles!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Birds flying south for the winter is an example of ___.",
                        practiceAnswer = "migration",
                        practiceOptions = listOf("migration", "hibernation", "camouflage", "pollination").shuffled(),
                        solveSteps = listOf("Seasonal long-distance travel", "That journey is called migration")
                    )
                    6 -> Lesson(
                        title = "Herbivores and Carnivores",
                        explanation = "Herbivores eat only plants (cows). Carnivores eat only meat (lions). Omnivores eat both (bears, humans)!",
                        steps = listOf(
                            "1. Herbi = plant: herbivores munch leaves and grass.",
                            "2. Carni = meat: carnivores hunt other animals.",
                            "3. Omni = all: omnivores eat plants AND meat."
                        ),
                        example = "A panda is a herbivore - it eats almost only bamboo!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "A lion eats only meat. It is a ___.",
                        practiceAnswer = "carnivore",
                        practiceOptions = listOf("carnivore", "herbivore", "omnivore", "producer").shuffled(),
                        solveSteps = listOf("Meat-eaters are carnivores", "Answer: carnivore")
                    )
                    else -> Lesson(
                        title = "Fossils",
                        explanation = "Fossils are the preserved remains of ancient life - bones, footprints, even poop! They tell us about dinosaurs.",
                        steps = listOf(
                            "1. An animal dies and gets buried quickly.",
                            "2. Minerals slowly replace the bones with rock.",
                            "3. Millions of years later, we dig them up!"
                        ),
                        example = "Amber can trap insects perfectly for 100 million years!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Scientists who study fossils are called ___.",
                        practiceAnswer = "paleontologists",
                        practiceOptions = listOf("paleontologists", "astronomers", "chemists", "doctors").shuffled(),
                        solveSteps = listOf("Fossil experts dig up and study ancient life", "They are called paleontologists")
                    )
                }
            }
            else -> {
                when (lessonIdx) {
                    0 -> Lesson(
                        title = "The Human Cell",
                        explanation = "Every living thing is made of tiny building blocks called cells.",
                        steps = listOf(
                            "1. Nucleus: The 'brain' that contains DNA.",
                            "2. Mitochondria: The 'powerhouse' that creates energy.",
                            "3. Cell Membrane: The 'outer skin' that protects the cell."
                        ),
                        example = "Your body is made of trillions of cells working together!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Which part of the cell is the 'brain'?",
                        practiceAnswer = "Nucleus",
                        practiceOptions = listOf("Nucleus", "Mitochondria", "Cell Wall", "Ribosome").shuffled(),
                        solveSteps = listOf("The Nucleus holds the genetic information and controls the cell.")
                    )
                    1 -> Lesson(
                        title = "DNA and Genes",
                        explanation = "DNA is the set of instructions for building you.",
                        steps = listOf(
                            "1. Genes: Short sections of DNA.",
                            "2. They determine things like eye color or height.",
                            "3. You inherit half from each parent."
                        ),
                        example = "You might have blue eyes because of the genes from your parents.",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "What is the acronym for Deoxyribonucleic Acid?",
                        practiceAnswer = "DNA",
                        practiceOptions = listOf("DNA", "RNA", "ATP", "ABC").shuffled(),
                        solveSteps = listOf("DNA is the master blueprint of life.")
                    )
                    2 -> Lesson(
                        title = "Photosynthesis Deep Dive",
                        explanation = "This is the process plants use to change light into food energy.",
                        steps = listOf(
                            "1. They take in Carbon Dioxide and Water.",
                            "2. Using Sunlight, they create Glucose (sugar).",
                            "3. They release Oxygen as a waste product."
                        ),
                        example = "Plants are the reason we have oxygen to breathe!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "What gas do plants release for us to breathe?",
                        practiceAnswer = "Oxygen",
                        practiceOptions = listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Helium").shuffled(),
                        solveSteps = listOf("Plants breathe in CO2 and release Oxygen.")
                    )
                    3 -> Lesson(
                        title = "Natural Selection",
                        explanation = "Charles Darwin's theory that individuals with helpful traits are more likely to survive and reproduce.",
                        steps = listOf(
                            "1. Variation: Individuals are different.",
                            "2. Selection: Helpful traits help survival.",
                            "3. Adaptation: Traits become more common over time."
                        ),
                        example = "Giraffes with longer necks could reach more food.",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Who is famous for the theory of Natural Selection?",
                        practiceAnswer = "Charles Darwin",
                        practiceOptions = listOf("Charles Darwin", "Isaac Newton", "Albert Einstein", "Thomas Edison").shuffled(),
                        solveSteps = listOf("Darwin studied finches and tortoises to develop this theory.")
                    )
                    4 -> Lesson(
                        title = "Mitosis",
                        explanation = "Mitosis is how cells divide to make two identical copies! Your body uses it to grow and heal cuts.",
                        steps = listOf(
                            "1. The cell copies its DNA.",
                            "2. The copies line up and pull apart.",
                            "3. The cell splits into two identical cells!"
                        ),
                        example = "You started as ONE cell - mitosis built all trillions of yours!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Cell division that makes two identical cells is called ___.",
                        practiceAnswer = "mitosis",
                        practiceOptions = listOf("mitosis", "photosynthesis", "digestion", "evaporation").shuffled(),
                        solveSteps = listOf("Identical copies come from cell division", "That process is mitosis")
                    )
                    5 -> Lesson(
                        title = "Biomes of the World",
                        explanation = "A biome is a huge region with its own climate and wildlife: rainforests, deserts, tundras, grasslands, and more!",
                        steps = listOf(
                            "1. Rainforest: hot, wet, and packed with life.",
                            "2. Desert: dry with extreme temperatures.",
                            "3. Tundra: frozen, with only tough plants surviving."
                        ),
                        example = "Rainforests cover 6% of Earth but hold half its species!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Which biome is hot, wet, and packed with life?",
                        practiceAnswer = "Rainforest",
                        practiceOptions = listOf("Rainforest", "Desert", "Tundra", "Grassland").shuffled(),
                        solveSteps = listOf("Hot + wet + dense life", "That describes the rainforest")
                    )
                    6 -> Lesson(
                        title = "The Nervous System",
                        explanation = "Your nervous system is the body's wiring! The brain decides, the spinal cord relays, and nerves carry messages.",
                        steps = listOf(
                            "1. Nerves sense the world and send signals.",
                            "2. The spinal cord is the information highway.",
                            "3. The brain reads signals and sends orders back."
                        ),
                        example = "Touching something hot: the signal reaches your brain in a blink!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Which organ is the control center of the nervous system?",
                        practiceAnswer = "The brain",
                        practiceOptions = listOf("The brain", "The heart", "The lungs", "The stomach").shuffled(),
                        solveSteps = listOf("The control center makes decisions", "That is the brain")
                    )
                    else -> Lesson(
                        title = "Evidence for Evolution",
                        explanation = "Evolution is change over generations! Fossils, DNA, and similar body parts all prove species change over time.",
                        steps = listOf(
                            "1. Fossils show ancient versions of modern animals.",
                            "2. DNA reveals how closely species are related.",
                            "3. Whales have tiny hip bones - leftovers from walking ancestors!"
                        ),
                        example = "Human and chimpanzee DNA is about 99% identical!",
                        type = LessonType.SCIENCE_FACTS,
                        practiceQuestion = "Whales have tiny hip bones left over from walking ancestors. These are called ___ structures.",
                        practiceAnswer = "vestigial",
                        practiceOptions = listOf("vestigial", "identical", "modern", "perfect").shuffled(),
                        solveSteps = listOf("Leftover parts from ancestors", "These are called vestigial structures")
                    )
                }
            }
        }
    }

    private val k2LifeScience = listOf(
        Pair("What do animals need to breathe?", "Oxygen"),
        Pair("A baby cow is called a ___.", "Calf"),
        Pair("What part of the plant grows underground?", "Roots"),
        Pair("Which animal lays eggs?", "Chicken"),
        Pair("Humans use ___ to taste food.", "Tongues"),
        Pair("Which part of a tree is used to make paper?", "Trunk"),
        Pair("What do caterpillars turn into?", "Butterflies"),
        Pair("Where do fish live?", "Water"),
        Pair("A baby dog is called a ___.", "Puppy"),
        Pair("Which organ pumps blood?", "Heart"),
        Pair("Which animal has a very long neck?", "Giraffe"),
        Pair("Plants make their own ___.", "Food"),
        Pair("What do we call the covering on a bird?", "Feathers"),
        Pair("Which animal hibernates in winter?", "Bear"),
        Pair("What do seeds grow into?", "Plants"),
            Pair("A group of fish swimming together is called a ___.", "School"),
            Pair("Which animal can change color to blend in?", "Chameleon"),
    )

    private val elementaryLifeScience = listOf(
        Pair("What is the green pigment in plants?", "Chlorophyll"),
        Pair("Animals that only eat plants are ___.", "Herbivores"),
        Pair("What is the basic unit of life?", "Cell"),
        Pair("The heart and lungs are part of the ___ system.", "Circulatory"),
        Pair("Which organ filters waste from the blood?", "Kidney"),
        Pair("What is the name for animals with backbones?", "Vertebrates"),
        Pair("Plants take in ___ and release oxygen.", "Carbon Dioxide"),
        Pair("A group of living things and their environment is an ___.", "Ecosystem"),
        Pair("The food chain starts with ___.", "Producers"),
        Pair("Which organ is responsible for thinking?", "Brain"),
        Pair("What do we call the study of living things?", "Biology"),
        Pair("Reptiles are ___ animals.", "Cold-blooded"),
        Pair("What is the hard outer shell of an insect?", "Exoskeleton"),
        Pair("Which bird is known for its wisdom?", "Owl"),
        Pair("Fungi get their food from ___ matter.", "Decaying"),
            Pair("Animals that are active at night are ___.", "Nocturnal"),
            Pair("Shedding old skin or shells to grow is called ___.", "Molting"),
    )

    private val middleLifeScience = listOf(
        Pair("What is known as the powerhouse of the cell?", "Mitochondria"),
        Pair("DNA stands for ___ acid.", "Deoxyribonucleic"),
        Pair("What process do plants use to make energy?", "Photosynthesis"),
        Pair("The passing of traits from parents to offspring is ___.", "Heredity"),
        Pair("Which organelle stores genetic information?", "Nucleus"),
        Pair("What is the process of cell division called?", "Mitosis"),
        Pair("Humans have ___ pairs of chromosomes.", "23"),
        Pair("What is the primary source of energy for life on Earth?", "Sun"),
        Pair("The physical appearance of an organism is its ___.", "Phenotype"),
        Pair("Organisms that consist of only one cell are ___.", "Unicellular"),
        Pair("What is the largest organ of the human body?", "Skin"),
        Pair("The space between neurons is called a ___.", "Synapse"),
        Pair("Which system protects the body from disease?", "Immune"),
        Pair("What are the building blocks of proteins?", "Amino Acids"),
        Pair("Bacteria are examples of ___ cells.", "Prokaryotic"),
            Pair("Red blood cells carry ___ through the body.", "Oxygen"),
    )

    private val highLifeScience = listOf(
        Pair("What is the process by which populations change over time?", "Evolution"),
        Pair("Which enzyme breaks down starch in the mouth?", "Amylase"),
        Pair("The theory of natural selection was proposed by ___.", "Charles Darwin"),
        Pair("Adenine always pairs with ___ in DNA.", "Thymine"),
        Pair("What is the anaerobic process of breaking down glucose?", "Glycolysis"),
        Pair("Which hormone regulates blood sugar levels?", "Insulin"),
        Pair("The set of all genes in a population is the ___.", "Gene Pool"),
        Pair("What is the functional unit of the kidney?", "Nephron"),
        Pair("Xylem and phloem are types of ___ tissue in plants.", "Vascular"),
        Pair("A symbiotic relationship where both benefit is ___.", "Mutualism"),
        Pair("The light-independent reaction of photosynthesis is the ___.", "Calvin Cycle"),
        Pair("What is the maximum population size an environment can support?", "Carrying Capacity"),
        Pair("The sequence of three nucleotides on mRNA is a ___.", "Codon"),
        Pair("Which organelle is responsible for protein synthesis?", "Ribosome"),
        Pair("Transcription occurs in the ___ of a eukaryotic cell.", "Nucleus")
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
            gradeLevel <= 2 -> k2LifeScience
            gradeLevel <= 4 -> elementaryLifeScience
            gradeLevel <= 5 -> middleLifeScience
            else -> highLifeScience
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
        val allPoolAnswers = (k2LifeScience + elementaryLifeScience + middleLifeScience + highLifeScience).map { it.second }
        
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
