package jr.brian.drawai.model.state

data class AIChatState(
    val response: String? = null,
    val objectToDraw: String? = null
)

val drawingSuggestions = listOf(
    "Key",
    "Mushroom",
    "Feather",
    "Button",
    "Anchor",
    "Lightbulb",
    "Umbrella",
    "Sock",
    "Magnifying Glass",
    "Dice (single die)",
    "Banana",
    "Paperclip",
    "Cloud",
    "Whistle",
    "Snail",
    "Bicycle",
    "Car",
    "Skateboard",
    "Book (closed)",
    "Leaf",
    "Coin",
    "Glasses (spectacles)",
    "Ladder",
    "Envelope",
    "Comb",
    "Planet (simple with ring)",
    "Spiderweb (corner)",
    "Traffic Cone",
    "Brick",
    "Candle",
    "Arrow",
    "Pretzel",
    "Basketball"
)