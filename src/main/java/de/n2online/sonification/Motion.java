package de.n2online.sonification;

import javafx.scene.input.KeyCode;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;

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
            Vector2D normHoming = currentWp.pos.subtract(agent.pos).normalize();

            double distance = currentWp.pos.distance(agent.pos);

            //calculate signed to angle to indicate direction
            double angleHoming = Vector2D.angle(normHoming, new Vector2D(1, 0));
            angleHoming *= (agent.pos.getY()<currentWp.pos.getY() ? 1 : -1);

            double correction = MathUtils.normalizeAngle(angleHoming-agent.getOrientation(), 0.0);
            System.out.print(" | DIST: "+distance);
            System.out.print(" | CORRECTION: "+FastMath.toDegrees(correction));
            System.out.println();

            if (currentWp.isReached(agent.pos)) {
                currentWp.visited = true;
            }
        }


    }

}
