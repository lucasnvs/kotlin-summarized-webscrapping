import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.net.URL

data class Post(
    val title: String,
    val link: String,
    val image: String,
    val imageLocal: String = "",
    var content: String = ""
)

data class PostBean(
    val title: String,
    val author: String,
    var image: String,
    var content: String = ""
)

class SummarizedScraper(
    val DIRECTORY_NAME: String
) {
    private val TAG_SCRAPER = "SCRAPER"
    private val TAG_FILE_HANDLER = "FILE_HANDLER"
    private val TAG_NETWORK = "NETWORK"

    fun doScrapping() {
        logInfo(TAG_SCRAPER, "Iniciando o processo de scraping...")
        val posts = getPostsInformation()

        logSuccess(TAG_SCRAPER, "Total de posts: ${posts.size}")

        val outputFolder = File(DIRECTORY_NAME)
        if (!outputFolder.exists()) {
            outputFolder.mkdir()
            logSuccess(TAG_FILE_HANDLER, "Pasta '$DIRECTORY_NAME' criada.")
        }

        posts.forEach { post ->
            val postWithContent = findAndFormatPostContent(post)
            createPostFile(postWithContent, outputFolder)
        }

        logSuccess(TAG_SCRAPER, "Posts extraídos e salvos com sucesso!")
    }

    fun getPostsInformation(): List<Post> {
        val pagesUrl: List<String> = listOf( // Easy Way
            "https://summarizedbookz.blogspot.com",
            "https://summarizedbookz.blogspot.com/search?updated-max=2021-11-07T21%3A37%3A00%2B05%3A30&max-results=6#PageNo=2",
            "https://summarizedbookz.blogspot.com/search?updated-max=2021-10-29T23%3A13%3A00%2B05%3A30&max-results=6#PageNo=3",
            "https://summarizedbookz.blogspot.com/search?updated-max=2021-10-19T22%3A13%3A00%2B05%3A30&max-results=6#PageNo=4",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-03-12T15%3A33%3A00%2B05%3A30&max-results=6#PageNo=5",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-03-06T13%3A04%3A00%2B05%3A30&max-results=6#PageNo=6",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-02-29T13%3A35%3A00%2B05%3A30&max-results=6#PageNo=7",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-02-23T20%3A46%3A00%2B05%3A30&max-results=6#PageNo=8",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-02-17T11%3A27%3A00%2B05%3A30&max-results=6#PageNo=9",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-02-11T16%3A13%3A00%2B05%3A30&max-results=6#PageNo=10",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-02-05T13%3A12%3A00%2B05%3A30&max-results=6#PageNo=11",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-01-30T16%3A51%3A00%2B05%3A30&max-results=6#PageNo=12",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-01-24T20%3A48%3A00%2B05%3A30&max-results=6#PageNo=13",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-01-18T13%3A32%3A00%2B05%3A30&max-results=6#PageNo=14",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-01-12T19%3A46%3A00%2B05%3A30&max-results=6#PageNo=15",
            "https://summarizedbookz.blogspot.com/search?updated-max=2020-01-06T13%3A20%3A00%2B05%3A30&max-results=6#PageNo=16",
            "https://summarizedbookz.blogspot.com/search?updated-max=2019-12-31T19%3A21%3A00%2B05%3A30&max-results=6#PageNo=17"
        )

        val posts = mutableListOf<Post>()

        pagesUrl.forEach { pageUrl ->
            logInfo(TAG_SCRAPER, "Acessando $pageUrl")

            val document: Document = try {
                Jsoup.connect(pageUrl).get()
            } catch (e: Exception) {
                logError(TAG_NETWORK, "Erro ao acessar $pageUrl: ${e.message}")
                return@forEach
            }

            val postElements = document.select(".blog-post.hentry.index-post")
            if (postElements.isEmpty()) {
                logWarning(TAG_SCRAPER, "Nenhum post encontrado para página $pageUrl. Continuando...")
                return@forEach
            }

            postElements.forEach { element ->
                val title = element.select(".post-title a").text()
                val link = element.select(".post-title a").attr("abs:href")
                val image = element.select("img").attr("src")
                posts.add(Post(title, link, image))
            }
        }

        return posts
    }

    fun createPostFile(post: Post, outputFolder: File) {
        val sanitizedTitle = post.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val postFile = File(outputFolder, "$sanitizedTitle.txt")

        try {
            postFile.writeText("Título: ${post.title}\n")
            postFile.appendText("Link: ${post.link}\n")
            postFile.appendText("Imagem: ${post.image}\n")
            postFile.appendText("\nConteúdo:\n")
            postFile.appendText(post.content)
            logSuccess(TAG_FILE_HANDLER, "Arquivo criado: ${postFile.absolutePath}")
        } catch (e: Exception) {
            logError(TAG_FILE_HANDLER, "Erro ao salvar o arquivo ${postFile.name}: ${e.message}")
        }
    }

    fun findAndFormatPostContent(post: Post): Post {
        logInfo(TAG_NETWORK, "Acessando post: ${post.title}")
        val postDocument: Document = try {
            Jsoup.connect(post.link).get()
        } catch (e: Exception) {
            logError(TAG_NETWORK, "Erro ao acessar ${post.link}: ${e.message}")
            throw e
        }

        val content = postDocument.select(".post-body.post-content").html()

        val formattedContent = content
            .replace("<p>", "\n")
            .replace("</p>", "")
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace(Regex("\\s*\n\\s*"), "\n")

        post.content = formattedContent.trim()

        return post
    }

    fun readFolder(): List<String> {
        val folder = File(DIRECTORY_NAME)

        if (folder.exists() && folder.isDirectory) {
            val files = folder.listFiles()

            if (files != null && files.isNotEmpty()) {
                logInfo(TAG_FILE_HANDLER, "Arquivos encontrados na pasta: ${folder.absolutePath}")

                return files.mapNotNull { file ->
                    if (file.isFile) {
                        file.name
                    } else {
                        null
                    }
                }
            } else {
                logError(TAG_FILE_HANDLER, "A pasta está vazia ou ocorreu um erro ao ler os arquivos.")
            }
        } else {
            logError(TAG_FILE_HANDLER, "O diretório não existe ou não é uma pasta válida.")
        }

        return emptyList()
    }

    fun proccessFolder(caminhoPasta: String, acao: (File) -> Unit) {
        val pasta = File(caminhoPasta)

        if (pasta.exists() && pasta.isDirectory) {
            pasta.listFiles { file -> file.isDirectory }?.forEach { subpasta ->
                acao(subpasta)
            }
        } else {
            println("O caminho fornecido não é uma pasta válida!")
        }
    }

    fun formatFileName(originalFilePath: String) {
        val originalFile = File("$DIRECTORY_NAME/$originalFilePath")

        if (originalFile.exists()) {
            val nameWithoutExtension = originalFile.name.substringBeforeLast(".txt")

            val newNameWithoutInterval = nameWithoutExtension.replace(Regex("___.*?(?=-)"), "").lowercase()

            val newFileName = "$newNameWithoutInterval.txt"

            val newFile = File(originalFile.parent, newFileName)
            val renamed = originalFile.renameTo(newFile)

            if (renamed) {
                logSuccess(TAG_FILE_HANDLER, "Arquivo renomeado com sucesso para: ${newFile.absolutePath}")
            } else {
                logError(TAG_FILE_HANDLER, "Erro ao renomear o arquivo.")
            }
        } else {
            logError(TAG_FILE_HANDLER, "O arquivo original não foi encontrado. ($originalFilePath)")
        }
    }

    fun downloadPostImage(file: File) {
        val lines = file.readLines()
        val imageUrl = lines.find { it.startsWith("Imagem: ") }?.substringAfter("Imagem: ")?.trim()

        if (imageUrl.isNullOrEmpty()) {
            logError(TAG_NETWORK,"Nenhum link de imagem encontrado em ${file.name}")
            return
        }

        val folderName = file.nameWithoutExtension
        val outputFolder = File(file.parentFile, folderName)

        if (!outputFolder.exists()) {
            outputFolder.mkdir()
        }

        val imageFile = File(outputFolder, "imagem.jpg")

        if (imageFile.exists()) {
            logWarning(TAG_FILE_HANDLER, "A imagem já existe para ${file.name}, pulando download.")
        } else {
            try {
                logInfo(TAG_NETWORK, "Baixando imagem de $imageUrl para ${file.name}...")
                URL(imageUrl).openStream().use { input ->
                    imageFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                logSuccess(TAG_FILE_HANDLER, "Imagem salva em: ${imageFile.absolutePath}")
            } catch (e: Exception) {
                logError(TAG_NETWORK, "Erro ao baixar a imagem para ${file.name}: ${e.message}")
            }
        }

        val destinationFile = File(outputFolder, file.name)
        if (file.renameTo(destinationFile)) {
            logSuccess(TAG_FILE_HANDLER, "Arquivo ${file.name} movido para: ${outputFolder.absolutePath}")
        } else {
            logError(TAG_FILE_HANDLER, "Erro ao mover o arquivo ${file.name} para a pasta ${outputFolder.absolutePath}")
        }
    }

    fun parsePost(
        folderPath: String,
        fileName: String
    ): PostBean? {
        if(!File("$DIRECTORY_NAME/$folderPath/$fileName").exists()) return null

        val fileContent = File("$DIRECTORY_NAME/$folderPath/$fileName").readText()

        val tituloRegex = """Título: (.+?)\n""".toRegex()
        val conteudoRegex = """(?s)Conteúdo:\n(.+)""".toRegex()

        val titulo = tituloRegex.find(fileContent)?.groupValues?.get(1)?.trim()
        val conteudo = conteudoRegex.find(fileContent)?.groupValues?.get(1)?.trim()

        val imagemLocal = "$DIRECTORY_NAME/$folderPath/imagem.jpg"

        val regex = Regex("-\\s*([^|]+)\\s*(\\||$)")
        val match = titulo?.let { regex.find(it) }
        val author = match?.groupValues?.get(1)?.trim()

        return if (titulo != null && conteudo != null && author != null) {
            PostBean(titulo, author, imagemLocal, conteudo)
        } else {
            null
        }
    }

    private fun logInfo(tag: String, message: String) {
        Logger.info(tag, message)
    }

    private fun logSuccess(tag: String, message: String) {
        Logger.success(tag, message)
    }

    private fun logWarning(tag: String, message: String) {
        Logger.warning(tag, message)
    }

    private fun logError(tag: String, message: String) {
        Logger.error(tag, message)
    }
}