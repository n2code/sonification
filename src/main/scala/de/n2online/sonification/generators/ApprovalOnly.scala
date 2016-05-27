package de.n2online.sonification.generators

import de.n2online.sonification.Route
import de.sciss.synth.Ops._
import de.sciss.synth._

class ApprovalOnly() extends Generator {
  override def initialize(server: Server): Unit = { }
  override def update(absoluteDistance: Double, correctionAngle: Double, route: Option[Route]): Unit = { }
  override def reachedWaypoint(): Unit = {
    Common.approvalSynthDef.get.play(args = List("t_trig" -> 1))
  }
}
