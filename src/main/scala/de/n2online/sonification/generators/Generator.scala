package de.n2online.sonification.generators

import de.n2online.sonification.Route
import de.sciss.synth._

abstract class Generator {
  def initialize(server: Server)
  def update(absoluteDistance: Double, correctionAngle: Double, route: Option[Route] = None)
  def reachedWaypoint()
}
