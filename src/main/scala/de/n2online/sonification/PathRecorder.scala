package de.n2online.sonification

import javafx.scene.chart.{LineChart, XYChart}

import de.n2online.sonification.Helpers._
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

import scala.collection.mutable

class PathRecorder(val anglePlot: LineChart[Number, Number]) {
  private var path: mutable.MutableList[TimedPosition] = null
  private var active = false
  private var playing = false
  private var tUpdated: Long = 0
  private var tWaypointTackled: Long = 0
  private var tTotal: Long = 0
  private var posTargetAcquired = Vector2D.NaN
  private var posPrevious = Vector2D.NaN
  private var accDistance: Double = 0
  reset()

  def reset() = {
    active = false
    path = new mutable.MutableList[TimedPosition]
    tTotal = 0
  }

  def start(pos: Vector2D) = {
    initNewTarget(pos, systemTimeInMilliseconds)
    active = true
    play()
  }

  def play() = {
    assert(active, "Playing path recorder which is not active")
    tUpdated = systemTimeInMilliseconds
    playing = true
    this
  }

  def update(agent: Agent, reachedWaypoint: Option[Waypoint]) = {
    assert(active && playing, "Updating path recorder which is not active&&playing")
    val posCurrent = agent.pos

    val tNow = systemTimeInMilliseconds
    val tDelta = tNow - tUpdated
    tUpdated = tNow
    tTotal += tDelta

    accDistance += posCurrent.distance(posPrevious)
    posPrevious = posCurrent

    path += TimedPosition(posCurrent.getX, posCurrent.getY, tDelta, tTotal, agent.targetDistance, agent.targetAngle, reachedWaypoint)
    val seriesData = anglePlot.getData.get(0).getData
    seriesData.add(new XYChart.Data[Number, Number](tTotal / 1000.0, Math.toDegrees(agent.targetAngle)))
    if (seriesData.size > SuiteUI.maxGraphValues) seriesData.remove(0, seriesData.size - SuiteUI.maxGraphValues - 1)

    reachedWaypoint match {
      case Some(waypoint) => {
        //calculate stats
        waypoint.statUserTimeMilliseconds = Some(tNow - tWaypointTackled)
        waypoint.statStraightDistance = Some(posCurrent.distance(posTargetAcquired))
        waypoint.statUserDistance = Some(accDistance)
        val perfectTime = (waypoint.statStraightDistance.get / Agent.maxSpeed).toInt
        Sonification.log(s"[REACHED] time ${waypoint.statUserTimeMilliseconds.get / 1000}sec (${perfectTime}sec optimum)" +
          s", distance ${waypoint.statUserDistance.get.toInt} (${waypoint.statStraightDistance.get.toInt} optimum)")

        //reset counters
        initNewTarget(posCurrent, tNow)
      }
      case None =>
    }

  }

  private def initNewTarget(currentPos: Vector2D, tNow: Long) = {
    tWaypointTackled = tNow
    posTargetAcquired = currentPos
    posPrevious = currentPos
    accDistance = 0
  }

  def pause() = {
    playing = false
  }

  def getPath = {
    path.toList
  }
}
