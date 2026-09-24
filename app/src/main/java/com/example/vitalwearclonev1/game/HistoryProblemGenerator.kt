package com.example.vitalwearclonev1.game

import kotlin.random.Random

class HistoryProblemGenerator(private val grade: String, private val floor: Int, private val isAdjusted: Boolean = false) {
    private val random = Random(System.currentTimeMillis())
    private val questionCounts = mutableMapOf<String, Int>()

    fun generateLesson(seenTitles: List<String> = emptyList()): Lesson {
        val gradeLevel = when(grade) {
            "K" -> 0
            else -> grade.toIntOrNull() ?: 1
        }
        
        val lessons = when {
            gradeLevel <= 3 -> listOf(
                "Historical Nicknames",
                "Community Helpers",
                "Then and Now",
                "Family History",
                "Symbols of Freedom",
                "Holidays and Traditions",
                "Maps and Globes",
                "Past, Present, Future",
                "The American Flag",
                "Famous Inventors"
            )
            gradeLevel <= 5 -> listOf(
                "Quoting History",
                "The Declaration of Independence",
                "The Silk Road",
                "Ancient Egypt",
                "The Constitution",
                "Explorers of the World",
                "Ancient Rome",
                "The Middle Ages",
                "Native American Cultures",
                "The Oregon Trail"
            )
            else -> listOf(
                "Common Eras & Dates",
                "The Magna Carta",
                "World War I & II",
                "The Renaissance",
                "The Industrial Revolution",
                "Civil Rights Movement",
                "Ancient Greece",
                "The Space Race",
                "The Great Depression",
                "The Cold War"
            )
        }

        val unseenLessons = lessons.filter { !seenTitles.contains(it) }
        // Paced learning: work through new skills in a stable, gentle order instead of jumping randomly.
        val selectedTitle = if (unseenLessons.isNotEmpty()) {
            if (isAdjusted) unseenLessons.first() else unseenLessons.random(random)
        } else lessons.random(random)

        return when (selectedTitle) {
            // GRADE K-3
            "Historical Nicknames" -> Lesson(
                title = "Historical Nicknames",
                explanation = "Important people in history often have special titles. These must be capitalized!",
                steps = listOf(
                    "1. Always capitalize the first letter of a title (like King or President).",
                    "2. Capitalize the name that follows.",
                    "3. If the title is used without a name, it's usually lowercase."
                ),
                example = "Correct: President Washington. Incorrect: president washington.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Should 'president' be capitalized in 'president Washington'?",
                practiceAnswer = "Yes",
                practiceOptions = listOf("Yes", "No").shuffled(),
                solveSteps = listOf("When used as a title before a name, capitalize it.")
            )
            "Community Helpers" -> Lesson(
                title = "Community Helpers",
                explanation = "History is made by people in our communities who help every day.",
                steps = listOf(
                    "1. Firefighters, Police Officers.",
                    "2. Teachers, Doctors.",
                    "3. These roles are often capitalized when used as a title."
                ),
                example = "Officer Miller helped us cross the street.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Correct this: teacher smith",
                practiceAnswer = "Teacher Smith",
                practiceOptions = listOf("Teacher Smith", "teacher smith", "Teacher smith", "teacher Smith").shuffled(),
                solveSteps = listOf("Capitalize the title 'Teacher' and the last name 'Smith'.")
            )
            "Then and Now" -> Lesson(
                title = "Then and Now",
                explanation = "History is the study of how things change over time.",
                steps = listOf(
                    "1. Transportation (Horses vs. Cars).",
                    "2. Communication (Letters vs. Email).",
                    "3. Always capitalize the names of historical periods."
                ),
                example = "In the Stone Age, people used stone tools.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Which came first: Horses or Cars?",
                practiceAnswer = "Horses",
                practiceOptions = listOf("Horses", "Cars").shuffled(),
                solveSteps = listOf("Horses were used for thousands of years before cars were invented.")
            )
            "Family History" -> Lesson(
                title = "Family History",
                explanation = "Your own family has a history! We learn about it from our parents and grandparents.",
                steps = listOf(
                    "1. Ask about where your family came from.",
                    "2. Look at old photos.",
                    "3. Names of relatives are proper nouns and need capital letters."
                ),
                example = "My Grandma lives in New York.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Is 'Grandma' a proper noun here?",
                practiceAnswer = "Yes",
                practiceOptions = listOf("Yes", "No").shuffled(),
                solveSteps = listOf("When used as a name, 'Grandma' is capitalized.")
            )
            "Symbols of Freedom" -> Lesson(
                title = "Symbols of Freedom",
                explanation = "A symbol is an object that represents an idea. Many symbols represent freedom and the USA.",
                steps = listOf(
                    "1. Liberty Bell (Freedom).",
                    "2. Statue of Liberty (Welcome).",
                    "3. Bald Eagle (Strength)."
                ),
                example = "The Bald Eagle is the national bird of the USA.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What does the Liberty Bell represent?",
                practiceAnswer = "Freedom",
                practiceOptions = listOf("Freedom", "Food", "School", "Games").shuffled(),
                solveSteps = listOf("The Liberty Bell is a famous symbol of American independence and freedom.")
            )
            "Holidays and Traditions" -> Lesson(
                title = "Holidays and Traditions",
                explanation = "Holidays are special days to remember important events or people in history.",
                steps = listOf(
                    "1. Independence Day (July 4th).",
                    "2. Thanksgiving (Gratitude).",
                    "3. Memorial Day (Honoring soldiers)."
                ),
                example = "We watch fireworks on Independence Day.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Which holiday is on July 4th?",
                practiceAnswer = "Independence Day",
                practiceOptions = listOf("Independence Day", "Christmas", "Halloween", "Easter").shuffled(),
                solveSteps = listOf("July 4th is the birthday of the United States.")
            )

            // GRADE 4-6
            "Quoting History" -> Lesson(
                title = "Quoting History",
                explanation = "When we write down exactly what someone said, we use quotation marks.",
                steps = listOf(
                    "1. Start with an opening quote (\").",
                    "2. Capitalize the first word of the quote.",
                    "3. Put a comma before the quote if it follows a word like 'said'.",
                    "4. Put the end punctuation inside the closing quote (\")."
                ),
                example = "Lincoln said, \"Four score and seven years ago.\"",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What mark comes before a quote if someone 'said' it?",
                practiceAnswer = ",",
                practiceOptions = listOf(",", ".", ";", "!").shuffled(),
                solveSteps = listOf("Use a comma to introduce a direct quote.")
            )
            "The Declaration of Independence" -> Lesson(
                title = "The Declaration of Independence",
                explanation = "This famous document was written to declare that the 13 colonies were free.",
                steps = listOf(
                    "1. It was signed in 1776.",
                    "2. Use a comma after the day and year.",
                    "3. Capitalize the names of the signers."
                ),
                example = "It was signed on July 4, 1776, in Philadelphia.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "In what year was the Declaration signed?",
                practiceAnswer = "1776",
                practiceOptions = listOf("1776", "1492", "1812", "1945").shuffled(),
                solveSteps = listOf("The document was adopted on July 4, 1776.")
            )
            "The Silk Road" -> Lesson(
                title = "The Silk Road",
                explanation = "The Silk Road was an ancient network of trade routes connecting the East and West.",
                steps = listOf(
                    "1. People traded silk, spices, and ideas.",
                    "2. Capitalize the names of specific trade routes.",
                    "3. Use commas to separate items in a list of traded goods."
                ),
                example = "They traded silk, gold, and jade along the Silk Road.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Name one thing traded on the Silk Road.",
                practiceAnswer = "silk",
                practiceOptions = listOf("silk", "computers", "cars", "plastic").shuffled(),
                solveSteps = listOf("Silk was the most famous item traded on this route.")
            )
            "Ancient Egypt" -> Lesson(
                title = "Ancient Egypt",
                explanation = "Ancient Egyptians lived along the Nile River and built amazing pyramids.",
                steps = listOf(
                    "1. They used pictures called Hieroglyphics to write.",
                    "2. Their kings were called Pharaohs.",
                    "3. Capitalize the names of rivers and kings."
                ),
                example = "King Tut was a famous Pharaoh.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What was the Egyptian system of writing called?",
                practiceAnswer = "Hieroglyphics",
                practiceOptions = listOf("Hieroglyphics", "Alphabet", "Code", "Emojis").shuffled(),
                solveSteps = listOf("Hieroglyphics used symbols and pictures to represent words.")
            )
            "The Constitution" -> Lesson(
                title = "The Constitution",
                explanation = "The Constitution is the highest law in the United States. it describes how the government works.",
                steps = listOf(
                    "1. It starts with 'We the People'.",
                    "2. It was written in 1787.",
                    "3. It includes the Bill of Rights."
                ),
                example = "The Constitution protects our rights.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What are the first three words of the Constitution?",
                practiceAnswer = "We the People",
                practiceOptions = listOf("We the People", "In the beginning", "I pledge allegiance", "God bless America").shuffled(),
                solveSteps = listOf("The preamble begins with 'We the People'.")
            )
            "Explorers of the World" -> Lesson(
                title = "Explorers of the World",
                explanation = "Explorers traveled to find new lands, resources, and trade routes.",
                steps = listOf(
                    "1. Christopher Columbus (Americas).",
                    "2. Ferdinand Magellan (First to sail around the world).",
                    "3. Marco Polo (Asia)."
                ),
                example = "Marco Polo traveled the Silk Road to China.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Who was the first explorer to sail around the world?",
                practiceAnswer = "Ferdinand Magellan",
                practiceOptions = listOf("Ferdinand Magellan", "Christopher Columbus", "Marco Polo", "Neil Armstrong").shuffled(),
                solveSteps = listOf("Magellan's crew completed the first circumnavigation of the globe.")
            )

            // MIDDLE SCHOOL+
            "Common Eras & Dates" -> Lesson(
                title = "Common Eras & Dates",
                explanation = "Writing about time requires specific formatting for abbreviations.",
                steps = listOf(
                    "1. B.C. (Before Christ) or B.C.E. (Before Common Era).",
                    "2. A.D. (Anno Domini) or C.E. (Common Era).",
                    "3. Use a comma after the day and the year when writing a full sentence."
                ),
                example = "The signing took place on July 4, 1776, in Philadelphia.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What does B.C.E. stand for?",
                practiceAnswer = "Before Common Era",
                practiceOptions = listOf("Before Common Era", "Before Christ Era", "Big City Era", "Before Current Era").shuffled(),
                solveSteps = listOf("B.C.E. is the secular alternative to B.C.")
            )
            "The Magna Carta" -> Lesson(
                title = "The Magna Carta",
                explanation = "Signed in 1215, it was one of the first documents to limit the power of a king.",
                steps = listOf(
                    "1. It established that everyone is subject to the law.",
                    "2. Use a comma to separate the year from the rest of the sentence.",
                    "3. Capitalize the name of the document."
                ),
                example = "In 1215, King John signed the Magna Carta.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Who signed the Magna Carta?",
                practiceAnswer = "King John",
                practiceOptions = listOf("King John", "King Arthur", "King George", "King Henry").shuffled(),
                solveSteps = listOf("King John of England was forced to sign it by his barons.")
            )
            "World War I & II" -> Lesson(
                title = "World War I & II",
                explanation = "These were global conflicts that changed the map of the world.",
                steps = listOf(
                    "1. Use Roman Numerals (I and II).",
                    "2. Capitalize the names of all wars.",
                    "3. Be careful with the spelling of 'Armistice' and 'Treaty'."
                ),
                example = "World War I ended with an armistice in 1918.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Which World War used the numeral 'II'?",
                practiceAnswer = "World War II",
                practiceOptions = listOf("World War II", "World War I", "World War III", "Civil War").shuffled(),
                solveSteps = listOf("The second global conflict is written as World War II.")
            )
            "The Renaissance" -> Lesson(
                title = "The Renaissance",
                explanation = "A period of 'rebirth' in art, science, and culture following the Middle Ages.",
                steps = listOf(
                    "1. Famous artists: Leonardo da Vinci, Michelangelo.",
                    "2. It began in Italy in the 14th century.",
                    "3. Capitalize 'Renaissance' and the names of artists."
                ),
                example = "Leonardo da Vinci painted the Mona Lisa.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What does 'Renaissance' mean?",
                practiceAnswer = "Rebirth",
                practiceOptions = listOf("Rebirth", "War", "Darkness", "Discovery").shuffled(),
                solveSteps = listOf("Renaissance is a French word meaning 'rebirth'.")
            )
            "The Industrial Revolution" -> Lesson(
                title = "The Industrial Revolution",
                explanation = "A period where production shifted from hand tools to machines and factories.",
                steps = listOf(
                    "1. Began in Great Britain.",
                    "2. Led to growth of cities (urbanization).",
                    "3. Invention of the steam engine was key."
                ),
                example = "The steam engine powered new trains and factories.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Where did the Industrial Revolution begin?",
                practiceAnswer = "Great Britain",
                practiceOptions = listOf("Great Britain", "USA", "France", "China").shuffled(),
                solveSteps = listOf("Britain had the coal, iron, and money needed to start the revolution.")
            )
            // GRADE K-3 (continued)
            "Maps and Globes" -> Lesson(
                title = "Maps and Globes",
                explanation = "A globe is a round model of the whole Earth. A map is a flat drawing of a place. Both help us find our way!",
                steps = listOf(
                    "1. A globe shows the whole round Earth.",
                    "2. A map shows a flat view of an area.",
                    "3. The compass rose shows North, South, East, and West."
                ),
                example = "Find your country on a globe, then find your street on a map!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What is a round model of the whole Earth called?",
                practiceAnswer = "Globe",
                practiceOptions = listOf("Globe", "Map", "Atlas", "Compass").shuffled(),
                solveSteps = listOf("It is round like the Earth itself", "Answer: Globe")
            )
            "Past, Present, Future" -> Lesson(
                title = "Past, Present, Future",
                explanation = "The past already happened, the present is happening now, and the future hasn't happened yet!",
                steps = listOf(
                    "1. Past: yesterday, last year, long ago.",
                    "2. Present: right now, today.",
                    "3. Future: tomorrow, next week, someday."
                ),
                example = "Past: I ate breakfast. Present: I am reading. Future: I will play later.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Which word means 'happening right now'?",
                practiceAnswer = "Present",
                practiceOptions = listOf("Present", "Past", "Future", "History").shuffled(),
                solveSteps = listOf("'Right now' is happening in this moment", "Answer: Present")
            )
            "The American Flag" -> Lesson(
                title = "The American Flag",
                explanation = "The American flag has 13 stripes for the 13 original colonies and 50 stars for the 50 states!",
                steps = listOf(
                    "1. Count the stripes: there are 13, one per original colony.",
                    "2. Count the stars: 50, one per state.",
                    "3. Red stands for courage, white for purity, blue for justice."
                ),
                example = "Every star was added as new states joined the country!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "How many stripes are on the American flag?",
                practiceAnswer = "13",
                practiceOptions = listOf("13", "50", "12", "10").shuffled(),
                solveSteps = listOf("One stripe for each of the 13 original colonies", "Answer: 13")
            )
            "Famous Inventors" -> Lesson(
                title = "Famous Inventors",
                explanation = "Inventors solve problems with new ideas! Thomas Edison's light bulb changed how the whole world lives after dark.",
                steps = listOf(
                    "1. An inventor spots a problem.",
                    "2. They test many ideas - Edison tried thousands!",
                    "3. Their invention changes everyday life."
                ),
                example = "Alexander Graham Bell invented the telephone. The Wright brothers invented the airplane.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Who invented the practical light bulb?",
                practiceAnswer = "Thomas Edison",
                practiceOptions = listOf("Thomas Edison", "Alexander Graham Bell", "Benjamin Franklin", "Henry Ford").shuffled(),
                solveSteps = listOf("Edison's lab tested over 1,000 designs", "Answer: Thomas Edison")
            )

            // GRADE 4-6 (continued)
            "Ancient Rome" -> Lesson(
                title = "Ancient Rome",
                explanation = "Ancient Rome built an empire across Europe with roads, aqueducts, and arenas like the giant Colosseum!",
                steps = listOf(
                    "1. Rome grew from a small city into a huge empire.",
                    "2. Romans built straight roads and stone bridges still standing today.",
                    "3. The Colosseum held gladiator contests for 50,000 fans."
                ),
                example = "All roads lead to Rome - the saying comes from their road network!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "The Colosseum was built in which ancient city?",
                practiceAnswer = "Rome",
                practiceOptions = listOf("Rome", "Athens", "Cairo", "Paris").shuffled(),
                solveSteps = listOf("The Colosseum still stands in Italy's capital", "Answer: Rome")
            )
            "The Middle Ages" -> Lesson(
                title = "The Middle Ages",
                explanation = "The Middle Ages were a time of knights, castles, and kingdoms across Europe, roughly 500 to 1500 AD.",
                steps = listOf(
                    "1. Kings and queens ruled lands called kingdoms.",
                    "2. Knights trained for battle and followed a code called chivalry.",
                    "3. Lords lived in strong stone castles for protection."
                ),
                example = "A knight's armor could weigh as much as a big dog!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "What were the big stone homes of lords called?",
                practiceAnswer = "Castles",
                practiceOptions = listOf("Castles", "Pyramids", "Temples", "Cabins").shuffled(),
                solveSteps = listOf("Lords needed strong, safe homes", "Answer: Castles")
            )
            "Native American Cultures" -> Lesson(
                title = "Native American Cultures",
                explanation = "Native American tribes built rich cultures shaped by their land - from Plains tepees to Northwest totem poles!",
                steps = listOf(
                    "1. Different regions meant different homes, food, and crafts.",
                    "2. Plains tribes followed buffalo herds and lived in tepees.",
                    "3. Pacific Northwest tribes carved tall totem poles telling family stories."
                ),
                example = "Totem poles can be taller than a house!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Totem poles were carved by tribes of which region?",
                practiceAnswer = "Pacific Northwest",
                practiceOptions = listOf("Pacific Northwest", "Great Plains", "Desert Southwest", "Southeast").shuffled(),
                solveSteps = listOf("Tall cedar trees grew in the Northwest", "Tribes there carved them into totem poles")
            )
            "The Oregon Trail" -> Lesson(
                title = "The Oregon Trail",
                explanation = "In the 1800s, pioneer families rode covered wagons 2,000 miles west on the Oregon Trail seeking new land!",
                steps = listOf(
                    "1. Families packed everything into covered wagons.",
                    "2. Oxen pulled the wagons across rivers and mountains.",
                    "3. The trip took about 5 months of hard travel."
                ),
                example = "Wagon wheels left ruts in the rock you can still see today!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Pioneers on the Oregon Trail traveled west in covered ___.",
                practiceAnswer = "wagons",
                practiceOptions = listOf("wagons", "trains", "cars", "boats").shuffled(),
                solveSteps = listOf("No railroads crossed the plains yet", "Families used covered wagons pulled by oxen")
            )

            // HIGH SCHOOL+ (continued)
            "Ancient Greece" -> Lesson(
                title = "Ancient Greece",
                explanation = "Ancient Greece gave the world democracy, the Olympics, philosophy, and amazing temples like the Parthenon!",
                steps = listOf(
                    "1. Greek city-states like Athens invented voting by citizens.",
                    "2. Thinkers like Socrates asked big questions about life.",
                    "3. The first Olympic games were held in Olympia in 776 BC."
                ),
                example = "The word 'democracy' comes from Greek words meaning 'rule by the people'!",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "The Olympic games began in which ancient country?",
                practiceAnswer = "Greece",
                practiceOptions = listOf("Greece", "Rome", "Egypt", "China").shuffled(),
                solveSteps = listOf("The games were held in Olympia", "Olympia is in ancient Greece")
            )
            "The Space Race" -> Lesson(
                title = "The Space Race",
                explanation = "The Space Race was the US vs. Soviet Union competition to reach space first - ending with Americans on the Moon in 1969!",
                steps = listOf(
                    "1. The Soviets launched the first satellite, Sputnik, in 1957.",
                    "2. Yuri Gagarin became the first human in space in 1961.",
                    "3. Neil Armstrong first walked on the Moon in 1969."
                ),
                example = "Armstrong's famous words: 'One small step for man, one giant leap for mankind.'",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Who was the first person to walk on the Moon?",
                practiceAnswer = "Neil Armstrong",
                practiceOptions = listOf("Neil Armstrong", "Buzz Aldrin", "Yuri Gagarin", "John Glenn").shuffled(),
                solveSteps = listOf("Apollo 11 landed in 1969", "Its commander, Neil Armstrong, stepped out first")
            )
            "The Great Depression" -> Lesson(
                title = "The Great Depression",
                explanation = "The Great Depression was a terrible worldwide economic crash starting in 1929, when banks failed and millions lost jobs.",
                steps = listOf(
                    "1. The US stock market crashed in October 1929.",
                    "2. Banks closed and businesses shut down.",
                    "3. President Roosevelt's 'New Deal' programs helped the country recover."
                ),
                example = "At the worst point, 1 in 4 American workers had no job.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "The Great Depression began with the stock market crash in ___.",
                practiceAnswer = "1929",
                practiceOptions = listOf("1929", "1914", "1939", "1945").shuffled(),
                solveSteps = listOf("The crash happened in October 1929", "Answer: 1929")
            )
            "The Cold War" -> Lesson(
                title = "The Cold War",
                explanation = "The Cold War was the tense, decades-long standoff between the US and the Soviet Union - fought with spies and science, not battles.",
                steps = listOf(
                    "1. After WWII, the US and Soviets became rival superpowers.",
                    "2. Both built up nuclear weapons but never used them on each other.",
                    "3. It ended around 1991 when the Soviet Union broke apart."
                ),
                example = "The Berlin Wall splitting Germany was the Cold War's most famous symbol.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "The Cold War was mainly between the US and the ___.",
                practiceAnswer = "Soviet Union",
                practiceOptions = listOf("Soviet Union", "Germany", "Japan", "China").shuffled(),
                solveSteps = listOf("Two superpowers remained after WWII", "The US rival was the Soviet Union")
            )
            else -> Lesson(
                title = "Civil Rights Movement",
                explanation = "A struggle for social justice and equal rights for African Americans during the 1950s and 60s.",
                steps = listOf(
                    "1. Led by Martin Luther King Jr.",
                    "2. Aimed to end segregation.",
                    "3. Use of non-violent protest."
                ),
                example = "The 'I Have a Dream' speech was a key moment.",
                type = LessonType.SCIENCE_FACTS,
                practiceQuestion = "Who was a key leader of the Civil Rights Movement?",
                practiceAnswer = "Martin Luther King Jr.",
                practiceOptions = listOf("Martin Luther King Jr.", "George Washington", "Abraham Lincoln", "Thomas Jefferson").shuffled(),
                solveSteps = listOf("Dr. King is famous for his leadership and non-violent message.")
            )
        }
    }

