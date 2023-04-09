import java.io.OutputStream
import java.lang.Exception
import java.net.Socket
import java.nio.charset.Charset
import java.util.Scanner
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


val executorNode: ExecutorService = Executors.newSingleThreadExecutor()

fun nodeMode() {
  val address = "localhost"
  val port = 9999

  try {
    val client = Client(address, port)
    client.run()
  } catch (e: Exception) {
    println("FAIL TO CONNECT TO COORDINATOR")
    e.printStackTrace()
  }
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
      println("Read message from coordinator...")
      val coordinatorMessage = reader.nextLine()
      println("Received from coordinator: $coordinatorMessage")

      if (coordinatorMessage == MESSAGE.ARE_YOU_READY.toString()) {
        println("[Node]: Type my state: ")
        write(readStateTimeout(::readlnOrNull))
        when (val responseCoordinator = readCommandFromCoordinator(reader::nextLine)) {
          MESSAGE.COMMIT.toString() -> {
            println("[Coordinator]: COMMIT the work")
          }

          MESSAGE.ABORT.toString() -> {
            println("[Coordinator]: ABORT the work")
          }

          else -> {
            println("Unknown message from coordinator: $responseCoordinator")
          }
        }
      }
    }
  }

  fun readCommandFromCoordinator(readCmd: () -> String, timeoutSeconds: Long = 10): String {
    val future = executorNode.submit(Callable(readCmd))
    return try {
      future.get(timeoutSeconds, TimeUnit.SECONDS) ?: MESSAGE.NOT_READY.toString()
    } catch (ex: TimeoutException) {
      println("No message received from coordinator in ${timeoutSeconds} seconds. Go with ${MESSAGE.ABORT} command")
      MESSAGE.ABORT.toString()
    }
  }

  fun readStateTimeout(operation: () -> String?, timeoutSeconds: Long = 10): String {
    val future = executorNode.submit(Callable(operation))
    return try {
      future.get(timeoutSeconds, TimeUnit.SECONDS) ?: MESSAGE.NOT_READY.toString()
    } catch (ex: TimeoutException) {
      MESSAGE.NOT_READY.toString()
    }
  }

  private fun write(message: String) {
    writer.write((message + '\n').toByteArray(Charset.defaultCharset()))
  }
}
