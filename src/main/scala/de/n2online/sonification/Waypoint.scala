package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.util.MathUtils

object Waypoint {
  val thresholdReached: Double = 30
}

class Waypoint (val x: Double, val y: Double) {
  var node = new Node(x, y)
  var visited: Boolean = false

  def isReached(compare: Vector2D): Boolean = compare.distance(node.pos) <= Waypoint.thresholdReached

  def getAngleCorrection(agent: Agent): Double = {
    val normalizedHoming: Vector2D = this.node.pos.subtract(agent.pos).normalize
    val angleHoming: Double = Vector2D.angle(normalizedHoming, new Vector2D(1, 0)) * (if (agent.pos.getY < this.node.pos.getY) 1 else -1)
    MathUtils.normalizeAngle(angleHoming - agent.getOrientation, 0.0)
  }
}