    private val k2History = listOf(
        Pair("Who was the first President of the US?", "George Washington"),
        Pair("What holiday is in July?", "Independence Day"),
        Pair("Who is a famous civil rights leader?", "Martin Luther King Jr."),
        Pair("Which symbol represents America?", "Bald Eagle"),
        Pair("What do we call the US flag?", "Stars and Stripes"),
        Pair("Who lived in America before explorers arrived?", "Native Americans"),
        Pair("Where does the President live?", "White House"),
        Pair("What do we celebrate on Thanksgiving?", "Gratitude"),
        Pair("Which President is on the penny?", "Abraham Lincoln"),
        Pair("Who was a famous woman who helped slaves escape?", "Harriet Tubman"),
        Pair("What is the capital of the USA?", "Washington D.C."),
        Pair("Who discovered the lightbulb?", "Thomas Edison"),
        Pair("The Liberty ___ is in Philadelphia.", "Bell"),
        Pair("What are the three colors of the US flag?", "Red, White, Blue"),
        Pair("Who was the first man to walk on the moon?", "Neil Armstrong"),
            Pair("How many states are in the USA?", "50"),
    )

    private val elementaryHistory = listOf(
        Pair("Who discovered America in 1492?", "Christopher Columbus"),
        Pair("The Pilgrims sailed on a ship called the ___.", "Mayflower"),
        Pair("Who wrote the Declaration of Independence?", "Thomas Jefferson"),
        Pair("The ___ was a path used by pioneers.", "Oregon Trail"),
        Pair("Who was the 16th President during the Civil War?", "Abraham Lincoln"),
        Pair("The Boston ___ Party was a protest against taxes.", "Tea"),
        Pair("The first 10 amendments to the Constitution are the ___.", "Bill of Rights"),
        Pair("Who made the first solo flight across the Atlantic?", "Charles Lindbergh"),
        Pair("Benjamin Franklin is known for his work with ___.", "Electricity"),
        Pair("The Louisiana ___ doubled the size of the US.", "Purchase"),
        Pair("Who was the first woman to fly solo across the Atlantic?", "Amelia Earhart"),
        Pair("The ___ Canal connected the Great Lakes to the Atlantic.", "Erie"),
        Pair("Who was the main general of the Continental Army?", "George Washington"),
        Pair("The Star-Spangled Banner is the national ___.", "Anthem"),
        Pair("Who invented the telephone?", "Alexander Graham Bell"),
            Pair("The Great Wall is in which country?", "China"),
            Pair("The ancient Olympic games were first held in ___.", "Greece"),
    )

