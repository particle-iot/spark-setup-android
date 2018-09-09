package io.particle.android.sdk.utils

import java.util.*


object CoreNameGenerator {

    private val random = Random()

    private val TROCHEES = arrayOf("aardvark", "bacon", "badger", "banjo", "bobcat", "boomer", "captain", "chicken", "cowboy", "maker", "splendid", "useful", "dentist", "doctor", "dozen", "easter", "ferret", "gerbil", "hacker", "hamster", "sparkling", "hobbit", "hoosier", "hunter", "jester", "jetpack", "kitty", "laser", "lawyer", "mighty", "monkey", "morphing", "mutant", "narwhal", "ninja", "normal", "penguin", "pirate", "pizza", "plumber", "power", "puppy", "ranger", "raptor", "robot", "scraper", "spark", "station", "tasty", "trochee", "turkey", "turtle", "vampire", "wombat", "zombie")

    private val randomName: String
        get() {
            val randomIndex = random.nextInt(TROCHEES.size)
            return TROCHEES[randomIndex]
        }


    fun generateUniqueName(existingNames: Set<String>): String {
        var uniqueName: String? = null
        while (uniqueName == null) {
            val part1 = randomName
            val part2 = randomName
            val candidate = part1 + "_" + part2
            if (!existingNames.contains(candidate) && part1 != part2) {
                uniqueName = candidate
            }
        }
        return uniqueName
    }
}
