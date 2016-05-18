package de.n2online.sonification

import de.sciss.synth._

import scala.concurrent.{Future, Promise}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

class SoundManager {

  private val cfg = Server.Config()
  cfg.program = "/usr/bin/scsynth"
  cfg.deviceName = Some("Sonification")
  private val serverConnection = Server.boot(config = cfg) _

  private val sync = new AnyRef
  var s: Server = null

  private var generator = None: Option[Generator]

  val server: Future[Server] = {
    val p = Promise[Server]()
    serverConnection({
      case ServerConnection.Running(srv) => {
        sync.synchronized { s = srv }
        p.success(srv)
      }
    })
    p.future
  }

  def execute(collidercode: Server => Unit, successMsg: Option[String] = None): Future[Long] = {
    val p = Promise[Long]()
    val start = System.nanoTime()
    server.onComplete {
      case Success(srv) => {
        collidercode(srv)
        println(successMsg getOrElse "sc-code executed.")
        p.success((System.nanoTime() - start) / 1000000)
      }
      case Failure(ex) => println("Could not execute, server future failed")
    }
    p.future
  }

  def getGenerator = generator

  def setGenerator(sgen: Generator): Future[Long] = {
    //execute((s: Server) => { ??? }) //TODO: free previous
    generator = Some(sgen)
    execute((s: Server) => { sgen.initialize(s) }, Some(s"Generator ${sgen.getClass.getSimpleName} initialized."))
  }

}
