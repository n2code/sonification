package de.n2online.sonification

object Helpers {

  def wrapToSignedPi(radianAngle: Double): Double =  {
    var angle: Double = radianAngle
    while (angle > Math.PI) {
      angle -= 2 * Math.PI
    }
    while (angle < -Math.PI) {
      angle += 2 * Math.PI
    }
    angle
  }

}

