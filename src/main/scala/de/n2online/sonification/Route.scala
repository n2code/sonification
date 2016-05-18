package de.n2online.sonification

import scala.collection.mutable

class Route {
  private var waypoints: mutable.MutableList[Waypoint] = new mutable.MutableList[Waypoint]

  def addWaypoint(waypoint: Waypoint) {
    waypoints += waypoint
  }

  def getWaypoints = waypoints

  def currentWaypoint: Option[Waypoint] = {
    waypoints.find(_.visited == false)
  }
}