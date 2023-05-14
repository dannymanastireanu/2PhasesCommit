fun main(args: Array<String>) {
  if(args.size != 1) {
    println("Received ${args.size} args.")
  } else {
    if(args[0] == "coordinator") {
      println("Starting coordinator")
      coordinatorMode()
    } else if(args[0] == "node") {
      println("Starting node")
      nodeMode()
    } else {
      throw Error("Argument param should be `coordinator` or `node`. Not '${args[0]}'")
    }
  }
}