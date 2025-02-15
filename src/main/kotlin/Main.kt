import java.io.File

fun main() {
    val summarizedScraper = SummarizedScraper("posts")

    summarizedScraper.proccessFolder("posts", { folder ->
        Logger.info("TAG", folder.name)
    })
}