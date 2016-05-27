package de.n2online.sonification.generators

import de.n2online.sonification.{Helpers, Route}
import de.sciss.synth.Ops._
import de.sciss.synth._
import de.sciss.synth.ugen._

class SoniGuide() extends Generator {
  private var deltaAcc: Long = 0
  private var lastUpdate: Long = 0
  private val noteDeltaThreshold = 1000

  override def initialize(server: Server): Unit = {
  }

  def assertInitialized() = {
    assert(true)
  }

  override def update(absoluteDistance: Double, correctionAngle: Double, route: Option[Route]): Unit = {
    assertInitialized()
  }

  override def reachedWaypoint(): Unit = {
  }
}
