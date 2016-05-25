package de.n2online.sonification

import java.lang.System.nanoTime
import java.text.SimpleDateFormat
import java.util.Date

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

  def baseFileName(exp: Experiment, append: Option[String] = None) = {
    sortableDate +
      "_" + exp.textSeed +
      "_" + exp.meshSize.toString +
      "_" + exp.route.waypoints.length.toString + "n" +
      append.fold("")("_" + _)
  }

  def sortableDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date)
}