    private val middleHistory = listOf(
        Pair("Ancient ___ built the pyramids.", "Egyptians"),
        Pair("The ___ was a period of rebirth in art.", "Renaissance"),
        Pair("The Industrial Revolution began in ___.", "England"),
        Pair("The ___ was a long war between North and South.", "Civil War"),
        Pair("The Magna Carta was signed in the year ___.", "1215"),
        Pair("The Silk Road was a trade route between Europe and ___.", "Asia"),
        Pair("The Roman Empire was centered in modern-day ___.", "Italy"),
        Pair("The Black ___ killed millions in the Middle Ages.", "Death"),
        Pair("The French Revolution began with the storming of the ___.", "Bastille"),
        Pair("Who was the leader of the Mongol Empire?", "Genghis Khan"),
        Pair("The ___ was a series of religious wars in the Middle Ages.", "Crusades"),
        Pair("The Aztecs lived in what is now ___.", "Mexico"),
        Pair("Johannes Gutenberg invented the printing ___.", "Press"),
        Pair("The Vikings came from northern ___.", "Europe"),
        Pair("The Code of ___ was one of the first sets of laws.", "Hammurabi"),
            Pair("Marco Polo traveled all the way to ___.", "China"),
    )

    private val highHistory = listOf(
        Pair("The Archduke Franz Ferdinand was assassinated in ___.", "Sarajevo"),
        Pair("The Manhattan Project created the ___.", "Atomic Bomb"),
        Pair("The Cold War was between the US and the ___.", "Soviet Union"),
        Pair("The Treaty of ___ ended World War I.", "Versailles"),
        Pair("Apartheid was a system of segregation in ___.", "South Africa"),
        Pair("The Great Depression began with the stock market crash in ___.", "1929"),
        Pair("The Cuban ___ Crisis was a tense standoff in 1962.", "Missile"),
        Pair("The Berlin ___ fell in 1989.", "Wall"),
        Pair("The League of ___ was formed after WWI to prevent wars.", "Nations"),
        Pair("Who was the leader of the Soviet Union during WWII?", "Joseph Stalin"),
        Pair("The Marshall ___ helped rebuild Europe after WWII.", "Plan"),
        Pair("The Industrial Revolution led to a rise in ___.", "Urbanization"),
        Pair("The Enlightenment emphasized ___ and individualism.", "Reason"),
        Pair("The Watergate scandal led to the resignation of ___.", "Richard Nixon"),
        Pair("The Vietnam War ended in ___.", "1975"),
            Pair("The first satellite in space was called ___.", "Sputnik"),
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
            gradeLevel <= 2 -> k2History
            gradeLevel <= 4 -> elementaryHistory
            gradeLevel <= 5 -> middleHistory
            else -> highHistory
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
        val allPoolAnswers = (k2History + elementaryHistory + middleHistory + highHistory).map { it.second }
        
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
