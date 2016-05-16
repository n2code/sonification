package de.n2online.sonification.generators

import de.n2online.sonification.Generator
import de.sciss.synth._
import ugen._
import Ops._
import org.apache.commons.math3.util.FastMath

class PanningSaws extends Generator {
  private var synth = None: Option[Synth]
  private var sawdef = None: Option[SynthDef]

  override def initialize(server: Server): Unit = {
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
      Out.ar(outChannel, acc * (1.0 / numSaws) * "factor".kr(1.0))
    })
    synth = Some(sawdef.get.play())
  }

  def assertInitialized = {
    assert(synth.isDefined)
    assert(sawdef.isDefined)
  }

  override def update(absoluteDistance: Double): Unit = {
    assertInitialized

    val newFactor = FastMath.max(0.0, 400.0-absoluteDistance) / 400

    synth.get.set("factor" -> newFactor)
  }
}
