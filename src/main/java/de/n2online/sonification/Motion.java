package de.n2online.sonification;

import javafx.scene.input.KeyCode;

public class Motion {
    private boolean activeAi = false;

    public void handle(double stepFraction, Keyboard keyboard, Agent agent, Route route) {
        if (keyboard.isKeyDown(KeyCode.LEFT))  agent.turn(stepFraction*-agent.maxTurn);
        if (keyboard.isKeyDown(KeyCode.RIGHT)) agent.turn(stepFraction*agent.maxTurn);

        if (keyboard.isKeyDown(KeyCode.UP)) {
            agent.speed = Agent.maxSpeed;
        } else {
            agent.speed = 0;
        }

        if (keyboard.isKeyDown(KeyCode.SPACE)) activeAi = true;
        if (activeAi) {
            double corr = stepFraction*route.currentWaypoint().getAngleCorrection(agent);
            agent.turn(Math.min(corr, stepFraction*agent.maxTurn*Math.signum(corr)));
            agent.speed = Agent.maxSpeed/(Math.max(2*Waypoint.thresholdReached,Agent.maxSpeed))*route.currentWaypoint().pos.distance(agent.pos);
            agent.speed = Math.min(Math.max(Waypoint.thresholdReached*stepFraction, agent.speed), Agent.maxSpeed);
        }

        agent.moveForward(stepFraction);

        Waypoint currentWp = route.currentWaypoint();
        if (currentWp != null) {
            if (currentWp.isReached(agent.pos)) {
                currentWp.visited = true;
            }
        }
    }

}
