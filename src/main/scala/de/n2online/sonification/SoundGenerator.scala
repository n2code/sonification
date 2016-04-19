package de.n2online.sonification

import de.sciss.synth._
import ugen._
import Ops._
import de.sciss.synth.ServerConnection.Listener

class SoundGenerator {
  var synth = Option.empty[Synth ]
  var factor = 0.05
  val cfg = Server.Config()
  cfg.program = "/usr/bin/scsynth"
  val serverConnection = Server.boot(config = cfg) _
  val sync = new AnyRef
  var s: Server = null
  val li: ServerConnection.Listener = {
    case ServerConnection.Running(srv) => sync.synchronized {
      s = srv
    }; synth = Some(play {
      val f = LFSaw.kr(0.4).madd(24, LFSaw.kr(Seq(8, 7.23)).madd(3, 80)).midicps
      CombN.ar(SinOsc.ar(f) * 0.04, 0.2, 0.2, 4) * "factor".kr(0.05)
    })
  }
  serverConnection(li)

  def setVol(vol: Double) {
    factor = vol
    synth.foreach(_.set("factor" -> vol))
  }
}
