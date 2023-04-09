import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.Scanner
import java.util.UUID
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

      nodes.values.forEach {
        if (it.isClosed) {
          println("${getId(it)} is closed")
        }
      }
    }
  }

  while (true) {
    if (nodes.size != 0) {
      val message = if(READ_FROM_CONSOLE) {
        println("Read message for nodes from console:")
        readlnOrNull() ?: MESSAGE.ARE_YOU_READY.toString()
      } else {
        MESSAGE.ARE_YOU_READY.toString()
      }
      val transactionId = UUID.randomUUID().toString().split("-")[0]

      if(message == MESSAGE.ARE_YOU_READY.toString()) {
        broadcast(nodes, MESSAGE.ARE_YOU_READY, transactionId)
        val messages = try {
          nodes.entries.map { Pair(it.key, readMessageFromNode(nodes, it.value, transactionId)) }
        } catch (e: Exception) {
          listOf(Pair("", MESSAGE.NOT_READY.toString()))
        }

        val invalidMessages = messages.filter { it.second != MESSAGE.READY.toString() }
        if (invalidMessages.isNotEmpty()) {
          invalidMessages.forEach {
            println("[$transactionId][Node:${it.first}] send back ${it.second}")
          }
          broadcast(nodes, MESSAGE.ABORT, transactionId)
        } else {
          broadcast(nodes, MESSAGE.COMMIT, transactionId)
        }
      } else {
        println("Allow message: ${MESSAGE.ARE_YOU_READY}")
      }
    }
  }
}


fun broadcast(nodes: ConcurrentHashMap<String, Socket>, message: MESSAGE, transactionId: String) {
  nodes.values.forEach {
    println("[$transactionId][Coordinator]: Send $message command to ${getId(it)}")
    write(nodes, it, message.toString())
  }
}

fun readMessageFromNode(nodes: ConcurrentHashMap<String, Socket>, node: Socket, transactionId: String, timeoutSeconds: Long = 20): String {
  return try {
    val future = executor.submit(Callable(Scanner(node.getInputStream())::nextLine))
    future.get(timeoutSeconds, TimeUnit.SECONDS)
  } catch (ex: TimeoutException) {
    println("[$transactionId]Fail to receive message from node[${getId(node)}]")
    MESSAGE.NOT_READY.toString()
  } catch (e: NoSuchElementException) {
    println("[$transactionId][Node: ${getId(node)}] disconnected")
    nodes.remove(getId(node))
    MESSAGE.NOT_READY.toString()
  } catch (e: java.lang.Exception) {
    println("[$transactionId]Unable to read message from ${getId(node)}")
    nodes.remove(getId(node))
    MESSAGE.NOT_READY.toString()
  }
}

fun getId(node: Socket): String = "${node.inetAddress.hostAddress}:${node.port}"

fun write(nodes: ConcurrentHashMap<String, Socket>, node: Socket, message: String) {
  try {
    node.getOutputStream().write((message + '\n').toByteArray(Charset.defaultCharset()))
  } catch (e: IOException) {
    e.printStackTrace()
    nodes.remove(getId(node))
  }
}