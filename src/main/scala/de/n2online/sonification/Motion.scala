package de.n2online.sonification

import javafx.scene.input.KeyCode

class Motion {
  private var activeAi: Boolean = false

  def handle(stepFraction: Double, keyboard: Nothing, agent: Nothing, route: Route) {
    if (keyboard.isKeyDown(KeyCode.LEFT)) agent.turn(stepFraction * -agent.maxTurn)
    if (keyboard.isKeyDown(KeyCode.RIGHT)) agent.turn(stepFraction * agent.maxTurn)
    if (keyboard.isKeyDown(KeyCode.UP)) {
      agent.speed = Agent.maxSpeed
    }
    else {
      agent.speed = 0
    }
    if (keyboard.isKeyDown(KeyCode.SPACE)) activeAi = true
    if (activeAi) {
      val corr: Double = stepFraction * route.currentWaypoint.getAngleCorrection(agent)
      agent.turn(Math.min(corr, stepFraction * agent.maxTurn * Math.signum(corr)))
      agent.speed = Agent.maxSpeed / (Math.max(2 * Waypoint.thresholdReached, Agent.maxSpeed)) * route.currentWaypoint.pos.distance(agent.pos)
      agent.speed = Math.min(Math.max(Waypoint.thresholdReached * stepFraction, agent.speed), Agent.maxSpeed)
    }
    agent.moveForward(stepFraction)
    val currentWp: Waypoint = route.currentWaypoint
    if (currentWp != null) {
      if (currentWp.isReached(agent.pos)) {
        currentWp.visited = true
      }
    }
  }
}