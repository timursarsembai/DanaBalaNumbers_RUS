package com.example.danabala

data class MatchingItem(
    val id: Int,
    val type: ItemType,
    val value: Int,
    val emoji: String = "",
    var isMatched: Boolean = false,
    var position: Int = 0
)

enum class ItemType {
    NUMBER,
    OBJECTS
}

data class MatchingPair(
    val number: MatchingItem,
    val objects: MatchingItem
)

data class MatchingLevel(
    val levelNumber: Int,
    val pairs: List<MatchingPair>
)

class MatchingGameData {
    companion object {
        private val objectEmojis = listOf("🍎", "⭐", "🎈", "🌸", "🎯", "🍓", "🎪", "🎨", "🎁", "🌟")

        fun generateLevel(levelNumber: Int): MatchingLevel {
            val numbers = (1..5).toList()
            val shuffledEmojis = objectEmojis.shuffled().take(5)

            val pairs = numbers.mapIndexed { index, number ->
                val numberItem = MatchingItem(
                    id = index * 2,
                    type = ItemType.NUMBER,
                    value = number,
                    position = index
                )

                val objectsItem = MatchingItem(
                    id = index * 2 + 1,
                    type = ItemType.OBJECTS,
                    value = number,
                    emoji = shuffledEmojis[index],
                    position = index + 5
                )

                MatchingPair(numberItem, objectsItem)
            }

            return MatchingLevel(levelNumber, pairs)
        }

        fun generateAllLevels(): List<MatchingLevel> {
            return (1..10).map { generateLevel(it) }
        }
    }
}
