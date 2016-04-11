package de.n2online.sonification;

import javafx.scene.input.KeyCode;

public class Motion {

    public void handle(double stepFraction, Keyboard keyboard, Agent agent, Route route) {
        if (keyboard.isKeyDown(KeyCode.LEFT))  agent.turn(-stepFraction*agent.maxTurn);
        if (keyboard.isKeyDown(KeyCode.RIGHT)) agent.turn(stepFraction*agent.maxTurn);

        if (keyboard.isKeyDown(KeyCode.UP)) {
            agent.speed = agent.maxSpeed;
        } else {
            agent.speed = 0;
        }

        agent.moveForward(stepFraction);
        Waypoint current = route.currentWaypoint();
        if (current != null && current.isReached(agent.pos)) {
            current.visited = true;
        }
    }

}
