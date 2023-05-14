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
import kotlin.random.Random
import kotlin.random.nextInt

val executorNode: ExecutorService = Executors.newSingleThreadExecutor()

fun retryConnect(address: String = "localhost", port: Int = 9999): Client {
  val maxRetry = 3
  for(i in 1..maxRetry) {
    try {
      return Client(address, port)
    } catch (e: Exception) {
      val sleepTimeMs = 5000L * i
      Thread.sleep(sleepTimeMs)
      if (i == maxRetry) {
        e.printStackTrace()
      }
      println("Attempt[$i/$maxRetry] -> FAIL TO CONNECT TO COORDINATOR.. Sleep($sleepTimeMs ms)")
    }
  }
  throw Error("Fail to connect to the server[$address:$port]")
}

fun nodeMode(address: String = "localhost") {
  try {
    val client = retryConnect(address)
    client.run()
  } catch (e: Exception) {
    e.printStackTrace()
  }
}


class Client(address: String, port: Int) {
  private val coordinator: Socket = Socket(address, port)

  init {
    println("Connected to server at $address on port $port")
  }

  private val reader: Scanner = Scanner(coordinator.getInputStream())
  private val writer: OutputStream = coordinator.getOutputStream()

  fun run() {
    while (true) {
      println("Read message from coordinator...")
      val coordinatorMessage = reader.nextLine()
      println("Received from coordinator: $coordinatorMessage")

      if (coordinatorMessage == MESSAGE.ARE_YOU_READY.toString()) {
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
    if (READ_FROM_CONSOLE) {
      println("[Node]: Type my state: ")
      val future = executorNode.submit(Callable(operation))
      return try {
        future.get(timeoutSeconds, TimeUnit.SECONDS) ?: MESSAGE.NOT_READY.toString()
      } catch (ex: TimeoutException) {
        MESSAGE.NOT_READY.toString()
      }
    } else {
      Thread.sleep((timeoutSeconds / 3) * 1000)
      val probability = Random.nextInt(0..100)
      val probabilityOfFailure = 10
      return if(probability < probabilityOfFailure) {
        println("[Node]: I am ==== NOT READY ====")
        MESSAGE.NOT_READY.toString()
      } else {
        println("[Node]: I am ---- READY! ----")
        MESSAGE.READY.toString()
      }
    }
  }

  private fun write(message: String) {
    writer.write((message + '\n').toByteArray(Charset.defaultCharset()))
  }
}
