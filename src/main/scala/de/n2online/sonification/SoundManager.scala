package de.n2online.sonification

import de.sciss.synth._

class SoundManager {

  private val cfg = Server.Config()
  cfg.program = "/usr/bin/scsynth"
  cfg.deviceName = Some("Sonification")
  private val serverConnection = Server.boot(config = cfg) _

  private val sync = new AnyRef
  var s: Server = null

  private var generator = None: Option[Generator]

  def execute(collidercode: Server => Unit): Unit = {
    val li: ServerConnection.Listener = {
      case ServerConnection.Running(srv) => {
        sync.synchronized { s = srv }
        collidercode(s)
      }
    }
    serverConnection(li)
  }

  def getGenerator = generator

  def setGenerator(sgen: Generator): Unit = {
    execute((s: Server) => { /* TODO: free previous */ })
    generator = Some(sgen)
    execute((s: Server) => { sgen.initialize(s) })
  }

}
