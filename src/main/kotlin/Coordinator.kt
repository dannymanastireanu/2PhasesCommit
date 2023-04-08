import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.Scanner
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

val executor = Executors.newSingleThreadExecutor()

fun coordinatorMode() {
  val nodes = ConcurrentHashMap<String, Socket>()
  val server = ServerSocket(9999)
  println("Coordinator is running on port ${server.localPort}")

  thread {
    while (true) {
      val client = server.accept()
      nodes[client.inetAddress.hostAddress] = client

      println("Client connected: ${client.inetAddress.hostAddress}")
    }
  }

  while (true) {
    if (nodes.size != 0) {
      println("Read command:")
      val input = readlnOrNull()
      if (input == MESSAGE.ARE_YOU_READY.toString()) {
        nodes.values.forEach { write(it.getOutputStream(), input) }
        println("Successfully send ${MESSAGE.ARE_YOU_READY} command")

        println("Read messages from ${nodes.size}")
        val messages = nodes.entries.map { Pair(it.key, readMessageFromNode(it.value)) }

        val invalidMessages = messages.filter { it.second != MESSAGE.READY.toString() }
        if (invalidMessages.isNotEmpty()) {
          invalidMessages.forEach {
            println("Node[${it.first}] send back ${it.second}")
          }
          broadcast(nodes, MESSAGE.ABORT)
        } else {
          broadcast(nodes, MESSAGE.COMMIT)
        }
      }
    }
  }
}


fun broadcast(nodes: ConcurrentHashMap<String, Socket>, message: MESSAGE) {
  println("SEND: $message command")
  nodes.values.forEach { write(it.getOutputStream(), message.toString()) }
}

fun readMessageFromNode(node: Socket, timeoutSeconds: Long = 20): String {
  val future = executor.submit(Callable(Scanner(node.getInputStream())::nextLine))
  return try {
    future.get(timeoutSeconds, TimeUnit.SECONDS)
  } catch (ex: TimeoutException) {
    ex.printStackTrace()
    println("Fail to receive message from node[${node.inetAddress.hostAddress}]")
    ""
  }
}

fun write(writer: OutputStream, message: String) {
  writer.write((message + '\n').toByteArray(Charset.defaultCharset()))
}