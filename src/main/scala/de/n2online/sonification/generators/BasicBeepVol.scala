package de.n2online.sonification.generators

import de.n2online.sonification.{Helpers, Route}
import de.n2online.sonification.Helpers._
import de.sciss.synth.Ops._
import de.sciss.synth._
import de.sciss.synth.ugen._

class BasicBeepVol extends Generator {
  private var notedef = None: Option[SynthDef]
  private var approval = None: Option[Synth]

  private var deltaAcc: Long = 0
  private var lastUpdate: Long = 0
  private val noteDeltaThreshold = 1000

  override def initialize(server: Server): Unit = {
    notedef = Some(SynthDef("NoteGen") {
      val vol = "expVol".kr(0.0)

      val freq = 220
      val pure = SinOsc.ar(freq / 2) * 0.1 + SinOsc.ar(freq) * 0.25 + Pulse.ar(freq) * 0.2

      import Env.{Segment => Seg}
      val graph = Env(0.001, List(Seg(0.05, 1), Seg(0.1, 0.5.dbamp), Seg(1, 0.001)))
      val env = EnvGen.kr(graph, doneAction = 2)

      val sig = pure * env

      Out.ar(List(0, 1), sig * vol)
    })

    approval = Some(Common.approvalSynthDef.get.play())
  }

  def assertInitialized() = {
    assert(notedef.isDefined && approval.isDefined)
  }

  override def update(absoluteDistance: Double, correctionAngle: Double, route: Option[Route]): Unit = {
    assertInitialized()

    val now = systemTimeInMilliseconds
    deltaAcc += now - lastUpdate
    if (deltaAcc > noteDeltaThreshold) {

      Some(notedef.get.play(args = List(
        "expVol" -> (1.0 - Math.abs(Helpers.wrapToSignedPi(correctionAngle) / Math.PI))
      )))

      deltaAcc = 0
    }
    lastUpdate = now
  }

  override def reachedWaypoint(): Unit = {
    approval.get.set("t_trig" -> 1)
  }
}
