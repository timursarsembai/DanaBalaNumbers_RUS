package com.timursarsembayev.danabalanumbers

data class AudioMatchingLevel(
    val pairs: List<AudioMatchingPair>
)

data class AudioMatchingPair(
    val number: MatchingItem
)

object AudioMatchingGameData {
    fun generateAllLevels(): List<AudioMatchingLevel> {
        val levels = mutableListOf<AudioMatchingLevel>()

        // Создаем 10 уровней
        repeat(10) { levelIndex ->
            val pairs = mutableListOf<AudioMatchingPair>()

            // Генерируем 5 пар чисел для каждого уровня
            val usedNumbers = mutableSetOf<Int>()
            repeat(5) {
                var number: Int
                do {
                    number = (1..9).random()
                } while (usedNumbers.contains(number))

                usedNumbers.add(number)

                val matchingItem = MatchingItem(
                    id = number,
                    type = ItemType.NUMBER,
                    value = number,
                    emoji = "", // Для аудио кнопок не нужен emoji
                    isMatched = false
                )

                pairs.add(AudioMatchingPair(matchingItem))
            }

            levels.add(AudioMatchingLevel(pairs))
        }

        return levels
    }
}
