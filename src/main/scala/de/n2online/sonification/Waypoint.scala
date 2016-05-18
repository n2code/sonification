package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.util.MathUtils
import javafx.scene.paint.Color

object Waypoint {
  val thresholdReached: Double = 20
  final val colorVisited = Color.DARKGREEN
  final val colorNext = Color.ORANGE
  final val colorLater = Color.DARKRED
}

class Waypoint (centerNode: Node) {
  val node = centerNode
  var visited: Boolean = false

  def isReached(compare: Vector2D): Boolean = compare.distance(node.pos) <= Waypoint.thresholdReached

  def getAngleCorrection(agent: Agent): Double = {
    val normalizedHoming: Vector2D = this.node.pos.subtract(agent.pos).normalize
    val angleHoming: Double = Vector2D.angle(normalizedHoming, new Vector2D(1, 0)) * (if (agent.pos.getY < this.node.pos.getY) 1 else -1)
    MathUtils.normalizeAngle(angleHoming - agent.getOrientation, 0.0)
  }
}