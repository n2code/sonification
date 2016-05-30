package de.n2online.sonification

import de.n2online.sonification.Helpers._
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

object Agent {
  val maxSpeed: Double = 20
  val maxTurnPerSecond: Double = Math.toRadians(60)
}

class Agent(posX: Double, posY: Double, initialOrientation: Double) {
  var pos: Vector2D = new Vector2D(posX, posY)
  var speed: Double = 0
  var targetDistance: Double = Double.NaN
  var targetAngle: Double = Double.NaN
  private var orientation: Double = initialOrientation

  def getOrientation: Double = orientation

  def turn(angle: Double) {
    orientation = wrapToSignedPi(orientation + angle)
  }

  def move(moveFraction: Double) {
    pos = pos.add(moveFraction * speed, new Vector2D(Math.cos(orientation), Math.sin(orientation)))
  }
}