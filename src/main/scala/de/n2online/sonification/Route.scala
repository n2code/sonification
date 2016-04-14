package de.n2online.sonification

import java.util.LinkedList
import java.util.List

class Route private[sonification] {
  waypoints = new util.LinkedList[Waypoint]
  private var waypoints: util.LinkedList[Waypoint] = null

  def addWaypoint(waypoint: Waypoint) {
    waypoints.add(waypoint)
  }

  def getWaypoints: util.List[Waypoint] = {
    return waypoints
  }

  def currentWaypoint: Waypoint = {
    var i: Int = 0
    while (i < waypoints.size) {
      {
        if (!waypoints.get(i).visited) {
          return waypoints.get(i)
        }
      }
      ({
        i += 1; i - 1
      })
    }
    return null
  }
}