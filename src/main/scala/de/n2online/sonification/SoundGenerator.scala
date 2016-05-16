package de.n2online.sonification

import de.sciss.synth._
import ugen._
import Ops._
import de.sciss.synth.ServerConnection.Listener

class SoundGenerator {
  var synth = None: Option[Synth]
  var sawdef = None: Option[SynthDef]
  var factor = 0.05
  val cfg = Server.Config()
  cfg.program = "/usr/bin/scsynth"
  cfg.deviceName = Some("Sonification")
  val serverConnection = Server.boot(config = cfg) _
  val sync = new AnyRef
  var s: Server = null
  val li: ServerConnection.Listener = {
    case ServerConnection.Running(srv) => sync.synchronized { s = srv };
      sawdef = Some(SynthDef("MegaSaw") {
        val numSaws = 7
        val outChannel = "out".kr(0.0)
        val freq = "freq".kr(33.midicps)
        val acc =
          VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
            VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
            VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
            VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
            VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
            VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
            VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05)
        Out.ar(outChannel, acc * (1.0 / numSaws) * "factor".kr(0.0))
      })
      synth = Some(sawdef.get.play())
  }
  serverConnection(li)

  def setVol(vol: Double) {
    factor = vol
    synth.foreach(_.set("factor" -> vol))
  }
}
