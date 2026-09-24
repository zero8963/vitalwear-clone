package com.example.vitalwearclonev1.game

import kotlin.random.Random

class ReadingProblemGenerator(private val grade: String, private val floor: Int, private val isAdjusted: Boolean = false) {
    private val random = Random(System.currentTimeMillis())
    private val questionCounts = mutableMapOf<String, Int>()

    fun generateLesson(seenTitles: List<String> = emptyList()): Lesson {
        val gradeLevel = when(grade) {
            "K" -> 0
            else -> grade.toIntOrNull() ?: 1
        }
        
        val lessons = when {
            gradeLevel <= 2 -> listOf(
                "Uppercase Letters & Periods",
                "Question Marks",
                "Proper Nouns",
                "Silent E",
                "Rhyming Words",
                "Reading Short Vowels",
                "Exclamation Marks",
                "Capitalizing the Word I",
                "Beginning Blends",
                "The Magic Long A"
            )
            gradeLevel <= 5 -> listOf(
                "Comma Rules",
                "Apostrophes in Contractions",
                "A vs. An",
                "Homophones",
                "Compound Words",
                "Prefixes and Suffixes",
                "Their, There, They're",
                "Synonyms and Antonyms",
                "Quotation Marks in Dialogue",
                "Topic Sentences"
            )
            else -> listOf(
                "Common Spelling Traps",
                "Using Semicolons & Colons",
                "Subject-Verb Agreement",
                "Active vs. Passive Voice",
                "Metaphors and Similes",
                "Context Clues",
                "Thesis Statements",
                "Comma Splices",
                "Tone vs. Mood",
                "Analogies"
            )
        }

        val unseenLessons = lessons.filter { !seenTitles.contains(it) }
        // Paced learning: work through new skills in a stable, gentle order instead of jumping randomly.
        val selectedTitle = if (unseenLessons.isNotEmpty()) {
            if (isAdjusted) unseenLessons.first() else unseenLessons.random(random)
        } else lessons.random(random)

        return when (selectedTitle) {
            // GRADE K-2
            "Uppercase Letters & Periods" -> {
                val sentences = listOf("the cat is big", "i see a dog", "the sun is hot", "my hat is red")
                val n = random.nextInt(sentences.size)
                val raw = sentences[n]
                val corrected = raw.replaceFirstChar { it.uppercase() } + "."
                Lesson(
                    title = "Uppercase Letters & Periods",
                    explanation = "Every sentence starts with a big letter and ends with a dot called a period.",
                    steps = listOf(
                        "1. Check the very first word: Is it capitalized?",
                        "2. Look at the end of the thought: Did you put a period (.)?",
                        "3. Make sure names (like John) also start with a big letter."
                    ),
                    example = "Correct: The cat is napping. Incorrect: the cat is napping",
                    type = LessonType.PUNCTUATION_AND_SPELLING,
                    practiceQuestion = "Capitalize and end: $raw",
                    practiceAnswer = corrected,
                    practiceOptions = listOf(corrected, raw, raw + "!", raw.uppercase()).shuffled(),
                    solveSteps = listOf("Start: $raw", "Upper: ${raw.replaceFirstChar { it.uppercase() }}", "End: $corrected")
                )
            }
            "Question Marks" -> Lesson(
                title = "Question Marks",
                explanation = "When you ask something, the sentence must end with a question mark (?) instead of a period.",
                steps = listOf(
                    "1. Does the sentence ask a question?",
                    "2. Words like Who, What, Where, Why, and How often start questions.",
                    "3. Put the ? at the very end."
                ),
                example = "Where is the ball?",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Finish this: What time is it __",
                practiceAnswer = "?",
                practiceOptions = listOf("?", ".", "!", ",").shuffled(),
                solveSteps = listOf("The sentence asks a question", "Use '?' at the end")
            )
            "Proper Nouns" -> Lesson(
                title = "Proper Nouns",
                explanation = "Names of people, places, and pets always start with a capital letter.",
                steps = listOf(
                    "1. Is it a specific name of a person?",
                    "2. Is it the name of a city or country?",
                    "3. Capitalize only that word."
                ),
                example = "I live in Paris with my friend Tom.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Should 'london' be capitalized?",
                practiceAnswer = "Yes",
                practiceOptions = listOf("Yes", "No").shuffled(),
                solveSteps = listOf("'London' is a city name", "Cities are proper nouns", "Proper nouns are capitalized")
            )
            "Silent E" -> Lesson(
                title = "Silent E",
                explanation = "When 'e' is at the end of a word, it often stays silent and makes the other vowel say its name.",
                steps = listOf(
                    "1. Look for 'e' at the end.",
                    "2. Don't say the 'e' sound.",
                    "3. Make the first vowel sound long (like 'A' in Cake)."
                ),
                example = "Hop becomes Hope. Cap becomes Cape.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "What word do you get if you add E to 'Hat'?",
                practiceAnswer = "Hate",
                practiceOptions = listOf("Hate", "Hatey", "Hat", "Haate").shuffled(),
                solveSteps = listOf("Hat + E = Hate", "The 'a' becomes long", "The 'e' is silent")
            )
            "Rhyming Words" -> {
                val words = listOf("Cat", "Dog", "Blue", "Rain")
                val rhymes = mapOf("Cat" to "Mat", "Dog" to "Log", "Blue" to "Glue", "Rain" to "Train")
                val n = words.random(random)
                Lesson(
                    title = "Rhyming Words",
                    explanation = "Words rhyme when they have the same ending sound.",
                    steps = listOf(
                        "1. Listen to the end of the word.",
                        "2. Think of other words with that same sound.",
                        "3. Changing the first letter often makes a rhyme."
                    ),
                    example = "House and Mouse rhyme!",
                    type = LessonType.PUNCTUATION_AND_SPELLING,
                    practiceQuestion = "Which word rhymes with '$n'?",
                    practiceAnswer = rhymes[n]!!,
                    practiceOptions = listOf(rhymes[n]!!, "Apple", "Bird", "Sun").shuffled(),
                    solveSteps = listOf("Listen to '$n'", "The end sound matches '${rhymes[n]}'")
                )
            }
            "Reading Short Vowels" -> {
                Lesson(
                    title = "Short Vowel Sounds",
                    explanation = "Vowels (a, e, i, o, u) have short sounds like 'ah', 'eh', 'ih', 'off', 'uh'.",
                    steps = listOf(
                        "1. Look at the vowel in the middle of a short word.",
                        "2. Try the short sound first.",
                        "3. Example: 'p-i-g' (ih) makes 'pig'."
                    ),
                    example = "Cat (a), Hen (e), Pig (i), Dog (o), Bug (u).",
                    type = LessonType.PUNCTUATION_AND_SPELLING,
                    practiceQuestion = "What is the vowel in the word 'SUN'?",
                    practiceAnswer = "u",
                    practiceOptions = listOf("u", "a", "e", "o").shuffled(),
                    solveSteps = listOf("S-U-N", "The middle letter is U")
                )
            }

            // GRADE 3-5
            "Comma Rules" -> Lesson(
                title = "Comma Rules",
                explanation = "Commas help readers take a small breath and separate items in a list.",
                steps = listOf(
                    "1. Use a comma when listing three or more things.",
                    "2. Use a comma before words like 'but' or 'and' when joining two full sentences.",
                    "3. Use a comma to separate the day from the year in a date."
                ),
                example = "I like apples, oranges, and bananas.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Add commas: red blue green",
                practiceAnswer = "red, blue, green",
                practiceOptions = listOf("red, blue, green", "red blue green", "red, blue green", "red blue, green").shuffled(),
                solveSteps = listOf("Item 1: red", "Item 2: blue", "Item 3: green", "Combine: red, blue, green")
            )
            "Apostrophes in Contractions" -> Lesson(
                title = "Apostrophes in Contractions",
                explanation = "Apostrophes (') are used to join two words together into one short word.",
                steps = listOf(
                    "1. Put the apostrophe where letters were taken out.",
                    "2. Common ones: do not -> don't, I am -> I'm, it is -> it's.",
                    "3. Make sure the apostrophe is in the right spot!"
                ),
                example = "I can't go today instead of I can not go today.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "What is the contraction for 'can not'?",
                practiceAnswer = "can't",
                practiceOptions = listOf("can't", "cant", "ca'nt", "canot").shuffled(),
                solveSteps = listOf("can + not", "Remove 'no'", "Add ': can't")
            )
            "A vs. An" -> Lesson(
                title = "A vs. An",
                explanation = "Use 'an' before words that start with a vowel sound (a, e, i, o, u). Use 'a' for everything else.",
                steps = listOf(
                    "1. Look at the first letter of the next word.",
                    "2. If it sounds like a vowel, use 'an'.",
                    "3. Otherwise, use 'a'."
                ),
                example = "An apple. A banana. An elephant.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "__ orange (a or an?)",
                practiceAnswer = "an",
                practiceOptions = listOf("a", "an").shuffled(),
                solveSteps = listOf("'Orange' starts with 'O'", "'O' is a vowel", "Use 'an'")
            )
            "Homophones" -> Lesson(
                title = "Homophones",
                explanation = "Homophones are words that sound the same but have different meanings and spellings.",
                steps = listOf(
                    "1. Think about what the word means.",
                    "2. Check the spelling.",
                    "3. Use the one that fits the sentence."
                ),
                example = "I 'write' with a pen. I have the 'right' answer.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "I can ___ the ocean. (see or sea?)",
                practiceAnswer = "see",
                practiceOptions = listOf("see", "sea").shuffled(),
                solveSteps = listOf("'See' means looking with eyes", "'Sea' means the ocean", "Use 'see'")
            )
            "Compound Words" -> Lesson(
                title = "Compound Words",
                explanation = "A compound word is made when two smaller words are joined together to make a new word.",
                steps = listOf(
                    "1. Find two words that make sense alone.",
                    "2. Put them together.",
                    "3. Check if the new word has its own meaning."
                ),
                example = "Rain + Bow = Rainbow. Sun + Flower = Sunflower.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Combine 'FOOT' and 'BALL'. What is the word?",
                practiceAnswer = "football",
                practiceOptions = listOf("football", "ballfoot", "footsball", "feetball").shuffled(),
                solveSteps = listOf("Foot + Ball = Football")
            )
            "Prefixes and Suffixes" -> Lesson(
                title = "Prefixes and Suffixes",
                explanation = "Prefixes go at the start of a word. Suffixes go at the end. They change the word's meaning.",
                steps = listOf(
                    "1. Prefix 'un-' means 'not' (unhappy).",
                    "2. Suffix '-ful' means 'full of' (joyful).",
                    "3. Suffix '-less' means 'without' (fearless)."
                ),
                example = "Restart means to start again.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "What does 'UNHAPPY' mean?",
                practiceAnswer = "Not happy",
                practiceOptions = listOf("Not happy", "Very happy", "Almost happy", "Always happy").shuffled(),
                solveSteps = listOf("Prefix 'un-' means 'not'", "So un-happy = not happy")
            )

            // MIDDLE SCHOOL+
            "Common Spelling Traps" -> Lesson(
                title = "Common Spelling Traps",
                explanation = "Some words are tricky! Learning patterns helps you avoid errors.",
                steps = listOf(
                    "1. 'i' before 'e', except after 'c' (Believe vs. Receive).",
                    "2. Their (belongs to them), There (over there), They're (they are).",
                    "3. Its (belongs to it) vs. It's (it is)."
                ),
                example = "They're going to put their bags over there.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Which 'there' means it belongs to them?",
                practiceAnswer = "their",
                practiceOptions = listOf("their", "there", "they're", "thier").shuffled(),
                solveSteps = listOf("'Their' has 'heir' in it (people)", "So 'their' belongs to them")
            )
            "Using Semicolons & Colons" -> Lesson(
                title = "Using Semicolons & Colons",
                explanation = "Advanced punctuation allows you to link complex ideas more clearly.",
                steps = listOf(
                    "1. Semicolon (;): Use it to join two related full sentences without a conjunction.",
                    "2. Colon (:): Use it to introduce a list or a significant explanation.",
                    "3. Don't capitalize the word after a semicolon unless it's a proper noun."
                ),
                example = "Call me tomorrow; I will have the answer then.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "I like tea___ he prefers coffee.",
                practiceAnswer = ";",
                practiceOptions = listOf(";", ":", ",", "!").shuffled(),
                solveSteps = listOf("Two independent sentences", "No joining word like 'but'", "Use a semicolon ';'")
            )
            "Subject-Verb Agreement" -> Lesson(
                title = "Subject-Verb Agreement",
                explanation = "The subject and the verb must work together. If the subject is singular, the verb usually needs an 's'.",
                steps = listOf(
                    "1. Identify the subject (who or what).",
                    "2. If there's only one, add 's' to the verb (The cat sits).",
                    "3. If there are many, don't add 's' (The cats sit)."
                ),
                example = "He runs. They run.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "The dogs ___ (bark or barks?)",
                practiceAnswer = "bark",
                practiceOptions = listOf("bark", "barks").shuffled(),
                solveSteps = listOf("Subject: 'dogs' (plural)", "Plural subjects don't need 's' on the verb", "Use 'bark'")
            )
            "Active vs. Passive Voice" -> Lesson(
                title = "Active vs. Passive Voice",
                explanation = "Active voice makes your writing stronger by putting the 'doer' first.",
                steps = listOf(
                    "1. Active: Subject does the action (I ate the pizza).",
                    "2. Passive: The action happens to the subject (The pizza was eaten by me).",
                    "3. Use Active voice whenever possible."
                ),
                example = "Active: The chef cooked the meal.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Which is Active Voice?",
                practiceAnswer = "The boy kicked the ball.",
                practiceOptions = listOf("The boy kicked the ball.", "The ball was kicked by the boy.").shuffled(),
                solveSteps = listOf("In 'The boy kicked the ball', the boy (subject) does the action.")
            )
            "Metaphors and Similes" -> Lesson(
                title = "Metaphors and Similes",
                explanation = "Similes compare things using 'like' or 'as'. Metaphors say one thing IS another.",
                steps = listOf(
                    "1. Simile: 'Cool as a cucumber'.",
                    "2. Metaphor: 'Time is a thief'.",
                    "3. Use these to make your writing more interesting."
                ),
                example = "Simile: Her smile was like sunshine.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Is 'Life is a highway' a Simile or Metaphor?",
                practiceAnswer = "Metaphor",
                practiceOptions = listOf("Simile", "Metaphor").shuffled(),
                solveSteps = listOf("It doesn't use 'like' or 'as'. It says life IS a highway. That's a metaphor.")
            )
            // GRADE K-2 (continued)
            "Exclamation Marks" -> Lesson(
                title = "Exclamation Marks",
                explanation = "An exclamation mark (!) shows strong feeling - excitement, surprise, or a loud warning.",
                steps = listOf(
                    "1. Is the sentence shouted or full of feeling?",
                    "2. Words like Wow, Stop, and Hooray often come with it.",
                    "3. Put the ! at the very end - just one is enough!"
                ),
                example = "Watch out! vs. Watch out.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Finish with feeling: Watch out __",
                practiceAnswer = "!",
                practiceOptions = listOf("!", ".", "?", ",").shuffled(),
                solveSteps = listOf("The sentence is a warning - it needs strong feeling!", "Answer: !")
            )
            "Capitalizing the Word I" -> Lesson(
                title = "Capitalizing the Word I",
                explanation = "The word 'I' (meaning yourself) is ALWAYS capitalized, no matter where it sits in a sentence!",
                steps = listOf(
                    "1. Find the word 'i' that means yourself.",
                    "2. Make it a big I.",
                    "3. Names at the start of a sentence get capitals too."
                ),
                example = "Correct: Sara and I went home. Incorrect: sara and i went home.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Fix it: 'sara and i went home'",
                practiceAnswer = "Sara and I went home",
                practiceOptions = listOf("Sara and I went home", "sara and I went home", "Sara and i went home", "sara and i went home").shuffled(),
                solveSteps = listOf("Capitalize the name: Sara", "Capitalize the word meaning yourself: I")
            )
            "Beginning Blends" -> {
                val pair = listOf(Pair("st", "star"), Pair("br", "brick"), Pair("cl", "clock"), Pair("fr", "frog")).random(random)
                Lesson(
                    title = "Beginning Blends",
                    explanation = "A blend is two consonants whose sounds slide together, like 'st' in 'star'. You can still hear both letters!",
                    steps = listOf(
                        "1. Look at the first two letters.",
                        "2. Say each sound quickly, sliding them together.",
                        "3. Common blends: st, br, cl, fr, sn, tr."
                    ),
                    example = "star = s + t + ar. You hear both the s and the t!",
                    type = LessonType.PUNCTUATION_AND_SPELLING,
                    practiceQuestion = "Which word starts with the '${pair.first}' blend?",
                    practiceAnswer = pair.second,
                    practiceOptions = listOf(pair.second, "apple", "orange", "igloo").shuffled(),
                    solveSteps = listOf("'${pair.second}' starts with ${pair.first}", "Both sounds slide together!")
                )
            }
            "The Magic Long A" -> Lesson(
                title = "The Magic Long A",
                explanation = "A silent e at the end of a word is magic: it makes the vowel say its own name! 'cap' becomes 'cape'.",
                steps = listOf(
                    "1. Find a short-vowel word like 'cap'.",
                    "2. Add a magic e at the end.",
                    "3. The a now says its name: AY. The e stays silent."
                ),
                example = "cap -> cape. mad -> made. tap -> tape.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Add a magic e: what does 'kit' become?",
                practiceAnswer = "kite",
                practiceOptions = listOf("kite", "kit", "kiete", "keet").shuffled(),
                solveSteps = listOf("kit + magic e", "The i says its own name: kite!")
            )

            // GRADE 3-5 (continued)
            "Their, There, They're" -> Lesson(
                title = "Their, There, They're",
                explanation = "These sound the same but mean different things! Their = belonging, There = a place, They're = they are.",
                steps = listOf(
                    "1. Can you replace it with 'they are'? Use they're.",
                    "2. Does it show ownership? Use their.",
                    "3. Otherwise it points to a place: there."
                ),
                example = "They're going over there to get their books.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Fill in: '___ going to the park today.'",
                practiceAnswer = "They're",
                practiceOptions = listOf("They're", "Their", "There", "Theirs").shuffled(),
                solveSteps = listOf("Try 'they are': 'They are going to the park' works!", "So the answer is They're.")
            )
            "Synonyms and Antonyms" -> {
                val pair = listOf(Pair("happy", "joyful"), Pair("fast", "quick"), Pair("big", "huge"), Pair("smart", "clever")).random(random)
                Lesson(
                    title = "Synonyms and Antonyms",
                    explanation = "Synonyms are words that mean almost the same thing. Antonyms mean the opposite!",
                    steps = listOf(
                        "1. Synonym = same: happy and joyful.",
                        "2. Antonym = opposite: happy and sad.",
                        "3. Strong writers pick exact synonyms to paint pictures."
                    ),
                    example = "Synonyms for 'said': whispered, shouted, replied.",
                    type = LessonType.PUNCTUATION_AND_SPELLING,
                    practiceQuestion = "Which word is a SYNONYM for '${pair.first}'?",
                    practiceAnswer = pair.second,
                    practiceOptions = listOf(pair.second, "slow", "tiny", "angry").shuffled(),
                    solveSteps = listOf("'${pair.first}' and '${pair.second}' mean almost the same thing!")
                )
            }
            "Quotation Marks in Dialogue" -> Lesson(
                title = "Quotation Marks in Dialogue",
                explanation = "Quotation marks show the exact words someone said out loud. Put them around the spoken words only!",
                steps = listOf(
                    "1. Find the exact words the person said.",
                    "2. Put an opening quote before the first word.",
                    "3. Put a closing quote after the last word, with punctuation inside."
                ),
                example = "Sam said, \"Let's go!\"",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Add quotes: Sam said, let's go!",
                practiceAnswer = "Sam said, \"let's go!\"",
                practiceOptions = listOf("Sam said, \"let's go!\"", "\"Sam said, let's go!\"", "Sam said, let's go!\"", "Sam said, \"let's go!").shuffled(),
                solveSteps = listOf("Only the spoken words get quotes", "Punctuation goes inside the closing quote")
            )
            "Topic Sentences" -> Lesson(
                title = "Topic Sentences",
                explanation = "A topic sentence is the first sentence of a paragraph. It tells the reader what the whole paragraph will be about!",
                steps = listOf(
                    "1. A topic sentence makes a big, general claim.",
                    "2. The other sentences give details and proof.",
                    "3. Ask: does this sentence sum up the whole paragraph?"
                ),
                example = "Topic: Dogs make wonderful pets. Details: they are loyal, playful, and protective.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Which is the best topic sentence for a paragraph about dolphins?",
                practiceAnswer = "Dolphins are some of the smartest animals in the ocean.",
                practiceOptions = listOf(
                    "Dolphins are some of the smartest animals in the ocean.",
                    "A dolphin ate three fish at lunch.",
                    "The dolphin's fin was gray.",
                    "I saw a dolphin once."
                ).shuffled(),
                solveSteps = listOf("The topic sentence makes the big claim", "The other sentences are small details")
            )

            // MIDDLE SCHOOL+ (continued)
            "Thesis Statements" -> Lesson(
                title = "Thesis Statements",
                explanation = "A thesis statement is the main argument of an essay, usually one sentence at the end of the intro. Everything else proves it!",
                steps = listOf(
                    "1. Take a clear position - no 'maybe' or 'I think'.",
                    "2. Give a hint of your reasons.",
                    "3. Keep it to one strong sentence."
                ),
                example = "School should start later because teens need more sleep and learn better rested.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Which is the strongest thesis statement?",
                practiceAnswer = "Cities should build more bike lanes to cut traffic and pollution.",
                practiceOptions = listOf(
                    "Cities should build more bike lanes to cut traffic and pollution.",
                    "Bikes are fun.",
                    "I like riding my bike sometimes.",
                    "This essay is about bikes."
                ).shuffled(),
                solveSteps = listOf("A thesis takes a position AND gives reasons", "Only one option does both")
            )
            "Comma Splices" -> Lesson(
                title = "Comma Splices",
                explanation = "A comma splice wrongly joins two full sentences with just a comma. Fix it with a period, a conjunction, or a semicolon!",
                steps = listOf(
                    "1. Check each side of the comma: can both stand alone?",
                    "2. If yes, it's a splice - fix it!",
                    "3. Use a period, add 'and/but/so', or use a semicolon."
                ),
                example = "Wrong: I ran fast, I won the race. Right: I ran fast, and I won the race.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "Fix the splice: 'The bell rang, we packed up.'",
                practiceAnswer = "The bell rang, so we packed up.",
                practiceOptions = listOf(
                    "The bell rang, so we packed up.",
                    "The bell rang, we packed up.",
                    "The bell rang we, packed up.",
                    "The bell, rang we packed up."
                ).shuffled(),
                solveSteps = listOf("Both sides are full sentences", "Add the conjunction 'so' to join them properly")
            )
            "Tone vs. Mood" -> Lesson(
                title = "Tone vs. Mood",
                explanation = "Tone is the AUTHOR's attitude. Mood is how the READER feels. Tone creates mood!",
                steps = listOf(
                    "1. Tone = the writer's voice (sarcastic, joyful, serious).",
                    "2. Mood = the feeling you get reading it (gloomy, excited).",
                    "3. Ask: whose feeling is it - the writer's or mine?"
                ),
                example = "A sarcastic tone can create an amused mood.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "The author's attitude toward the subject is called the ___.",
                practiceAnswer = "tone",
                practiceOptions = listOf("tone", "mood", "theme", "plot").shuffled(),
                solveSteps = listOf("Attitude of the author = tone", "Feeling of the reader = mood")
            )
            "Analogies" -> {
                val pair = listOf(Pair("Hot is to cold", "night"), Pair("Bird is to nest", "house"), Pair("Author is to book", "song")).random(random)
                Lesson(
                    title = "Analogies",
                    explanation = "An analogy compares two pairs with the same relationship. Figure out the first pair's link, then apply it!",
                    steps = listOf(
                        "1. Find the relationship in the first pair.",
                        "2. Look for the same relationship in the options.",
                        "3. Say it as a sentence: 'A is to B as C is to ___.'"
                    ),
                    example = "Hot is to cold as day is to night (opposites).",
                    type = LessonType.PUNCTUATION_AND_SPELLING,
                    practiceQuestion = "${pair.first} as ${if (pair.first.startsWith("Hot")) "day" else if (pair.first.startsWith("Bird")) "person" else "musician"} is to ___?",
                    practiceAnswer = pair.second,
                    practiceOptions = listOf(pair.second, "morning", "water", "tree").shuffled(),
                    solveSteps = listOf("Find the link in the first pair", "Apply the same link to the second pair")
                )
            }
            else -> Lesson(
                title = "Context Clues",
                explanation = "When you find a word you don't know, look at the words around it to guess the meaning.",
                steps = listOf(
                    "1. Read the whole sentence.",
                    "2. Look for clues about the mood or situation.",
                    "3. Replace the hard word with a guess and see if it makes sense."
                ),
                example = "The 'gigantic' elephant was taller than the house.",
                type = LessonType.PUNCTUATION_AND_SPELLING,
                practiceQuestion = "In the sentence above, does 'gigantic' mean tiny or very large?",
                practiceAnswer = "very large",
                practiceOptions = listOf("tiny", "very large", "blue", "fast").shuffled(),
                solveSteps = listOf("The clue 'taller than the house' tells us it means very large.")
            )
        }
    }

