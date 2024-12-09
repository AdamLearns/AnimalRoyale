package live.adamlearns.animalroyale.extensions

import org.bukkit.Color
import kotlin.math.pow
import kotlin.math.sqrt

fun Color.distanceTo(otherColor: Color): Double {
    // This is not the most 'accurate' distance since RGB is not uniform, but is probably close enough for us 😬
    // Taken from https://en.wikipedia.org/wiki/Color_difference
    val rDiff = (red - otherColor.red).toDouble()
    val gDiff = (green - otherColor.green).toDouble()
    val bDiff = (green - otherColor.green).toDouble()
    val rMean = (red + otherColor.red).toDouble() / 2.0

    val a = (2.0 + rMean / 256.0)
    val b = 4.0
    val c = (2.0 + (255.0 - rMean) / 256.0)

    return sqrt( a * rDiff.pow(2) + b * gDiff.pow(2) + c * bDiff.pow(2))
}
