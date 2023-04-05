import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.util.Scanner
import java.util.concurrent.Executors

enum class MESSAGE {
  COMMIT,
  ABORT,
  ARE_YOU_READY,
  NOT_READY,
  READY
}

fun main() {
  val address = "localhost"
  val port = 9999

  val client = Client(address, port)
  client.run()
}


class Client(address: String, port: Int) {
  private val connection: Socket = Socket(address, port)
  private var connected: Boolean = true

  init {
    println("Connected to server at $address on port $port")
  }

  private val reader: Scanner = Scanner(connection.getInputStream())
  private val writer: OutputStream = connection.getOutputStream()

  fun run() {
    while (connected) {
      println("Read message from coordinator")
      val coordinatorMessage = reader.nextLine()
      println("Received from coordinator: $coordinatorMessage")

      if (coordinatorMessage == MESSAGE.ARE_YOU_READY.toString()) {
        println("Type my state: ")
        val state = readlnOrNull() ?: MESSAGE.NOT_READY.toString()
        write(state)
        when (val responseCoordinator = reader.nextLine()) {
          MESSAGE.COMMIT.toString() -> {
            println("Commit the work")
          }

          MESSAGE.ABORT.toString() -> {
            println("ABORT the work")
          }

          else -> {
            println("Unknown message from coordinator: $responseCoordinator")
          }
        }
      }
    }
  }

  private fun write(message: String) {
    writer.write((message + '\n').toByteArray(Charset.defaultCharset()))
  }

  private fun read() {
    while (connected)
      println("Server: ${reader.nextLine()}")
  }
}