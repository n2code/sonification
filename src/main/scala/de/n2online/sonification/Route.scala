package de.n2online.sonification

class Route(wpoints: List[Waypoint]) {
  val waypoints = wpoints

  def splitByVisited = waypoints.span(_.visited)

  def visited = splitByVisited._1

  def remaining = splitByVisited._2

  def currentWaypoint = remaining.headOption

  def nextWaypoint = remaining.tail.headOption
}