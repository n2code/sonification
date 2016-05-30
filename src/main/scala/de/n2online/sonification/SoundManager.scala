package de.n2online.sonification

import de.n2online.sonification.Helpers._
import de.n2online.sonification.generators.Generator
import de.sciss.synth._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class SoundManager {
  private var generator = None: Option[Generator]
  private val sync = new AnyRef

  //boot process
  private val cfg = Server.Config()
  cfg.program = "/usr/bin/scsynth"
  cfg.deviceName = Some("Sonification")
  cfg.outputBusChannels = 2
  private var s: Server = null
  val server: Future[Server] = {
    val p = Promise[Server]()
    try {
      Server.boot(config = cfg)({
        case ServerConnection.Running(srv) => {
          sync.synchronized {
            s = srv
          }
          p.success(srv)
        }
      })
    } catch {
      case err: Exception => p.failure(err)
    }
    p.future
  }

  def stopServer() = {
    if (s != null && s.condition != Server.Offline) s.quit()
  }

  def execute(collidercode: Server => Unit, successMsg: Option[String] = None): Future[Long] = {
    val p = Promise[Long]()
    val start = systemTimeInMilliseconds
    server.onComplete {
      case Success(srv) =>
        sync.synchronized {
          collidercode(srv)
        }
        println(successMsg getOrElse "sc-code executed.")
        p.success(systemTimeInMilliseconds - start)
      case Failure(ex) => println("Could not execute, server future failed")
    }
    p.future
  }

  def freeAll() = {
    execute((s: Server) => {
      s ! s.freeAllMsg
    })
  }

  def getGenerator = generator

  def setGenerator(sgen: Generator): Future[Long] = {
    generator = Some(sgen)
    execute((s: Server) => {
      sgen.initialize(s)
    }, Some(s"Generator ${sgen.getClass.getSimpleName} initialized."))
  }

}
