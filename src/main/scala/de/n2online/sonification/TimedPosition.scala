package de.n2online.sonification

case class TimedPosition(x: Double, y: Double,
                         millisecondDelta: Long,
                         reachedWaypoint: Option[Waypoint] = None
                        )
