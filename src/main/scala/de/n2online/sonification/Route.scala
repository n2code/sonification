package de.n2online.sonification

import scala.collection.mutable.MutableList
import java.util.List

class Route {
  private var waypoints: MutableList[Waypoint] = null

  def addWaypoint(waypoint: Waypoint) {
    waypoints += waypoint
  }

  def getWaypoints: MutableList[Waypoint] = {
    return waypoints
  }

  def currentWaypoint: Waypoint = {
    var i: Int = 0
    while (i < waypoints.size) {
      {
        if (!waypoints(i).visited) {
          return waypoints(i)
        }
      }
      ({
        i += 1; i - 1
      })
    }
    return null
  }
}