    private val kPool = listOf(
        Pair("A ___ is red.", "ball"),
        Pair("The ___ is big.", "dog"),
        Pair("I see a ___.", "cat"),
        Pair("My ___ is blue.", "hat"),
        Pair("The ___ is hot.", "sun"),
        Pair("Look at the ___.","moon"),
        Pair("The ___ is green.","leaf"),
        Pair("I like to ___.","read"),
        Pair("The ___ is yellow.","bus"),
        Pair("A ___ can fly.","bird"),
        Pair("The ___ is cold.","snow"),
        Pair("I have two ___.","eyes"),
        Pair("The ___ is on the mat.","cat"),
        Pair("An ___ is for eating.","apple"),
        Pair("The ___ is fast.","car"),
            Pair("Which letter makes the 'sss' sound like a snake?","S"),
            Pair("Which letter does the word 'apple' start with?","A"),
            Pair("Fill in the missing word: I ___ to school every day.","go"),
    )

    private val elementaryPool = listOf(
        Pair("The explorer found a hidden ___ in the cave.", "treasure"),
        Pair("He was so hungry that he ate the ___ pizza.", "entire"),
        Pair("The flowers will ___ in the spring.", "bloom"),
        Pair("The heavy rain caused a ___ in the street.", "puddle"),
        Pair("She decided to ___ her room a new color.", "paint"),
        Pair("The library is a quiet place to ___.", "study"),
        Pair("The squirrel gathered ___ for the winter.","acorns"),
        Pair("A ___ is a large body of salt water.","ocean"),
        Pair("The mountain was very ___ to climb.","steep"),
        Pair("Bees are important because they ___ flowers.","pollinate"),
        Pair("The telescope helps us see ___ stars.","distant"),
        Pair("A caterpillar turns into a ___ inside a cocoon.","butterfly"),
        Pair("The compass helps you find the right ___.","direction"),
        Pair("Electricity flows through a complete ___.","circuit"),
        Pair("Photosynthesis is how plants make ___.","food"),
            Pair("Tom grabbed his umbrella. The sky was dark and full of clouds. What will probably happen next?","It will rain"),
            Pair("The lost puppy wagged its tail and licked the girl's hand. How does the puppy probably feel?","Happy"),
            Pair("Sara read a book about planets, stars, and rockets. What is the book MOSTLY about?","Space"),
            Pair("The enormous elephant blocked the whole road. 'Enormous' means ___.","very big"),
    )

