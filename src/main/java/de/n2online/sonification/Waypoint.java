package de.n2online.sonification;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.MathUtils;

public class Waypoint {
    public Vector2D pos;
    public static final double thresholdReached = 30;
    public boolean visited = false;

    Waypoint(double x, double y) {
        pos = new Vector2D(x, y);
    }

    public boolean isReached(Vector2D compare) {
        if (compare.distance(pos) <= thresholdReached) {
            return true;
        } else {
            return false;
        }
    }

    public double getAngleCorrection(Agent agent){
        Vector2D normHoming = this.pos.subtract(agent.pos).normalize();

        //calculate signed to angle to indicate direction
        double angleHoming = Vector2D.angle(normHoming, new Vector2D(1, 0));
        angleHoming *= (agent.pos.getY()<this.pos.getY() ? 1 : -1);

        //return normalized (-PI to PI)
        return MathUtils.normalizeAngle(angleHoming-agent.getOrientation(), 0.0);
    }
}
