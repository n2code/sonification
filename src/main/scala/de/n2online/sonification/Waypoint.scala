package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.util.MathUtils

object Waypoint {
  val thresholdReached: Double = 30
}

class Waypoint (val x: Double, val y: Double) {
  var pos: Vector2D = new Vector2D(x, y)
  var visited: Boolean = false

  def isReached(compare: Vector2D): Boolean = {
    compare.distance(pos) <= Waypoint.thresholdReached
  }

  def getAngleCorrection(agent: Agent): Double = {
    val normHoming: Vector2D = this.pos.subtract(agent.pos).normalize
    val angleHoming: Double = Vector2D.angle(normHoming, new Vector2D(1, 0)) * (if (agent.pos.getY < this.pos.getY) 1 else -1)
    MathUtils.normalizeAngle(angleHoming - agent.getOrientation, 0.0)
  }
}