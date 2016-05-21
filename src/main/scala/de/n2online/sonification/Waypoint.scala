package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.util.MathUtils
import javafx.scene.paint.Color

object Waypoint {
  final val thresholdReached: Double = 20
  final val colorVisited = Color.DARKGREEN
  final val colorCurrentCore = Color.DARKORANGE
  final val colorCurrentHitbox = Color.ORANGE
  final val colorNext = Color.RED
  final val colorLater = Color.DARKRED
}

class Waypoint (centerNode: Node) {
  val node = centerNode
  var visited: Boolean = false

  var statStraightDistance: Option[Double] = None
  var statUserDistance: Option[Double] = None
  var statUserTimeMilliseconds: Option[Long] = None

  override def equals(that: Any): Boolean = that match {
    case that: Waypoint => that.node.equals(this.node)
    case _ => false
  }
  override def hashCode = node.hashCode()

  def isReached(compare: Vector2D): Boolean = compare.distance(node.pos) <= Waypoint.thresholdReached

  def getAngleCorrection(agent: Agent): Double = {
    this.node.pos.subtract(agent.pos) match {
      case Vector2D.ZERO => 0.0
      case homing: Vector2D => {
        val normalizedHoming: Vector2D = homing.normalize()
        val angleHoming: Double = Vector2D.angle(normalizedHoming, new Vector2D(1, 0)) * (if (agent.pos.getY < this.node.pos.getY) 1 else -1)
        MathUtils.normalizeAngle(angleHoming - agent.getOrientation, 0.0)
      }
    }
  }
}