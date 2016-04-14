package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.util.MathUtils

object Waypoint {
  val thresholdReached: Double = 30
}

class Waypoint private[sonification](val x: Double, val y: Double) {
  pos = new Nothing(x, y)
  var pos: Nothing = null
  var visited: Boolean = false

  def isReached(compare: Nothing): Boolean = {
    if (compare.distance(pos) <= Waypoint.thresholdReached) {
      return true
    }
    else {
      return false
    }
  }

  def getAngleCorrection(agent: Nothing): Double = {
    val normHoming: Nothing = this.pos.subtract(agent.pos).normalize
    var angleHoming: Double = Vector2D.angle(normHoming, new Nothing(1, 0))
    angleHoming *= (if (agent.pos.getY < this.pos.getY) 1
    else -1)
    return MathUtils.normalizeAngle(angleHoming - agent.getOrientation, 0.0)
  }
}