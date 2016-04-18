package de.n2online.sonification

import scala.collection.mutable.MutableList

class Route {
  private var waypoints: MutableList[Waypoint] = new MutableList[Waypoint]

  def addWaypoint(waypoint: Waypoint) {
    waypoints += waypoint
  }

  def getWaypoints = waypoints

  def currentWaypoint: Waypoint = {
    waypoints.find(_.visited == false).orNull
  }
}