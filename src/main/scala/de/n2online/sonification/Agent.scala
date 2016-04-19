package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.util.FastMath

object Agent {
  val maxSpeed: Double = 150
}

class Agent (val posX: Double, val posY: Double, var initialOrientation: Double) {
  var pos: Vector2D = new Vector2D(posX, posY)
  var speed: Double = 0
  val maxTurn: Double = Math.toRadians(90)
  var orientation: Double = initialOrientation

  def getOrientation: Double = orientation

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
    pos = pos.add(moveFraction * speed, new Vector2D(FastMath.cos(orientation), FastMath.sin(orientation)))
  }
}