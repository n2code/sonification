package de.n2online.sonification

import java.lang.System.nanoTime

import scala.util.Try

object Helpers {

  def wrapToSignedPi(radianAngle: Double): Double = {
    var angle: Double = radianAngle
    while (angle > Math.PI) {
      angle -= 2 * Math.PI
    }
    while (angle < -Math.PI) {
      angle += 2 * Math.PI
    }
    angle
  }

  def systemTimeInMilliseconds: Long = {
    nanoTime / 1000000
  }

  def tryToInt(text: String) = Try(text.toInt).toOption
}

