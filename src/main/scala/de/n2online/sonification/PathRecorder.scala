package de.n2online.sonification

import de.n2online.sonification.Helpers._
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

import scala.collection.mutable

class PathRecorder {
  private var path: mutable.MutableList[TimedPosition] = null
  private var active = false
  private var started: Long = 0
  private var updated: Long = 0
  reset()

  def reset() = {
    active = false
    path = new mutable.MutableList[TimedPosition]
  }

  def start() = {
    started = systemTimeInMilliseconds
    updated = started
    active = true
    this
  }

  def update(pos: Vector2D) = {
    assert(active, "Updating path recorder which is not running")
    val now = systemTimeInMilliseconds
    val delta = now - updated
    path += TimedPosition(pos.getX, pos.getY, delta)
    updated = now
  }

  def play = start()
  def pause() = {
    active = false
  }

  def getPath = {
    path.toList
  }
}
