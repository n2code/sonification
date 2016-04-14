package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.util.FastMath

object Agent {
  val maxSpeed: Double = 150
}

class Agent private[sonification](val posX: Double, val posY: Double, var orientation: Double) {
  pos = new Nothing(posX, posY)
  speed = 0
  var pos: Nothing = null
  var speed: Double = .0
  final val maxTurn: Double = Math.toRadians(90)

  def getOrientation: Double = {
    return orientation
  }

  def turn(angle: Double) {
    orientation += angle
    while (orientation > Math.PI) {
      orientation -= 2 * Math.PI
    }
    while (orientation < -Math.PI) {
      orientation += 2 * Math.PI
    }
  }

  def moveForward(moveFraction: Double) {
    pos = pos.add(moveFraction * speed, new Nothing(FastMath.cos(orientation), FastMath.sin(orientation)))
  }
}