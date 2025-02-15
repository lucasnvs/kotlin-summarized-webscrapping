import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

//data class MyObject(
//    val id: Int,
//    val name: String,
//    val imagePath: String
//)

class DatabaseHandler(private val connection: Connection) {

    fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS objects (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                image_url TEXT NOT NULL
            )
        """
        connection.createStatement().execute(sql)
    }

    fun insertObject(obj: PostBean) {
        val sql = "INSERT INTO posts (title, author, image_resource, content) VALUES (?, ?, ?, ?)"
        val statement: PreparedStatement = connection.prepareStatement(sql)
        statement.setString(1, obj.title)
        statement.setString(2, obj.author)
        statement.setString(3, obj.image)
        statement.setString(4, obj.content)
        statement.executeUpdate()
    }

    fun close() {
        connection.close()
    }
}

fun saveImage(imagePath: String): String {
    val storageFolder = File("storage")
    if (!storageFolder.exists()) {
        storageFolder.mkdirs()
    }

    val sourceFile = File(imagePath)
    val destinationFile = File(storageFolder, sourceFile.name)

    Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

    return destinationFile.absolutePath
}

fun connectToDatabase(): Connection {
    val url = "jdbc:mysql://localhost:3306/trilho"
    val user = "root"
    val password = ""

    Class.forName("com.mysql.cj.jdbc.Driver")

    return DriverManager.getConnection(url, user, password)
}

fun main() {
    val summarizedScraper = SummarizedScraper("posts")

    val connection = connectToDatabase()
    val dbHandler = DatabaseHandler(connection)

    summarizedScraper.proccessFolder("posts", { folder ->
        Logger.info("TAG", folder.name)

        val postInfo = summarizedScraper.parsePost(
            folder.name,
            "${folder.name}.txt"
        );


        if(postInfo != null) {
            val imageUrl = saveImage(postInfo.image)

            postInfo.image = imageUrl
            dbHandler.insertObject(postInfo)
            Logger.success("DB", "Object saved successfully! Image URL: $imageUrl")
        }
    })


    dbHandler.close()
}