package jr.brian.drawai.model.state

data class AIChatState(
    val response: String? = null,
    val objectToDraw: String? = null,
    val currentCapture: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AIChatState

        if (response != other.response) return false
        if (objectToDraw != other.objectToDraw) return false
        if (!currentCapture.contentEquals(other.currentCapture)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = response?.hashCode() ?: 0
        result = 31 * result + (objectToDraw?.hashCode() ?: 0)
        result = 31 * result + (currentCapture?.contentHashCode() ?: 0)
        return result
    }
}

val drawingSuggestions = listOf(
    "Animal",
    "Appliance",
    "Ball",
    "Building",
    "Clothing",
    "Electronic device",
    "Flower",
    "Food",
    "Furniture",
    "Insect",
    "Musical instrument",
    "Plant",
    "Tool",
    "Tree",
    "Vehicle",
    "Cartoon"
)