package de.n2online.sonification.generators

import de.sciss.synth._
import de.sciss.synth.ugen._

object Common {
  def approvalSynthDef = Some(SynthDef("MegaSaw") {
    val numSaws = 7
    val freq = "freq".kr(55.0)
    val acc =
      VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0.1, width = 0.05) +
        VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0.2, width = 0.05) +
        VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0.3, width = 0.05) +
        VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0.4, width = 0.05) +
        VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0.5, width = 0.05) +
        VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0.6, width = 0.05) +
        VarSaw.ar(freq = List.fill(2)(freq * LinRand(0.99, 1.02)), iphase = 0.7, width = 0.05)

    import Env.{Segment => Seg}
    val graph = Env(0.001, List(Seg(0.05, 1), Seg(0.1, 1), Seg(0.2, 0.6), Seg(2.0, 0.001)))
    val t_trig = "t_trig".tr
    val env = EnvGen.kr(graph, t_trig, doneAction = 0)
    Out.ar(List(0, 1), acc * (1.0 / numSaws) * env)
  })
}
