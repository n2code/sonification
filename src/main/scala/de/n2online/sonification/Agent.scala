package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.util.FastMath

object Agent {
  val maxSpeed: Double = 150
}

class Agent (val posX: Double, val posY: Double, var initialOrientation: Double) {
  var pos: Vector2D = new Vector2D(posX, posY)
  var speed: Double = 0
  val maxTurnPerSecond: Double = Math.toRadians(90)
  var orientation: Double = initialOrientation

  def getOrientation: Double = orientation

  def turn(angle: Double) {
    orientation = wrapToSignedPi(orientation + angle)
  }

  private def wrapToSignedPi(radianAngle: Double): Double =  {
    var angle: Double = radianAngle
    while (angle > Math.PI) {
      angle -= 2 * Math.PI
    }
    while (angle < -Math.PI) {
      angle += 2 * Math.PI
    }
    angle
  }

  def moveForward(moveFraction: Double) {
    pos = pos.add(moveFraction * speed, new Vector2D(FastMath.cos(orientation), FastMath.sin(orientation)))
  }
}