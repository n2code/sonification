package de.n2online.sonification.generators

import de.n2online.sonification.{Agent, Helpers, Route}
import de.sciss.synth.Ops._
import de.sciss.synth._
import de.sciss.synth.ugen._

class BasicBeepFreq() extends Generator {
  private var note = None: Option[Synth]
  private var approval = None: Option[Synth]

  private var deltaAcc: Long = 0
  private var lastUpdate: Long = 0
  private val noteDeltaThreshold = 1000

  override def initialize(server: Server): Unit = {
    val notedef = Some(SynthDef("NoteGen") {
      val vol = LinExp.ar("linVol".kr(0.0), 0.0, 1.0, 0.2, 1.0)

      val freq = LinExp.ar("freqLin".kr(0.0), 0.01, 1.0, 55.0, 220.0)
      val pure = SinOsc.ar(freq / 2) * 0.1 + SinOsc.ar(freq) * 0.25 + Pulse.ar(freq) * 0.1

      Out.ar(List(0, 1), pure * vol)
    })
    note = Some(notedef.get.play())

    approval = Some(Common.approvalSynthDef.get.play())
  }

  def assertInitialized() = {
    assert(note.isDefined && approval.isDefined)
  }

  override def update(absoluteDistance: Double, correctionAngle: Double, route: Option[Route]): Unit = {
    assertInitialized()

    val linAngle = 1.0 - Math.abs(Helpers.wrapToSignedPi(correctionAngle) / Math.PI)

    note.get.set("freqLin" -> linAngle, "linVol" -> linAngle)
  }

  override def reachedWaypoint(agent: Agent, route: Route): Unit = {
    approval.get.set("t_trig" -> 1)
  }
}
