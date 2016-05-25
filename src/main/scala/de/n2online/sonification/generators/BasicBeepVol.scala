package de.n2online.sonification.generators

import de.n2online.sonification.{Generator, Helpers}
import de.n2online.sonification.Helpers._
import de.sciss.synth.Ops._
import de.sciss.synth._
import de.sciss.synth.ugen._

class BasicBeepVol extends Generator {
  private var notedef = None: Option[SynthDef]
  private var approval = None: Option[Synth]
  private var approvalDef = None: Option[SynthDef]

  private var deltaAcc: Long = 0
  private var lastUpdate: Long = 0
  private val noteDeltaThreshold = 1000

  override def initialize(server: Server): Unit = {
    notedef = Some(SynthDef("NoteGen") {
      val vol = "expVol".kr(0.0)

      //val midiLow = 36
      //val midiHigh = 84
      //val calculatedNote = LinLin("linHeight".kr(0.5), 0.0, 1.0, midiLow, midiHigh).roundTo(1)
      val freq = 220

      val pure = SinOsc.ar(freq / 2) * 0.1 + SinOsc.ar(freq) * 0.25 + Pulse.ar(freq) * 0.2

      import Env.{Segment => Seg}
      val hi = Seg(0.05, 1)
      val mid = Seg(0.1, 0.5.dbamp)
      val graph = Env(0.001, List(hi, mid, Seg(1, 0.001)))
      val env = EnvGen.kr(graph, doneAction = 2)

      val sig = pure * env

      //val panned = Pan2.ar(sig, LinLin("signedPan".kr(0.0), -math.Pi, math.Pi, -1.0, 1.0))
      //Out.ar(0, panned * vol.dbamp)
      Out.ar(List(0, 1), sig * vol)
    })

    approvalDef = Some(SynthDef("MegaSaw") {
      val numSaws = 7
      val freq = "freq".kr(55.0)
      val acc =
        VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
          VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
          VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
          VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
          VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
          VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05) +
          VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0, width = 0.05)

      import Env.{Segment => Seg}
      val hi = Seg(0.2, 1)
      val mid = Seg(0.2, 0.6)
      val graph = Env(0.001, List(hi, mid, Seg(2.5, 0.001)))
      val t_trig = "t_trig".tr
      val env = EnvGen.kr(graph, t_trig, doneAction = 0) //0 damit es retriggerable bleibt
      Out.ar(List(0, 1), acc * (1.0 / numSaws) * env)
    })
    approval = Some(approvalDef.get.play())
  }

  def assertInitialized = {
    assert(notedef.isDefined && approvalDef.isDefined && approval.isDefined)
  }

  override def update(absoluteDistance: Double, correctionAngle: Double): Unit = {
    assertInitialized

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
