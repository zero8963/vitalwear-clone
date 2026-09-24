package com.example.vitalwearclonev1.game

import kotlin.random.Random

class MathProblemGenerator(private val grade: String, private val floor: Int, private val isAdjusted: Boolean = false) {
    private val random = Random(System.currentTimeMillis())
    private val questionCounts = mutableMapOf<String, Int>()

    fun generateLesson(seenTitles: List<String> = emptyList()): Lesson {
        val gradeLevel = when(grade) {
            "K" -> 0
            else -> grade.toIntOrNull() ?: 1
        }

        val lessons = when {
            gradeLevel <= 1 -> listOf(
                "Introduction to Addition",
                "Understanding Subtraction",
                "Counting On",
                "Doubles Magic",
                "Number Bonds to 10",
                "Comparing Numbers",
                "Adding Three Numbers",
                "Subtraction Word Problems",
                "Skip Counting by 2s",
                "Making 10 to Add"
            )
            gradeLevel <= 3 -> listOf(
                "Learning Multiplication",
                "Place Value",
                "Regrouping (Carrying)",
                "The Power of Zero",
                "Odd and Even Numbers",
                "Basic Division",
                "Rounding to the Nearest Ten",
                "Arrays: Rows and Columns",
                "Multiplying by 10",
                "Division with Remainders"
            )
            gradeLevel <= 5 -> listOf(
                "Long Division Basics",
                "Simplifying Fractions",
                "Decimals to Percentages",
                "Adding Decimals",
                "Multi-digit Multiplication",
                "Factors and Multiples",
                "Equivalent Fractions",
                "Multiplying Fractions by Whole Numbers",
                "Decimal Place Value",
                "Area of Rectangles"
            )
            else -> listOf(
                "Order of Operations (PEMDAS)",
                "Solving for X",
                "Negative Numbers",
                "Exponents (Squares)",
                "Square Roots",
                "Scientific Notation",
                "Ratios and Rates",
                "Percent of a Number",
                "Two-Step Equations",
                "Mean, Median, Mode"
            )
        }

        val unseenLessons = lessons.filter { !seenTitles.contains(it) }
        // Paced learning: work through new skills in a stable, gentle order instead of jumping randomly.
        val selectedTitle = if (unseenLessons.isNotEmpty()) {
            if (isAdjusted) unseenLessons.first() else unseenLessons.random(random)
        } else lessons.random(random)

        return when (selectedTitle) {
            // GRADE K-1
            "Introduction to Addition" -> {
                val n1 = random.nextInt(1, 6)
                val n2 = random.nextInt(1, 5)
                val answer = n1 + n2
                Lesson(
                    title = "Introduction to Addition",
                    explanation = "Addition is joining groups of things together to see how many there are in total.",
                    steps = listOf(
                        "1. Look at the first number: this is your first group.",
                        "2. Look at the second number: this is how many more you are adding.",
                        "3. Count all of them together starting from 1."
                    ),
                    example = "If you have 2 apples and get 3 more, you have 2 + 3 = 5 apples!",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 + $n2 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 1).toString(), (answer - 1).coerceAtLeast(0).toString(), (answer + 2).toString()).shuffled(),
                    solveSteps = listOf("Start with $n1", "Add $n2 more", "Result is $answer")
                )
            }
            "Understanding Subtraction" -> {
                val n1 = random.nextInt(5, 11)
                val n2 = random.nextInt(1, 5)
                val answer = n1 - n2
                Lesson(
                    title = "Understanding Subtraction",
                    explanation = "Subtraction is taking things away from a group to see how many are left.",
                    steps = listOf(
                        "1. Start with the big group (the first number).",
                        "2. Take away the number of things shown by the second number.",
                        "3. Count what is left over."
                    ),
                    example = "If you have 5 cookies and eat 2, you have 5 - 2 = 3 cookies left!",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 - $n2 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 1).toString(), (answer - 2).coerceAtLeast(0).toString(), (answer + 3).toString()).shuffled(),
                    solveSteps = listOf("Start with $n1", "Take away $n2", "Result is $answer")
                )
            }
            "Counting On" -> {
                val n1 = random.nextInt(1, 11)
                val answer = n1 + 2
                Lesson(
                    title = "Counting On",
                    explanation = "Counting on is a shortcut for addition where you start from the bigger number.",
                    steps = listOf(
                        "1. Put the bigger number in your head.",
                        "2. Use your fingers to count up the smaller number.",
                        "3. The last number you say is the answer!"
                    ),
                    example = "To solve 7 + 2, start at 7, then count 8, 9. The answer is 9.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Start at $n1 and count on 2 more. What number is it?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 1).toString(), (answer - 1).toString(), (answer + 3).toString()).shuffled(),
                    solveSteps = listOf("Put $n1 in your head", "Add 1 -> ${n1+1}", "Add 1 more -> $answer")
                )
            }
            "Doubles Magic" -> {
                val n1 = random.nextInt(1, 6)
                val answer = n1 * 2
                Lesson(
                    title = "Doubles Magic",
                    explanation = "Doubles are when you add the same number to itself. It's like looking in a mirror!",
                    steps = listOf(
                        "1. Take a number.",
                        "2. Add the same number again.",
                        "3. This is also called multiplying by 2."
                    ),
                    example = "2 + 2 = 4. 4 + 4 = 8.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "What is double $n1?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 1).toString(), (answer - 1).toString(), (answer + 2).toString()).shuffled(),
                    solveSteps = listOf("$n1 + $n1 = $answer")
                )
            }
            "Number Bonds to 10" -> {
                val n1 = random.nextInt(1, 10)
                val answer = 10 - n1
                Lesson(
                    title = "Number Bonds to 10",
                    explanation = "Number bonds are pairs of numbers that add up to make a bigger number. Knowing which pairs make 10 is super helpful!",
                    steps = listOf(
                        "1. Start with 10.",
                        "2. If you have one number, think how many more you need to reach 10.",
                        "3. Common pairs: 1+9, 2+8, 3+7, 4+6, 5+5."
                    ),
                    example = "If you have 8, you need 2 more to make 10.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "If you have $n1, how many more do you need to make 10?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 1).toString(), (answer - 1).coerceAtLeast(0).toString(), "10").shuffled(),
                    solveSteps = listOf("10 - $n1 = $answer")
                )
            }
            "Comparing Numbers" -> {
                val n1 = random.nextInt(1, 10)
                val n2 = random.nextInt(1, 10).let { if (it == n1) it + 1 else it }
                val answer = if (n1 > n2) "Greater" else "Smaller"
                Lesson(
                    title = "Comparing Numbers",
                    explanation = "We use 'greater than' (>) and 'less than' (<) to show which number is bigger.",
                    steps = listOf(
                        "1. Look at both numbers.",
                        "2. The 'hungry alligator' (>) always wants to eat the bigger number.",
                        "3. If they are the same, use equals (=)."
                    ),
                    example = "5 is greater than 3 (5 > 3).",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Is $n1 Greater or Smaller than $n2?",
                    practiceAnswer = answer,
                    practiceOptions = listOf("Greater", "Smaller", "Equal").shuffled(),
                    solveSteps = listOf("Look at $n1 and $n2", "The bigger one is ${Math.max(n1, n2)}")
                )
            }

            // GRADE 2-3
            "Learning Multiplication" -> {
                val n1 = random.nextInt(2, 6)
                val n2 = random.nextInt(2, 6)
                val answer = n1 * n2
                Lesson(
                    title = "Learning Multiplication",
                    explanation = "Multiplication is a fast way to do special addition where the numbers are all the same.",
                    steps = listOf(
                        "1. Think of $n1 x $n2 as '$n1 groups of $n2'.",
                        "2. You can add $n2, $n1 times.",
                        "3. The result is the same!"
                    ),
                    example = "3 x 4 means you have 3 boxes, and each box has 4 toys. Total is 12.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 x $n2 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + n2).toString(), (answer - n1).toString(), (answer + 5).toString()).shuffled(),
                    solveSteps = listOf("Add $n2, $n1 times", (1..n1).joinToString(" + ") { n2.toString() } + " = $answer")
                )
            }
            "Place Value" -> {
                val n1 = random.nextInt(20, 100)
                val tens = n1 / 10
                Lesson(
                    title = "Place Value",
                    explanation = "Digits have different values depending on where they are in a number.",
                    steps = listOf(
                        "1. Ones place is on the far right.",
                        "2. Tens place is to the left of the ones.",
                        "3. Hundreds place is to the left of the tens."
                    ),
                    example = "In the number 42, the 4 is 40 (tens) and the 2 is 2 (ones).",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "In the number $n1, what digit is in the tens place?",
                    practiceAnswer = tens.toString(),
                    practiceOptions = listOf(tens.toString(), (n1 % 10).toString(), "1", "0").shuffled(),
                    solveSteps = listOf("$n1 = ${tens * 10} + ${n1 % 10}", "The tens digit is $tens")
                )
            }
            "Regrouping (Carrying)" -> {
                val n1 = random.nextInt(10, 40)
                val n2 = random.nextInt(10, 40)
                val answer = n1 + n2
                Lesson(
                    title = "Regrouping (Carrying)",
                    explanation = "When adding, if a column adds up to more than 9, you 'carry' the tens to the next column.",
                    steps = listOf(
                        "1. Add the ones column first.",
                        "2. If it's 10 or more, write the ones digit and carry the ten.",
                        "3. Add the tens column, including the number you carried."
                    ),
                    example = "18 + 5: 8+5=13. Write 3, carry 1. 1+1=2. Answer: 23.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 + $n2 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 10).toString(), (answer - 5).toString(), (answer + 7).toString()).shuffled(),
                    solveSteps = listOf("Ones: ${n1%10} + ${n2%10} = ${(n1%10)+(n2%10)}", "Tens: ${n1/10} + ${n2/10} + (carry if needed)", "Total: $answer")
                )
            }
            "The Power of Zero" -> {
                val n1 = random.nextInt(11, 20)
                Lesson(
                    title = "The Power of Zero",
                    explanation = "Zero is a special number! When you multiply any number by zero, the answer is always zero.",
                    steps = listOf(
                        "1. Take any number, no matter how big.",
                        "2. Multiply it by 0.",
                        "3. The result is always 0."
                    ),
                    example = "1,000,000 x 0 = 0.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 x 0 = ?",
                    practiceAnswer = "0",
                    practiceOptions = listOf("0", n1.toString(), "1", "10").shuffled(),
                    solveSteps = listOf("Anything times zero is zero", "So $n1 x 0 = 0")
                )
            }
            "Odd and Even Numbers" -> {
                val n1 = random.nextInt(1, 21)
                val answer = if (n1 % 2 == 0) "Even" else "Odd"
                Lesson(
                    title = "Odd and Even Numbers",
                    explanation = "Even numbers can be split into two equal groups. Odd numbers always have one left over.",
                    steps = listOf(
                        "1. Look at the last digit.",
                        "2. Even numbers end in 0, 2, 4, 6, or 8.",
                        "3. Odd numbers end in 1, 3, 5, 7, or 9."
                    ),
                    example = "4 is even (2+2). 5 is odd (2+2+1).",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Is the number $n1 Odd or Even?",
                    practiceAnswer = answer,
                    practiceOptions = listOf("Odd", "Even").shuffled(),
                    solveSteps = listOf("$n1 ÷ 2 = ${n1/2} with remainder ${n1%2}")
                )
            }
            "Basic Division" -> {
                val n2 = random.nextInt(2, 6)
                val answer = random.nextInt(2, 6)
                val n1 = n2 * answer
                Lesson(
                    title = "Basic Division",
                    explanation = "Division is splitting a large group into smaller, equal groups. It's the opposite of multiplication.",
                    steps = listOf(
                        "1. Look at the total number.",
                        "2. Think about how many equal groups you want.",
                        "3. Each group must have the same amount."
                    ),
                    example = "6 cookies shared by 2 friends means 3 cookies each (6 ÷ 2 = 3).",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 ÷ $n2 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 1).toString(), (n1 - n2).toString(), "2").shuffled(),
                    solveSteps = listOf("How many $n2's fit into $n1?", "$answer x $n2 = $n1", "So $n1 ÷ $n2 = $answer")
                )
            }

            // GRADE 4-5
            "Long Division Basics" -> {
                val n2 = random.nextInt(2, 6)
                val n1 = n2 * random.nextInt(2, 10)
                val answer = n1 / n2
                Lesson(
                    title = "Long Division Basics",
                    explanation = "Division is splitting a large group into smaller, equal groups.",
                    steps = listOf(
                        "1. Divide: How many times does the divisor go into the digit?",
                        "2. Multiply: Multiply that number by the divisor.",
                        "3. Subtract: Find the difference.",
                        "4. Bring Down: Bring down the next digit and repeat."
                    ),
                    example = "12 ÷ 3 = 4 because 4 + 4 + 4 = 12.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 ÷ $n2 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 1).toString(), (answer * 2).toString(), (answer - 1).coerceAtLeast(1).toString()).shuffled(),
                    solveSteps = listOf("How many $n2 in $n1?", "$answer x $n2 = $n1", "Result is $answer")
                )
            }
            "Simplifying Fractions" -> {
                Lesson(
                    title = "Simplifying Fractions",
                    explanation = "Fractions can be written with smaller numbers if you divide the top and bottom by the same number.",
                    steps = listOf(
                        "1. Find a number that divides into both the numerator (top) and denominator (bottom).",
                        "2. Divide both by that number.",
                        "3. Repeat until they can't be divided anymore."
                    ),
                    example = "4/8 can be simplified to 1/2 by dividing both by 4.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Simplify 6/12. What is the top number (numerator)?",
                    practiceAnswer = "1",
                    practiceOptions = listOf("1", "2", "3", "6").shuffled(),
                    solveSteps = listOf("6 and 12 are both divisible by 6", "6 ÷ 6 = 1", "12 ÷ 6 = 2", "Result: 1/2")
                )
            }
            "Decimals to Percentages" -> {
                val n1 = random.nextInt(1, 10)
                Lesson(
                    title = "Decimals to Percentages",
                    explanation = "To turn a decimal into a percentage, move the decimal point two places to the right.",
                    steps = listOf(
                        "1. Look at the decimal number.",
                        "2. Move the dot two spots to the right.",
                        "3. Add the % sign."
                    ),
                    example = "0.50 becomes 50%. 0.05 becomes 5%.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "What is 0.$n1 as a percentage? (Just the number)",
                    practiceAnswer = (n1 * 10).toString(),
                    practiceOptions = listOf((n1 * 10).toString(), n1.toString(), "0.$n1", "100").shuffled(),
                    solveSteps = listOf("Move dot 1 place -> $n1.0", "Move dot again -> ${n1}0.0", "Result: ${n1}0%")
                )
            }
            "Adding Decimals" -> {
                Lesson(
                    title = "Adding Decimals",
                    explanation = "When adding decimals, the most important thing is to line up the decimal points!",
                    steps = listOf(
                        "1. Write the numbers vertically.",
                        "2. Line up the dots (decimal points).",
                        "3. Add like normal numbers."
                    ),
                    example = "1.5 + 0.2 = 1.7.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "0.25 + 0.25 = ?",
                    practiceAnswer = "0.5",
                    practiceOptions = listOf("0.5", "0.50", "0.25", "0.05").shuffled(),
                    solveSteps = listOf("0.25", "+ 0.25", "------", "0.50")
                )
            }
            "Multi-digit Multiplication" -> {
                val n1 = random.nextInt(10, 21)
                val n2 = random.nextInt(3, 6)
                val answer = n1 * n2
                Lesson(
                    title = "Multi-digit Multiplication",
                    explanation = "When multiplying bigger numbers, we multiply one digit at a time and then add.",
                    steps = listOf(
                        "1. Multiply the top number by the ones digit of the bottom number.",
                        "2. If there's a second digit, add a zero as a placeholder.",
                        "3. Multiply and then add the results together."
                    ),
                    example = "12 x 3: 2x3=6, 1x3=3. Result: 36.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 x $n2 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 10).toString(), (answer - n1).toString(), "50").shuffled(),
                    solveSteps = listOf("${n1/10}0 x $n2 = ${(n1/10)*10*n2}", "${n1%10} x $n2 = ${(n1%10)*n2}", "Add: ${(n1/10)*10*n2} + ${(n1%10)*n2} = $answer")
                )
            }
            "Factors and Multiples" -> {
                val n1 = listOf(4, 6, 8, 9, 10).random(random)
                Lesson(
                    title = "Factors and Multiples",
                    explanation = "Factors are numbers you multiply to get another number. Multiples are the results of multiplying a number.",
                    steps = listOf(
                        "1. Factors of 6: 1, 2, 3, 6.",
                        "2. Multiples of 6: 6, 12, 18, 24...",
                        "3. Every number has at least two factors (1 and itself)."
                    ),
                    example = "Multiples are like skip counting!",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Which of these is a FACTOR of $n1?",
                    practiceAnswer = "2",
                    practiceOptions = listOf("2", (n1 + 1).toString(), (n1 * 2).toString(), "0").shuffled(),
                    solveSteps = listOf("$n1 ÷ 2 = ${n1/2}", "Since there is no remainder, 2 is a factor.")
                )
            }

            // MIDDLE SCHOOL+
            "Order of Operations (PEMDAS)" -> {
                val n1 = random.nextInt(2, 10)
                val n2 = random.nextInt(2, 10)
                val n3 = random.nextInt(2, 10)
                val answer = n1 + n2 * n3
                Lesson(
                    title = "Order of Operations (PEMDAS)",
                    explanation = "When a problem has many parts, we follow a specific order so everyone gets the same answer.",
                    steps = listOf(
                        "1. Parentheses: Solve anything inside brackets first.",
                        "2. Exponents: Solve powers (like 2²).",
                        "3. Multiplication & Division: Solve from left to right.",
                        "4. Addition & Subtraction: Solve from left to right."
                    ),
                    example = "In 2 + 3 x 4, you multiply first! 2 + 12 = 14.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 + $n2 x $n3 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), ((n1 + n2) * n3).toString(), (n1 + n2 + n3).toString(), (n1 * n2 * n3).toString()).shuffled(),
                    solveSteps = listOf("Multiply: $n2 x $n3 = ${n2*n3}", "Add: $n1 + ${n2*n3} = $answer")
                )
            }
            "Solving for X" -> {
                val n1 = random.nextInt(2, 6)
                val answer = 10 - n1
                Lesson(
                    title = "Solving for X",
                    explanation = "Algebra is like a puzzle where we try to find the missing number, usually called 'x'.",
                    steps = listOf(
                        "1. Get 'x' by itself on one side of the equals sign.",
                        "2. Do the opposite of whatever is happening to 'x' (if it's +5, you -5).",
                        "3. Whatever you do to one side, you MUST do to the other."
                    ),
                    example = "x + 5 = 10. Subtract 5 from both sides: x = 5.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "If x + $n1 = 10, what is x?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (10 + n1).toString(), n1.toString(), "0").shuffled(),
                    solveSteps = listOf("x + $n1 = 10", "Subtract $n1 from both sides", "x = 10 - $n1", "x = $answer")
                )
            }
            "Negative Numbers" -> {
                Lesson(
                    title = "Negative Numbers",
                    explanation = "Negative numbers are numbers less than zero, like temperatures below freezing.",
                    steps = listOf(
                        "1. Adding a negative is the same as subtracting.",
                        "2. Two negatives make a positive when multiplying.",
                        "3. Subtracting a negative is like adding."
                    ),
                    example = "5 + (-3) = 2.  -2 x -3 = 6.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "5 - (-2) = ?",
                    practiceAnswer = "7",
                    practiceOptions = listOf("7", "3", "-7", "-3").shuffled(),
                    solveSteps = listOf("5 - (-2)", "Minus minus becomes plus", "5 + 2 = 7")
                )
            }
            "Exponents (Squares)" -> {
                Lesson(
                    title = "Exponents (Squares)",
                    explanation = "An exponent tells you how many times to multiply a number by itself.",
                    steps = listOf(
                        "1. Look at the base (the big number).",
                        "2. Look at the exponent (the small number).",
                        "3. Multiply the base by itself that many times."
                    ),
                    example = "3² = 3 x 3 = 9.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "4² = ?",
                    practiceAnswer = "16",
                    practiceOptions = listOf("16", "8", "4", "32").shuffled(),
                    solveSteps = listOf("4² means 4 x 4", "4 x 4 = 16")
                )
            }
            "Square Roots" -> {
                Lesson(
                    title = "Square Roots",
                    explanation = "A square root is the opposite of squaring a number. It asks: 'What number times itself equals this?'",
                    steps = listOf(
                        "1. Look at the number inside the √ symbol.",
                        "2. Find a number that, when multiplied by itself, gives that number.",
                        "3. Example: √9 = 3 because 3x3=9."
                    ),
                    example = "√16 = 4. √25 = 5.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "What is √36?",
                    practiceAnswer = "6",
                    practiceOptions = listOf("6", "18", "12", "9").shuffled(),
                    solveSteps = listOf("6 x 6 = 36", "So √36 = 6")
                )
            }
            // GRADE K-1 (continued)
            "Adding Three Numbers" -> {
                val n1 = random.nextInt(1, 4)
                val n2 = random.nextInt(1, 4)
                val n3 = random.nextInt(1, 4)
                val answer = n1 + n2 + n3
                Lesson(
                    title = "Adding Three Numbers",
                    explanation = "You can add three numbers by adding the first two, then adding the third to that answer.",
                    steps = listOf(
                        "1. Add the first two numbers together.",
                        "2. Take that answer and add the third number.",
                        "3. It helps to find pairs that make 10 first!"
                    ),
                    example = "2 + 3 + 4: First 2 + 3 = 5, then 5 + 4 = 9.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 + $n2 + $n3 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 1).toString(), (answer - 1).coerceAtLeast(0).toString(), (answer + 2).toString()).shuffled(),
                    solveSteps = listOf("$n1 + $n2 = ${n1+n2}", "${n1+n2} + $n3 = $answer")
                )
            }
            "Subtraction Word Problems" -> {
                val n1 = random.nextInt(6, 11)
                val n2 = random.nextInt(1, 6)
                val answer = n1 - n2
                val items = listOf("apples", "stickers", "toy cars", "crayons").random(random)
                Lesson(
                    title = "Subtraction Word Problems",
                    explanation = "Word problems tell a little story. Words like 'gave away', 'lost', or 'left' mean subtract!",
                    steps = listOf(
                        "1. Read the story carefully.",
                        "2. Find the starting number and the number taken away.",
                        "3. Write it as a subtraction problem and solve."
                    ),
                    example = "Mia had 7 balloons. 2 flew away. 7 - 2 = 5 left!",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Sam had $n1 $items. He gave $n2 to a friend. How many are left?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 1).toString(), (n1 + n2).toString(), (answer + 2).toString()).shuffled(),
                    solveSteps = listOf("Start: $n1", "Take away: $n2", "$n1 - $n2 = $answer")
                )
            }
            "Skip Counting by 2s" -> {
                val n1 = random.nextInt(1, 6) * 2
                val answer = n1 + 4
                Lesson(
                    title = "Skip Counting by 2s",
                    explanation = "Skip counting by 2s means counting every other number: 2, 4, 6, 8... It makes counting big groups faster!",
                    steps = listOf(
                        "1. Start at your number.",
                        "2. Add 2 to get the next number.",
                        "3. Keep adding 2 each time."
                    ),
                    example = "Counting socks: 2, 4, 6, 8 - that's 4 pairs!",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Skip count by 2s: $n1, ${n1+2}, __?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 1).toString(), (answer + 2).toString(), (n1 + 3).toString()).shuffled(),
                    solveSteps = listOf("$n1 + 2 = ${n1+2}", "${n1+2} + 2 = $answer")
                )
            }
            "Making 10 to Add" -> {
                val n1 = random.nextInt(6, 10)
                val n2 = random.nextInt(11 - n1, 10)
                val answer = n1 + n2
                val need = 10 - n1
                Lesson(
                    title = "Making 10 to Add",
                    explanation = "10 is a friendly number! Break the second number apart so the first number can become 10.",
                    steps = listOf(
                        "1. See how many the first number needs to reach 10.",
                        "2. Split the second number into two parts.",
                        "3. Give one part to make 10, then add what's left."
                    ),
                    example = "8 + 5: 8 needs 2 to make 10. Split 5 into 2 + 3. 10 + 3 = 13!",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Use make-10: $n1 + $n2 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), "10", (answer - 1).toString(), (answer + 1).toString()).shuffled(),
                    solveSteps = listOf("$n1 needs $need to make 10", "Split $n2 into $need + ${n2-need}", "10 + ${n2-need} = $answer")
                )
            }

            // GRADE 2-3 (continued)
            "Rounding to the Nearest Ten" -> {
                val n1 = random.nextInt(11, 100)
                val answer = ((n1 + 5) / 10) * 10
                Lesson(
                    title = "Rounding to the Nearest Ten",
                    explanation = "Rounding makes numbers simpler. Look at the ones digit: 4 or less rounds down, 5 or more rounds up!",
                    steps = listOf(
                        "1. Find the tens digit.",
                        "2. Look at the ones digit next to it.",
                        "3. If it's 0-4, keep the tens the same. If it's 5-9, go up one ten."
                    ),
                    example = "47 rounds to 50 because 7 is 5 or more. 42 rounds to 40.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Round $n1 to the nearest ten.",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (answer + 10).toString(), (answer - 10).coerceAtLeast(0).toString(), n1.toString()).shuffled(),
                    solveSteps = listOf("Ones digit is ${n1 % 10}", if (n1 % 10 >= 5) "5 or more: round UP" else "4 or less: round DOWN", "Answer: $answer")
                )
            }
            "Arrays: Rows and Columns" -> {
                val rows = random.nextInt(2, 5)
                val cols = random.nextInt(2, 5)
                val answer = rows * cols
                Lesson(
                    title = "Arrays: Rows and Columns",
                    explanation = "An array shows multiplication as neat rows and columns. Count one row, count the rows, then multiply!",
                    steps = listOf(
                        "1. Count how many items are in one row.",
                        "2. Count how many rows there are.",
                        "3. Multiply: rows x items-per-row."
                    ),
                    example = "3 rows of 4 stars = 3 x 4 = 12 stars.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "An array has $rows rows with $cols dots in each row. How many dots total?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), (rows + cols).toString(), (answer + rows).toString(), (answer - 1).toString()).shuffled(),
                    solveSteps = listOf("$rows rows", "$cols per row", "$rows x $cols = $answer")
                )
            }
            "Multiplying by 10" -> {
                val n1 = random.nextInt(2, 13)
                val answer = n1 * 10
                Lesson(
                    title = "Multiplying by 10",
                    explanation = "Multiplying by 10 is easy: just add a zero to the end of the number!",
                    steps = listOf(
                        "1. Take the number.",
                        "2. Write a 0 at the end.",
                        "3. That's your answer!"
                    ),
                    example = "7 x 10 = 70. 12 x 10 = 120.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 x 10 = ?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), n1.toString(), (answer + 10).toString(), (answer * 10).toString()).shuffled(),
                    solveSteps = listOf("Write $n1", "Add a zero at the end", "Answer: $answer")
                )
            }
            "Division with Remainders" -> {
                val n2 = random.nextInt(2, 6)
                val q = random.nextInt(2, 6)
                val r = random.nextInt(1, n2)
                val n1 = n2 * q + r
                Lesson(
                    title = "Division with Remainders",
                    explanation = "Sometimes things don't split evenly. What's left over is called the remainder!",
                    steps = listOf(
                        "1. Find the biggest multiple of the divisor that fits.",
                        "2. See how much is left over - that's the remainder.",
                        "3. Write it as: answer R remainder."
                    ),
                    example = "13 ÷ 4 = 3 R 1, because 3 x 4 = 12 and 1 is left over.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "$n1 ÷ $n2 = ? (Give the remainder)",
                    practiceAnswer = r.toString(),
                    practiceOptions = listOf(r.toString(), q.toString(), "0", (r + 1).toString()).shuffled(),
                    solveSteps = listOf("$q x $n2 = ${n2*q}", "$n1 - ${n2*q} = $r left over", "Remainder is $r")
                )
            }

            // GRADE 4-5 (continued)
            "Equivalent Fractions" -> {
                val mult = random.nextInt(2, 5)
                Lesson(
                    title = "Equivalent Fractions",
                    explanation = "Equivalent fractions look different but mean the same amount. Multiply top and bottom by the same number!",
                    steps = listOf(
                        "1. Pick a number to multiply by.",
                        "2. Multiply the numerator (top) by it.",
                        "3. Multiply the denominator (bottom) by the same number."
                    ),
                    example = "1/2 = 2/4 = 3/6. They all show half!",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Which fraction equals 1/2?",
                    practiceAnswer = "$mult/${mult*2}",
                    practiceOptions = listOf("$mult/${mult*2}", "1/3", "$mult/${mult*3}", "2/3").shuffled(),
                    solveSteps = listOf("Multiply 1 x $mult = $mult", "Multiply 2 x $mult = ${mult*2}", "So 1/2 = $mult/${mult*2}")
                )
            }
            "Multiplying Fractions by Whole Numbers" -> {
                val n1 = random.nextInt(2, 5)
                val denom = listOf(2, 3, 4).random(random)
                Lesson(
                    title = "Multiplying Fractions by Whole Numbers",
                    explanation = "To multiply a whole number by a fraction, multiply the whole number by the top (numerator) and keep the bottom!",
                    steps = listOf(
                        "1. Multiply the whole number by the numerator.",
                        "2. Keep the same denominator.",
                        "3. Simplify if you can."
                    ),
                    example = "3 x 1/4 = 3/4.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "What is $n1 x 1/$denom?",
                    practiceAnswer = "$n1/$denom",
                    practiceOptions = listOf("$n1/$denom", "${n1*denom}/$denom", "1/$denom", "$n1/${denom*2}").shuffled(),
                    solveSteps = listOf("$n1 x 1 = $n1", "Keep the denominator $denom", "Answer: $n1/$denom")
                )
            }
            "Decimal Place Value" -> {
                val tenths = random.nextInt(1, 10)
                val hundredths = random.nextInt(0, 10)
                val num = "3.$tenths$hundredths"
                Lesson(
                    title = "Decimal Place Value",
                    explanation = "After the decimal point, the first digit is tenths and the second is hundredths - like small slices of one whole!",
                    steps = listOf(
                        "1. The first digit after the dot is the tenths place.",
                        "2. The second digit after the dot is the hundredths place.",
                        "3. Each place is ten times smaller than the one before it."
                    ),
                    example = "In 2.45, 4 is in the tenths place and 5 is in the hundredths place.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "In the number $num, which digit is in the hundredths place?",
                    practiceAnswer = hundredths.toString(),
                    practiceOptions = listOf(hundredths.toString(), tenths.toString(), "3", "0").shuffled(),
                    solveSteps = listOf("$num: 3 is ones", "$tenths is tenths", "$hundredths is hundredths")
                )
            }
            "Area of Rectangles" -> {
                val w = random.nextInt(3, 10)
                val h = random.nextInt(3, 10)
                val answer = w * h
                Lesson(
                    title = "Area of Rectangles",
                    explanation = "Area is how much space a flat shape covers. For rectangles: Area = length x width!",
                    steps = listOf(
                        "1. Measure the length (long side).",
                        "2. Measure the width (short side).",
                        "3. Multiply them together."
                    ),
                    example = "A 5 by 3 rug covers 15 square units.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "What is the area of a ${w}x${h} rectangle?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), ((w+h)*2).toString(), (w+h).toString(), (answer + w).toString()).shuffled(),
                    solveSteps = listOf("Area = length x width", "$w x $h = $answer", "Answer: $answer square units")
                )
            }

            // MIDDLE SCHOOL+ (continued)
            "Ratios and Rates" -> {
                val a = random.nextInt(2, 6)
                val b = a * random.nextInt(2, 5)
                val simplified = b / a
                Lesson(
                    title = "Ratios and Rates",
                    explanation = "A ratio compares two amounts. Simplify it just like a fraction - divide both sides by the same number!",
                    steps = listOf(
                        "1. Write the ratio with a colon, like 4:8.",
                        "2. Find the biggest number that divides both.",
                        "3. Divide both sides to simplify."
                    ),
                    example = "6:9 simplifies to 2:3.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "Simplify the ratio $a:$b. What is the second number?",
                    practiceAnswer = simplified.toString(),
                    practiceOptions = listOf(simplified.toString(), b.toString(), a.toString(), (simplified + 1).toString()).shuffled(),
                    solveSteps = listOf("Both divide by $a", "$a ÷ $a = 1", "$b ÷ $a = $simplified", "Simplified: 1:$simplified")
                )
            }
            "Percent of a Number" -> {
                val n1 = random.nextInt(2, 11) * 10
                val answer = n1 / 10
                Lesson(
                    title = "Percent of a Number",
                    explanation = "10% means 10 out of every 100. To find 10% of a number, just divide it by 10!",
                    steps = listOf(
                        "1. Remember: 10% = 10/100 = 1/10.",
                        "2. Divide the number by 10.",
                        "3. That's 10 percent!"
                    ),
                    example = "10% of 50 = 5.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "What is 10% of $n1?",
                    practiceAnswer = answer.toString(),
                    practiceOptions = listOf(answer.toString(), n1.toString(), (answer * 2).toString(), "10").shuffled(),
                    solveSteps = listOf("10% means divide by 10", "$n1 ÷ 10 = $answer")
                )
            }
            "Two-Step Equations" -> {
                val x = random.nextInt(2, 10)
                val b = random.nextInt(1, 6)
                val c = 2 * x + b
                Lesson(
                    title = "Two-Step Equations",
                    explanation = "Two-step equations need two moves. Undo addition/subtraction first, then undo multiplication!",
                    steps = listOf(
                        "1. Undo the + or - by doing the opposite to both sides.",
                        "2. Undo the x2 (or ÷) by doing the opposite to both sides.",
                        "3. Check your answer by plugging it back in."
                    ),
                    example = "2x + 3 = 11: subtract 3 (2x = 8), then divide by 2 (x = 4).",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "If 2x + $b = $c, what is x?",
                    practiceAnswer = x.toString(),
                    practiceOptions = listOf(x.toString(), (x + 1).toString(), c.toString(), (2 * x).toString()).shuffled(),
                    solveSteps = listOf("2x + $b = $c", "Subtract $b: 2x = ${c-b}", "Divide by 2: x = $x")
                )
            }
            "Mean, Median, Mode" -> {
                val m = random.nextInt(4, 10)
                val a = m - 1
                val b = m + 1
                Lesson(
                    title = "Mean, Median, Mode",
                    explanation = "Mean is the average! Add all the numbers up, then divide by how many numbers there are.",
                    steps = listOf(
                        "1. Add all the numbers together.",
                        "2. Count how many numbers there are.",
                        "3. Divide the total by that count."
                    ),
                    example = "Mean of 2, 4, 6: (2+4+6) ÷ 3 = 12 ÷ 3 = 4.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "What is the mean (average) of $a, $m, $b?",
                    practiceAnswer = m.toString(),
                    practiceOptions = listOf(m.toString(), (m + 1).toString(), (a + b + m).toString(), (m - 1).toString()).shuffled(),
                    solveSteps = listOf("Add: $a + $m + $b = ${a+m+b}", "Count: 3 numbers", "Divide: ${a+m+b} ÷ 3 = $m")
                )
            }
            else -> {
                Lesson(
                    title = "Scientific Notation",
                    explanation = "Scientific notation is a way to write very large or very small numbers using powers of 10.",
                    steps = listOf(
                        "1. Move the decimal point so only one non-zero digit is on the left.",
                        "2. Count how many spaces you moved.",
                        "3. Write it as: Number x 10^(spaces moved)."
                    ),
                    example = "5,000 is 5 x 10³.",
                    type = LessonType.HOW_TO_SOLVE,
                    practiceQuestion = "How do you write 300 in scientific notation?",
                    practiceAnswer = "3 x 10²",
                    practiceOptions = listOf("3 x 10²", "3 x 10³", "30 x 10¹", "0.3 x 10³").shuffled(),
                    solveSteps = listOf("300.0 -> Move dot 2 places left", "3.0 x 10²")
                )
            }
        }
    }

    fun generate(): Problem {
        var problem: Problem
        var attempts = 0
        do {
            problem = generateInternal()
            attempts++
        } while ((questionCounts[problem.question] ?: 0) >= 2 && attempts < 15)
        
        questionCounts[problem.question] = (questionCounts[problem.question] ?: 0) + 1
        return problem
    }

    private fun generateInternal(): Problem {
        val difficulty = if (isAdjusted) (floor - 1) / 8 else (floor - 1) / 5 
        
        var gradeLevel = when(grade) {
            "K" -> 0
            else -> grade.toIntOrNull() ?: 1
        }
        
        if (isAdjusted) {
            gradeLevel = (gradeLevel - 2).coerceAtLeast(0)
        }

        // Procedural variety templates (30%): word problems, money, time, fractions, percents.
        if (random.nextFloat() < 0.30f) {
            return when {
                gradeLevel <= 1 -> if (random.nextBoolean()) missingAddendProblem(gradeLevel, difficulty)
                                   else moneyProblem(difficulty)
                gradeLevel <= 3 -> when (random.nextInt(3)) {
                    0 -> missingAddendProblem(gradeLevel, difficulty)
                    1 -> timeProblem()
                    else -> fractionOfSetProblem(difficulty)
                }
                gradeLevel <= 5 -> if (random.nextBoolean()) fractionOfSetProblem(difficulty)
                                   else percentProblem(difficulty)
                else -> percentProblem(difficulty)
            }
        }

        val operator: String
        val num1: Int
        val num2: Int

        when {
            gradeLevel <= 1 -> { // Kindergarten & 1st Grade
                operator = if (random.nextBoolean() || gradeLevel == 0) "+" else "-"
                num1 = random.nextInt(1, 10 + difficulty * 3)
                num2 = random.nextInt(1, 10)
            }
            gradeLevel <= 3 -> { // 2nd & 3rd Grade
                operator = if (isAdjusted) listOf("+", "-").random() else listOf("+", "-", "*").random()
                if (operator == "*") {
                    num1 = random.nextInt(1, 10 + difficulty)
                    num2 = random.nextInt(1, 5 + difficulty)
                } else {
                    num1 = random.nextInt(10, 50 + difficulty * 15)
                    num2 = random.nextInt(10, 30)
                }
            }
            gradeLevel <= 5 -> { // 4th & 5th Grade
                operator = if (isAdjusted) listOf("+", "-", "*").random() else listOf("+", "-", "*", "/").random()
                when (operator) {
                    "*" -> {
                        num1 = random.nextInt(5, 15 + difficulty * 3)
                        num2 = random.nextInt(2, 10)
                    }
                    "/" -> {
                        num2 = random.nextInt(2, 10)
                        num1 = num2 * random.nextInt(2, 10 + difficulty)
                    }
                    else -> {
                        num1 = random.nextInt(30, 100 + difficulty * 50)
                        num2 = random.nextInt(20, 80)
                    }
                }
            }
            gradeLevel <= 8 -> { // Middle School (6-8)
                operator = if (isAdjusted) listOf("+", "-", "*", "/").random() else listOf("+", "-", "*", "/").random()
                when (operator) {
                    "*" -> {
                        num1 = random.nextInt(10, 40 + difficulty * 5)
                        num2 = random.nextInt(2, 15)
                    }
                    "/" -> {
                        num2 = random.nextInt(5, 20)
                        num1 = num2 * random.nextInt(5, 20 + difficulty)
                    }
                    else -> {
                        num1 = random.nextInt(50, 500 + difficulty * 200)
                        num2 = random.nextInt(50, 500)
                    }
                }
            }
            else -> { // High School (9-12)
                operator = listOf("+", "-", "*", "/").random()
                num1 = random.nextInt(20, 100 + difficulty * 30)
                num2 = random.nextInt(10, 50 + difficulty * 10)
            }
        }

        val answer = when (operator) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            "*" -> num1 * num2
            "/" -> num1 / num2
            else -> num1 + num2
        }

        val options = mutableSetOf(answer)
        while (options.size < 4) {
            val variance = if (answer == 0) 10 else Math.abs(answer) / (if (isAdjusted) 2 else 4) + 5
            val offset = random.nextInt(-variance, variance)
            if (offset != 0) options.add(answer + offset)
        }

        return Problem(
            question = "$num1 $operator $num2 = ?",
            correctAnswer = answer.toString(),
            options = options.toList().shuffled().map { it.toString() }
        )
    }

    // Missing addend: "7 + ? = 12" (K-3)
    private fun missingAddendProblem(gradeLevel: Int, difficulty: Int): Problem {
        val max = if (gradeLevel <= 1) 10 + difficulty * 2 else 20 + difficulty * 5
        val total = random.nextInt(6, max + 1)
        val n1 = random.nextInt(1, total)
        val answer = total - n1
        val options = mutableSetOf(answer)
        while (options.size < 4) {
            val d = random.nextInt(-3, 4)
            if (d != 0 && answer + d >= 0) options.add(answer + d)
        }
        return Problem(
            question = "$n1 + ? = $total",
            correctAnswer = answer.toString(),
            options = options.toList().shuffled().map { it.toString() }
        )
    }

    // Counting money in cents (K-2; quarters unlock at higher floors)
    private fun moneyProblem(difficulty: Int): Problem {
        val coins = mutableListOf(Triple(1, "penny", "pennies"), Triple(5, "nickel", "nickels"), Triple(10, "dime", "dimes"))
        if (difficulty >= 2) coins.add(Triple(25, "quarter", "quarters"))
        val picked = coins.shuffled(random).take(2)
        val n1 = random.nextInt(1, 5)
        val n2 = random.nextInt(1, 5)
        val answer = n1 * picked[0].first + n2 * picked[1].first
        val label1 = if (n1 == 1) picked[0].second else picked[0].third
        val label2 = if (n2 == 1) picked[1].second else picked[1].third
        val options = mutableSetOf(answer)
        for (d in listOf(5, -5, 10, 1, -1).shuffled(random)) {
            if (options.size >= 4) break
            if (answer + d > 0) options.add(answer + d)
        }
        while (options.size < 4) options.add(answer + options.size * 7 + 3)
        return Problem(
            question = "How many cents are $n1 $label1 and $n2 $label2?",
            correctAnswer = answer.toString(),
            options = options.toList().shuffled().map { it.toString() }
        )
    }

    // Telling time from hand positions (1-3)
    private fun timeProblem(): Problem {
        val hour = random.nextInt(1, 12)
        val minuteHand = listOf(12 to "00", 3 to "15", 6 to "30", 9 to "45").random(random)
        val answer = "$hour:${minuteHand.second}"
        val options = mutableSetOf(answer)
        val candidates = listOf("$hour:00", "$hour:15", "$hour:30", "$hour:45", "${hour % 12 + 1}:${minuteHand.second}")
        for (o in candidates.shuffled(random)) {
            if (options.size >= 4) break
            options.add(o)
        }
        return Problem(
            question = "The hour hand is on $hour and the minute hand is on ${minuteHand.first}. What time is it?",
            correctAnswer = answer,
            options = options.toList().shuffled()
        )
    }

    // Fraction of a set: "What is 1/3 of 12?" (3-5)
    private fun fractionOfSetProblem(difficulty: Int): Problem {
        val denom = listOf(2, 3, 4).random(random)
        val answer = random.nextInt(2, 5 + difficulty)
        val total = denom * answer
        val options = mutableSetOf(answer)
        while (options.size < 4) {
            val d = random.nextInt(-2, 3)
            if (d != 0 && answer + d > 0) options.add(answer + d)
        }
        return Problem(
            question = "What is 1/$denom of $total?",
            correctAnswer = answer.toString(),
            options = options.toList().shuffled().map { it.toString() }
        )
    }

    // Percent of a number (4-6); bases chosen so answers stay whole
    private fun percentProblem(difficulty: Int): Problem {
        val pct = listOf(10, 25, 50).random(random)
        val base = when (pct) {
            10 -> random.nextInt(2, 10 + difficulty) * 10
            25 -> random.nextInt(1, 5 + difficulty) * 4
            else -> random.nextInt(2, 10 + difficulty) * 2
        }
        val answer = base * pct / 100
        val options = mutableSetOf(answer, answer * 2)
        while (options.size < 4) {
            val d = random.nextInt(1, base / 10 + 2)
            val candidate = if (random.nextBoolean()) answer + d else answer - d
            if (candidate > 0 && candidate != answer) options.add(candidate)
        }
        return Problem(
            question = "What is $pct% of $base?",
            correctAnswer = answer.toString(),
            options = options.toList().shuffled().map { it.toString() }
        )
    }
}
