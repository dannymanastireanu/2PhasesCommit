fun main(args: Array<String>) {
  if(args.size != 1) {
    throw Error("Should accept just one argument, not ${args.size}.")
  }

  if(args[0] == "coordinator") {
    println("Starting coordinator")
    coordinatorMode()
  } else if(args[0] == "node") {
    println("Starting node")
    nodeMode()
  } else {
    throw Error("Argument param should be `coordinator` or `node`");
  }
}