    private val middlePool = listOf(
        Pair("The protagonist's actions were driven by ___.", "ambition"),
        Pair("The author uses ___ to create a gloomy mood.", "imagery"),
        Pair("The scientific method requires a ___ hypothesis.", "testable"),
        Pair("The poem had a very complex ___ scheme.", "rhyme"),
        Pair("Democracy is a system where ___ have a voice.", "citizens"),
        Pair("The metaphor compared his heart to a block of ___.","ice"),
        Pair("A summary should be ___ and cover all main points.","concise"),
        Pair("The conflict in the story was ___ vs. society.","individual"),
        Pair("Persuasive writing aims to ___ the reader.","convince"),
        Pair("The climax is the most ___ part of the plot.","exciting"),
        Pair("An autobiography is a story written by the ___ person.","same"),
        Pair("Alliteration is the repetition of initial ___ sounds.","consonant"),
        Pair("The setting provides the time and ___ of the story.","place"),
        Pair("A narrator tells the story from a specific ___.","perspective"),
        Pair("Context clues help you figure out ___ words.","unknown"),
            Pair("The ancient manuscript was so fragile it crumbled when touched. 'Fragile' means ___.","easily broken"),
            Pair("Her diligent studying paid off with the highest grade. 'Diligent' means ___.","hardworking"),
            Pair("Two friends had a disagreement, but they made up by sharing lunch. What is the theme?","Forgiveness"),
    )

