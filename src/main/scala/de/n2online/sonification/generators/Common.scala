package de.n2online.sonification.generators

import de.sciss.synth._
import de.sciss.synth.ugen._

object Common {
  def approvalSynthDef = Some(SynthDef("MegaSaw") {
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
    val env = EnvGen.kr(graph, t_trig, doneAction = 0)
    Out.ar(List(0, 1), acc * (1.0 / numSaws) * env)
  })
}
