package de.n2online.sonification.generators

import de.n2online.sonification.Helpers._
import de.n2online.sonification.{Agent, Helpers, Route}
import de.sciss.synth.Ops._
import de.sciss.synth._
import de.sciss.synth.ugen._

class BasicBeepVolume(val panning: Boolean, val instantUpdate: Boolean) extends Generator {
  private var note = None: Option[Synth]
  private var approval = None: Option[Synth]

  private var deltaAcc: Long = 0
  private var lastUpdate: Long = 0
  private val noteDeltaThreshold = 1000

  override def initialize(server: Server): Unit = {
    val notedef = Some(SynthDef("NoteGen") {
      val vol = "expVol".kr(0.0)

      val freq = 220
      val pure = SinOsc.ar(freq / 2) * 0.1 + SinOsc.ar(freq) * 0.25 + Pulse.ar(freq) * 0.2

      import Env.{Segment => Seg}
      val graph = Env(0.001, List(Seg(0.05, 1), Seg(0.1, 0.5.dbamp), Seg(1, 0.001)))
      val t_trig = "t_trig".tr
      val env = EnvGen.kr(graph, t_trig, doneAction = 0)

      val sig = pure * env

      val panned = Pan2.ar(sig, LinLin("signedPan".kr(0.0), -1.0, 1.0, -0.9, 0.9))

      Out.ar(0, panned * vol)
    })
    note = Some(notedef.get.play())

    approval = Some(Common.approvalSynthDef.get.play())
  }

  override def update(absoluteDistance: Double, correctionAngle: Double, route: Option[Route]): Unit = {
    val expVol = 1.0 - Math.abs(Helpers.wrapToSignedPi(correctionAngle) / Math.PI)
    val signedPan = Math.sin(correctionAngle)
    val now = systemTimeInMilliseconds
    deltaAcc += now - lastUpdate
    if (deltaAcc > noteDeltaThreshold) {
      note.get.set(
        "t_trig" -> 1.0,
        "expVol" -> expVol,
        "signedPan" -> (if (panning) signedPan else 0.0)
      )
      deltaAcc = 0
    }
    if (instantUpdate) note.get.set("expVol" -> expVol, "signedPan" -> signedPan)
    lastUpdate = now
  }

  override def reachedWaypoint(agent: Agent, route: Route): Unit = {
    approval.get.set("t_trig" -> 1)
  }
}
