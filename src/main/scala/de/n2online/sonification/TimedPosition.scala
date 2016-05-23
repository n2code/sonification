package de.n2online.sonification

case class TimedPosition(x: Double, y: Double,
                         millisecondDelta: Long,
                         millisecondTotal: Long,
                         agentDistance: Double,
                         agentAngle: Double,
                         reachedWaypoint: Option[Waypoint] = None
                        )
