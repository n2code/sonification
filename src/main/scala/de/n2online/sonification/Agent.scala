package de.n2online.sonification

import de.n2online.sonification.Helpers._

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.util.FastMath

object Agent {
  final val maxSpeed: Double = 20
}

class Agent (val posX: Double, val posY: Double, var initialOrientation: Double) {
  var pos: Vector2D = new Vector2D(posX, posY)
  var speed: Double = 0
  val maxTurnPerSecond: Double = Math.toRadians(90)
  private var orientation: Double = initialOrientation
  val recorder = new PathRecorder()

  def getOrientation: Double = orientation

  def turn(angle: Double) {
    orientation = wrapToSignedPi(orientation + angle)
  }

  def move(moveFraction: Double) {
    pos = pos.add(moveFraction * speed, new Vector2D(FastMath.cos(orientation), FastMath.sin(orientation)))
  }
}