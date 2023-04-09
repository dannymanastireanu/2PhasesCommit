import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.Scanner
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

val executor: ExecutorService = Executors.newSingleThreadExecutor()

fun coordinatorMode() {
  val nodes = ConcurrentHashMap<String, Socket>()
  val server = ServerSocket(9999)
  println("Coordinator is running on port ${server.localPort}")

  thread {
    while (true) {
      val client = server.accept()
      val clientId = "${client.inetAddress.hostAddress}:${client.port}"
      nodes[clientId] = client
      println("Client connected: $clientId . Total Number of nodes: ${nodes.size}")
    }
  }

  while (true) {
    if (nodes.size != 0) {
      println("Read command:")
      val input = readlnOrNull()
      if (input == MESSAGE.ARE_YOU_READY.toString()) {
        broadcast(nodes, MESSAGE.ARE_YOU_READY)
        println("Successfully send ${MESSAGE.ARE_YOU_READY} command to all ${nodes.size} nodes")

        println("Read messages from ${nodes.size} nodes")
        val messages = try {
          nodes.entries.map { Pair(it.key, readMessageFromNode(nodes, it.value)) }
        } catch (e: Exception) {
          listOf(Pair("", MESSAGE.NOT_READY.toString()))
        }

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
  nodes.values.forEach {
    println("[Coordinator]: Send $message command to ${getId(it)}")
    write(nodes, it, message.toString())
  }
}

fun readMessageFromNode(nodes: ConcurrentHashMap<String, Socket>, node: Socket, timeoutSeconds: Long = 20): String {
  return try {
    val future = executor.submit(Callable(Scanner(node.getInputStream())::nextLine))
    future.get(timeoutSeconds, TimeUnit.SECONDS)
  } catch (ex: TimeoutException) {
    println("Fail to receive message from node[${getId(node)}]")
    MESSAGE.NOT_READY.toString()
  } catch (e: NoSuchElementException) {
    println("Node[${getId(node)}] disconnected")
    nodes.remove(getId(node))
    MESSAGE.NOT_READY.toString()
  }
}

fun getId(node: Socket): String = "${node.inetAddress.hostAddress}:${node.port}"

fun write(nodes: ConcurrentHashMap<String, Socket>, node: Socket, message: String) {
  try {
    if (node.isConnected && !node.isClosed) {
      node.getOutputStream().write((message + '\n').toByteArray(Charset.defaultCharset()))
    } else {
      nodes.remove(getId(node))
    }
  } catch (e: Exception) {
    e.printStackTrace()
    nodes.remove(getId(node))
  }
}