package fr.triquet.manyinone.radio

enum class SleepTimerOption(val label: String, val minutes: Long) {
    OFF("Off", 0),
    MIN_15("15 min", 15),
    MIN_30("30 min", 30),
    MIN_60("1 h", 60),
    MIN_90("1 h 30", 90),
    MIN_120("2 h", 120),
}
