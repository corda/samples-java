package net.corda.samples.auction.client

object SampleDataFactory{

    fun getProxyASampleAsset(): List<SampleAsset>{
        return listOf(
                SampleAsset(
                        "Mona Lisa",
                        "The most famous painting in the world, a masterpiece by Leonardo da Vinci, the mysterious woman with " +
                                "the enigmatic smile. The sitter in the painting is thought to be Lisa Gherardini, the wife of " +
                                "Florence merchant Francesco del Giocondo. It did represent an innovation in art -- the painting" +
                                " is the earliest known Italian portrait to focus so closely on the sitter in a half-length " +
                                "portrait.",
                        "img/Mona_Lisa.jpg"
                )
        )
    }
    fun getProxyBSampleAsset(): List<SampleAsset>{
        return listOf(
                SampleAsset(
                        "The Last Supper",
                        "Yet another masterpiece by Leonardo da Vinci, painted in an era when religious imagery was still " +
                                "a dominant artistic theme, \"The Last Supper\" depicts the last time Jesus broke bread with " +
                                "his disciples before his crucifixion.",
                        "img/The_Last_Supper.jpg"),
                SampleAsset(
                        "The Starry Night",
                        "Painted by Vincent van Gogh, this comparatively abstract painting is the signature example of " +
                                "van Gogh's innovative and bold use of thick brushstrokes. The painting's striking blues and " +
                                "yellows and the dreamy, swirling atmosphere have intrigued art lovers for decades.",
                        "img/The_Scary_Night.jpg")
        )
    }
    fun getProxyCSampleAsset(): List<SampleAsset>{
        return listOf(
                SampleAsset(
                        "The Scream",
                        "First things first -- \"The Scream\" is not a single work of art. According to a British Museum's blog," +
                                " there are two paintings, two pastels and then an unspecified number of prints. Date back to " +
                                "the the year 1893, this masterpiece is a work of Edvard Munch",
                        "img/The_Scream.jpg"
                )
        )
    }
}

data class SampleAsset(val title: String, val description: String, val imageUrl: String) {
}