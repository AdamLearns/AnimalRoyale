package live.adamlearns.animalroyale.extensions

fun Float.format(digits: Int) = "%.${digits}f".format(this)
