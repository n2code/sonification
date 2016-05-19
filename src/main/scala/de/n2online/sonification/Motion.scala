package de.n2online.sonification

import javafx.scene.input.KeyCode

class Motion {
  var activeAi: Boolean = false

  def handle(stepFraction: Double, keyboard: Keyboard, agent: Agent, route: Route) {
    if (keyboard.isKeyDown(KeyCode.LEFT)) agent.turn(stepFraction * -agent.maxTurnPerSecond)
    if (keyboard.isKeyDown(KeyCode.RIGHT)) agent.turn(stepFraction * agent.maxTurnPerSecond)

    agent.speed = 0
    if (keyboard.isKeyDown(KeyCode.UP)) agent.speed += Agent.maxSpeed
    if (keyboard.isKeyDown(KeyCode.DOWN)) agent.speed -= Agent.maxSpeed

    if (keyboard.isKeyDown(KeyCode.ENTER)) activeAi = true
    if (keyboard.isKeyDown(KeyCode.BACK_SPACE)) activeAi = false

    var reached: Option[Waypoint] = None

    route.currentWaypoint match {
      case Some(currentTarget) => {
        if (activeAi) {
          val corr: Double = stepFraction * currentTarget.getAngleCorrection(agent)
          agent.turn(Math.min(corr, stepFraction * agent.maxTurnPerSecond * Math.signum(corr)))
          agent.speed = Agent.maxSpeed / Math.max(2 * Waypoint.thresholdReached, Agent.maxSpeed) * currentTarget.node.pos.distance(agent.pos)
          agent.speed = Math.min(Math.max(Waypoint.thresholdReached * stepFraction, agent.speed), Agent.maxSpeed)
        }
        agent.move(stepFraction)
        if (currentTarget.isReached(agent.pos)) {
          currentTarget.visited = true
          reached = Some(currentTarget)
        }
      }
    }

    agent.recorder.update(agent.pos, reached)
  }
}