    private val highPool = listOf(
        Pair("The character's hubris led to his inevitable ___.", "downfall"),
        Pair("The philosophical treatise explored the nature of ___.", "existence"),
        Pair("Her argument was logical and ___.", "cogent"),
        Pair("The industrial revolution had a ___ impact on society.", "profound"),
        Pair("The satellite was placed into a ___ orbit.", "geostationary"),
        Pair("The protagonist's epiphany altered his ___ on life.","perspective"),
        Pair("The novel served as a scathing ___ of modern politics.","satire"),
        Pair("Her prose was known for its ___ and clarity.","brevity"),
        Pair("The tectonic plates shift due to ___ currents.","convection"),
        Pair("The legal precedent was established in a ___ case.","landmark"),
        Pair("Stoicism is an ancient Greek ___ of life.","philosophy"),
        Pair("The data showed a significant ___ between the variables.","correlation"),
        Pair("The author's use of allegory conveyed a deeper ___ meaning.","moral"),
        Pair("Quantum mechanics deals with the behavior of ___ particles.","subatomic"),
        Pair("The paradigm shift changed the way scientists ___ the world.","perceive")
    )

    private val punctuationPool = listOf(
        Pair("___Hello!___ she shouted.", "\"Hello!\""),
        Pair("___Where are you going?___ asked Mom.", "\"Where are you going?\""),
        Pair("The teacher said, ___Open your books.___ ", "\"Open your books.\""),
        Pair("___Stop!___ the police officer yelled.", "\"Stop!\""),
        Pair("I like apples___ oranges, and bananas.", ","),
        Pair("It's a beautiful day___ isn't it?", ","),
        Pair("She lives in Paris___ France.", ","),
        Pair("___I'll be back soon,___ he promised.", "\"I'll be back soon,\""),
        Pair("The ___ tail was very long.", "cat's"),
        Pair("___ going to the park today.", "They're"),
        Pair("Don't forget your umbrella___", "!"),
        Pair("Who is coming to the party___", "?"),
        Pair("Please sit down, John___", "."),
        Pair("___Can I help you?___ she asked.", "\"Can I help you?\""),
        Pair("Wow___ That was amazing.", "!"),
            Pair("___Wow, that's a big dog!___ he said.", "\"Wow, that's a big dog!\""),
            Pair("My birthday is on June 5___ 2020.", ","),
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
            gradeLevel <= 1 -> kPool
            gradeLevel <= 3 -> elementaryPool
            gradeLevel <= 5 -> middlePool
            else -> highPool
        }
        
        // Add punctuation for higher floors or higher grades, but less frequent if adjusted
        val punctuationChance = if (isAdjusted) 0.1f else 0.5f
        val finalPool = if (((gradeLevel >= 3 && floor >= 5) || gradeLevel >= 6) && random.nextFloat() < punctuationChance) {
            punctuationPool
        } else pool

        val eligibleQuestions = finalPool.filter { (questionCounts[it.first] ?: 0) < 1 } // FIXED: Limit to 1 repetition
        val selection = if (eligibleQuestions.isNotEmpty()) {
            eligibleQuestions.random(random)
        } else {
            finalPool.random(random)
        }
        
        questionCounts[selection.first] = (questionCounts[selection.first] ?: 0) + 1
        val question = selection.first
        val answer = selection.second

        val options = mutableSetOf(answer)
        
        // Disable deceptive logic if adjusted
        if (!isAdjusted && answer.length > 3) {
            val deceptive = answer.substring(0, answer.length - 1) + (if (answer.endsWith("e")) "a" else "e")
            options.add(deceptive)
        }

        val allPoolAnswers = (kPool + elementaryPool + middlePool + highPool + punctuationPool).map { it.second }
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
