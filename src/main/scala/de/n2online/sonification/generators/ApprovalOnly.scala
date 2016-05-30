package de.n2online.sonification.generators

import de.n2online.sonification.{Agent, Route}
import de.sciss.synth.Ops._
import de.sciss.synth._

class ApprovalOnly() extends Generator {
  //dummy to demo approval sound but avoid spoiling the real generator
  override def initialize(server: Server): Unit = { }
  override def update(absoluteDistance: Double, correctionAngle: Double, route: Option[Route]): Unit = { }
  override def reachedWaypoint(agent: Agent, route: Route): Unit = {
    Common.approvalSynthDef.get.play(args = List("t_trig" -> 1))
  }
}
