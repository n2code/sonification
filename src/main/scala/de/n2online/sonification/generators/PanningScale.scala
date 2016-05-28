package de.n2online.sonification.generators

import de.n2online.sonification.Helpers._
import de.n2online.sonification.{Agent, Route}
import de.sciss.synth.Ops._
import de.sciss.synth._
import de.sciss.synth.ugen._

class PanningScale extends Generator {
  private var notedef = None: Option[SynthDef]

  private var deltaAcc: Long = 0
  private var lastUpdate: Long = 0
  private val noteDeltaThreshold = 1000

  override def initialize(server: Server): Unit = {
    notedef = Some(SynthDef("NoteGen") {
      val gvol = "linVol".kr(1.0)
      import Env.{Segment => Seg}

      val midiLow = 36
      val midiHigh = 84
      val calculatedNote = LinLin("linHeight".kr(0.5), 0.0, 1.0, midiLow, midiHigh).roundTo(1)
      val freq = calculatedNote.midicps

      val pure = SinOsc.ar(freq / 2) * 0.1 + SinOsc.ar(freq) * 0.25 + Pulse.ar(freq) * 0.2

      val hi = Seg(0.05, 1)
      val mid = Seg(0.1, 0.5.dbamp)
      val graph = Env(0.001, List(hi, mid, Seg(1, 0.001)))
      val env = EnvGen.kr(graph, doneAction = 2)

      val sig = pure * env

      val panned = Pan2.ar(sig, LinLin("signedPan".kr(0.0), -math.Pi, math.Pi, -1.0, 1.0))
      Out.ar(0, panned * gvol.dbamp)
    })
  }

  def assertInitialized = {
    assert(notedef.isDefined)
  }

  override def update(absoluteDistance: Double, correctionAngle: Double, route: Option[Route]): Unit = {
    assertInitialized

    val now = systemTimeInMilliseconds
    deltaAcc += now - lastUpdate
    if (deltaAcc > noteDeltaThreshold) {

      Some(notedef.get.play(args = List(
        "linHeight" -> math.max(0, 1000 - absoluteDistance) / 1000.0,
        "signedPan" -> correctionAngle,
        "linVol" -> 1.0
      )))

      deltaAcc = 0
    }
    lastUpdate = now
  }

  override def reachedWaypoint(agent: Agent, route: Route): Unit = {
    /*NO-OP*/
  }
}
