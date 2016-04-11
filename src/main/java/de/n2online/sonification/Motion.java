package de.n2online.sonification;

import javafx.scene.input.KeyCode;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

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
        Waypoint currentWp = route.currentWaypoint();
        if (currentWp != null) {
            double distance = currentWp.pos.distance(agent.pos);
            double angle = Vector2D.angle(currentWp.pos.subtract(agent.pos), agent.pos);
            double aim = angle - agent.getOrientation();
            System.out.println("DIST: "+distance+" | ANGLE: "+angle+" | AIM: "+aim);

            if (currentWp.isReached(agent.pos)) {
                currentWp.visited = true;
            }
        }


    }

}
