package de.n2online.sonification;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

public class Agent {
    public Vector2D pos;
    public double speed; //position units per second
    private double orientation; //angle in radians, 0 is for only x axis movement
    public final double maxTurn = Math.toRadians(90); //maximum turning per second
    public final double maxSpeed = 50;

    Agent(double posX, double posY, double angle) {
        pos = new Vector2D(posX, posY);
        speed = 0;
        orientation = angle;
    }

    public double getOrientation() {
        return orientation;
    }

    public void turn(double angle) {
        orientation += angle;
        while (orientation > Math.PI) orientation -= 2*Math.PI;
        while (orientation < -Math.PI) orientation += 2*Math.PI;
    }

    public void moveForward(double moveFraction) {
        pos = pos.add(moveFraction * speed,
                new Vector2D(FastMath.cos(orientation), FastMath.sin(orientation))
        );
    }
}
